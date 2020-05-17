package net.veldor.flibustaloader.parsers;

import android.content.Intent;
import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.selections.SubscriptionItem;
import net.veldor.flibustaloader.utils.Grammar;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static net.veldor.flibustaloader.MyWebViewClient.TOR_CONNECT_ERROR_ACTION;

public class SubscriptionsParser {
    public static void handleSearchResults(ArrayList<FoundedBook> result, String answer, ArrayList<SubscriptionItem> subscribes) throws XPathExpressionException {
        Document document = getDocument(answer);
        XPath xpath = XPathFactory.newInstance().newXPath();
        if(subscribes != null && subscribes.size() > 0){
            for (SubscriptionItem si :
                    subscribes) {
                // проверю тип подписки.
                switch (si.type){
                    case "author":
                        foundAuthor(document, xpath, si.name, result);
                        break;
                    case "book":
                        foundBook(document, xpath, si.name, result);
                        break;
                    case "sequence":
                        foundSequence(document, xpath, si.name, result);
                        break;
                }
            }
        }
    }

    private static void foundSequence(Document document, XPath xpath, String name, ArrayList<FoundedBook> result) throws XPathExpressionException {
        name = name.toLowerCase();
        String value;
        // Найду автора по данным
        NodeList contentWithSequences = (NodeList) xpath.evaluate("/feed/entry/content[contains(text(),'Серия: ')]", document, XPathConstants.NODESET);
        if(contentWithSequences.getLength() > 0){
            int counter = 0;
            Node content;
            while ((content = contentWithSequences.item(counter)) != null) {
                value = content.getTextContent().toLowerCase();
                if(value.substring(value.lastIndexOf("серия: ")).contains(name)){
                    // добавлю найденный результат
                    addBookToResult(content.getParentNode(), result, xpath);
                }
                counter++;
            }
        }
    }

    private static void foundAuthor(Document document, XPath xpath, String name, ArrayList<FoundedBook> result) throws XPathExpressionException {
        name = name.toLowerCase();
        String value;
        // Найду автора по данным
        NodeList authors = (NodeList) xpath.evaluate("/feed/entry/author/name", document, XPathConstants.NODESET);
        if(authors.getLength() > 0){
            int counter = 0;
            Node author;
            while ((author = authors.item(counter)) != null) {
                value = author.getTextContent().toLowerCase();
                if(value.contains(name)){
                    // добавлю найденный результат
                    addBookToResult(author.getParentNode().getParentNode(), result, xpath);
                }
                counter++;
            }
        }
    }

    private static void foundBook(Document document, XPath xpath, String name, ArrayList<FoundedBook> result) throws XPathExpressionException {
        name = name.toLowerCase();
        String value;
        // Найду title, в котором пристутствует выбранное слово
        NodeList titles = (NodeList) xpath.evaluate("/feed/entry/title", document, XPathConstants.NODESET);
        if(titles.getLength() > 0){
            int counter = 0;
            Node title;
            while ((title = titles.item(counter)) != null) {
                value = title.getTextContent().toLowerCase();
                if(value.contains(name)){
                    // добавлю найденный результат
                    addBookToResult(title.getParentNode(), result, xpath);
                    return;
                }
                counter++;
            }
        }
    }

    private static Document getDocument(String rawText) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(rawText));
            return dBuilder.parse(is);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            // ошибка поиска, предположу, что страница недоступна
            // отправлю оповещение об ошибке загрузки TOR
            Intent finishLoadingIntent = new Intent(TOR_CONNECT_ERROR_ACTION);
            App.getInstance().sendBroadcast(finishLoadingIntent);
            e.printStackTrace();
        }
        return null;
    }


    private static void addBookToResult(Node bookNode, ArrayList<FoundedBook> result, XPath xPath) throws XPathExpressionException {
        Node someNode;
        String someString;
        NamedNodeMap someAttributes;
        NodeList xpathResult;
        DownloadLink downloadLink;
        Author author;
        Genre genre;

        StringBuilder stringBuilder = new StringBuilder();
        int innerCounter;
        FoundedSequence sequence;


        FoundedBook book = new FoundedBook();
        book.id = ((Node) xPath.evaluate("./id", bookNode, XPathConstants.NODE)).getTextContent();
        book.name = ((Node) xPath.evaluate("./title", bookNode, XPathConstants.NODE)).getTextContent();
        xpathResult = (NodeList) xPath.evaluate("./author", bookNode, XPathConstants.NODESET);
        if (xpathResult.getLength() > 0) {
            stringBuilder.setLength(0);
            innerCounter = 0;
            while ((someNode = xpathResult.item(innerCounter)) != null) {
                author = new Author();
                // найду имя
                someString = ((Node) xPath.evaluate("./name", someNode, XPathConstants.NODE)).getTextContent();
                author.name = someString;
                stringBuilder.append(someString);
                stringBuilder.append("\n");
                author.uri = ((Node) xPath.evaluate("./uri", someNode, XPathConstants.NODE)).getTextContent().substring(3);
                book.authors.add(author);
                ++innerCounter;

            }
            book.author = stringBuilder.toString();
        }
        // добавлю категории
        xpathResult = (NodeList) xPath.evaluate("./category", bookNode, XPathConstants.NODESET);
        if (xpathResult.getLength() > 0) {
            stringBuilder.setLength(0);
            innerCounter = 0;
            while ((someNode = xpathResult.item(innerCounter)) != null) {
                someAttributes = someNode.getAttributes();
                someString = someAttributes.getNamedItem("label").getTextContent();
                stringBuilder.append(someString);
                stringBuilder.append("\n");
                // добавлю жанр
                genre = new Genre();
                genre.label = someString;
                genre.term = someAttributes.getNamedItem("term").getTextContent();
                ++innerCounter;
                book.genres.add(genre);
            }
            book.genreComplex = stringBuilder.toString();
        }

        // разберу информацию о книге
        someString = ((Node) xPath.evaluate("./content", bookNode, XPathConstants.NODE)).getTextContent();
        book.bookInfo = someString;
        book.downloadsCount = getInfoFromContent(someString, "Скачиваний:");
        book.size = getInfoFromContent(someString, "Размер:");
        book.format = getInfoFromContent(someString, "Формат:");
        book.translate = Grammar.textFromHtml(getInfoFromContent(someString, "Перевод:"));
        book.sequenceComplex = getInfoFromContent(someString, "Серия:");

        // найду ссылки на скачивание книги
        xpathResult = (NodeList) xPath.evaluate("./link[@rel='http://opds-spec.org/acquisition/open-access']", bookNode, XPathConstants.NODESET);
        innerCounter = 0;
        while ((someNode = xpathResult.item(innerCounter)) != null) {
            someAttributes = someNode.getAttributes();
            downloadLink = new DownloadLink();
            downloadLink.id = book.id;
            downloadLink.url = someAttributes.getNamedItem("href").getTextContent();
            downloadLink.mime = someAttributes.getNamedItem("type").getTextContent();
            downloadLink.name = book.name;
            downloadLink.author = book.author;
            downloadLink.size = book.size;
            book.downloadLinks.add(downloadLink);
            innerCounter++;
        }
        // найду ссылки на серии
        xpathResult = (NodeList) xPath.evaluate("./link[@rel='related']", bookNode, XPathConstants.NODESET);
        innerCounter = 0;
        while ((someNode = xpathResult.item(innerCounter)) != null) {
            someAttributes = someNode.getAttributes();
            someString = someAttributes.getNamedItem("href").getTextContent();
            if (someString.startsWith("/opds/sequencebooks/")) {
                // Найдена серия
                sequence = new FoundedSequence();
                sequence.link = someString;
                sequence.title = someAttributes.getNamedItem("title").getTextContent();
                book.sequences.add(sequence);
            }
            innerCounter++;
        }
        result.add(book);
    }

    private static String getInfoFromContent(String item, String s) {
        int start = item.indexOf(s);
        int end = item.indexOf("<br/>", start);
        if (start > 0 && end > 0)
            return item.substring(start, end);
        return "";
    }
}
