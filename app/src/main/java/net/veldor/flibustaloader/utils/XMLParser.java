package net.veldor.flibustaloader.utils;

import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.workers.DownloadBookWorker;
import net.veldor.flibustaloader.workers.ParseSearchWorker;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class XMLParser {

    public static Document mDoc;
    private XPath mXPath;

    public XMLParser(String xml) {
        mDoc = getDocument(xml);
    }

    public static Document getDocument(String rawText) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(rawText));
            return dBuilder.parse(is);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;
    }

    public HashMap<String, ArrayList> getSearchResults() {

        Log.d("surprise", "XMLParser getSearchResults start parsing");

        // попробую xpath
        XPathFactory factory = XPathFactory.newInstance();
        mXPath = factory.newXPath();

        // объявлю переменные
        StringBuilder stringBuilder = new StringBuilder();
        try {
            NodeList entries = (NodeList) mXPath.evaluate("//entry", mDoc, XPathConstants.NODESET);
            int entriesLength = entries.getLength();
            if (entriesLength > 0) {
                // получу тип поиска
                App.getInstance().mSearchTitle.postValue(((Node) mXPath.evaluate("./title", mDoc.getDocumentElement(), XPathConstants.NODE)).getTextContent());
                Node id = (Node) mXPath.evaluate("./id", mDoc.getDocumentElement(), XPathConstants.NODE);
                String searchType = explodeByDelimiter(id.getTextContent(), ":", 3);
                if (searchType != null) {
                    int counter = 0;
                    switch (searchType) {
                        case "authors":
                            return getAuthors(entries, entriesLength, searchType, counter);
                        case "books":
                            return getBooks(stringBuilder, entries, entriesLength);
                        case "genre":
                            // проверю, если в выборке книги- верну книги, если жанры- верну жанры
                            String type = explodeByDelimiter(((Node) mXPath.evaluate("./id", entries.item(0), XPathConstants.NODE)).getTextContent(), ":", 2);
                            if(type != null && type.equals("book")){
                                return getBooks(stringBuilder, entries, entriesLength);
                            }
                            return getGenres(entries, entriesLength);
                        case "sequences" :
                            return getSequences(entries, entriesLength);
                    }

                    // возможно, идёт загрузка книг по автору
                    searchType = explodeByDelimiter(id.getTextContent(), ":", 2);
                    if(searchType != null){
                        if(searchType.equals("author")){
                            // смотрим, нашли ли мы книги или серии
                            searchType = explodeByDelimiter(id.getTextContent(), ":", 4);
                            if(searchType != null && (searchType.equals("books") || searchType.equals("sequenceless"))){
                                return getBooks(stringBuilder, entries, entriesLength);
                            }
                            else if(searchType != null && searchType.equals("sequences")){
                                return getSequences(entries, entriesLength);
                            }
                            else if(searchType != null && searchType.equals("sequence")){
                                return getBooks(stringBuilder, entries, entriesLength);
                            }
                        }
                        else if(searchType.equals("sequence")){
                            return getBooks(stringBuilder, entries, entriesLength);
                        }
                    }
                }
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @NotNull
    private HashMap<String, ArrayList> getSequences(NodeList entries, int entriesLength) throws XPathExpressionException {
        int counter;
        ArrayList<FoundedSequence> result = new ArrayList<>();
        FoundedSequence foundedSequence;
        counter = 0;
        while (counter < entriesLength) {
            Node entry = entries.item(counter);
            foundedSequence = new FoundedSequence();
            foundedSequence.title = ((Node) mXPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
            foundedSequence.content = ((Node) mXPath.evaluate("./content", entry, XPathConstants.NODE)).getTextContent();
            foundedSequence.link = ((Node) mXPath.evaluate("./link", entry, XPathConstants.NODE)).getAttributes().getNamedItem("href").getTextContent();
            result.add(foundedSequence);
            counter++;
        }
        HashMap<String, ArrayList> answer = new HashMap<>();
        answer.put("sequences", result);
        return answer;
    }

    @NotNull
    private HashMap<String, ArrayList> getAuthors(NodeList entries, int entriesLength, String searchType, int counter) throws XPathExpressionException {
        ArrayList<Author> result = new ArrayList<>();
        Author thisAuthor;
        // создам массив писателей
        while (counter < entriesLength) {
            thisAuthor = new Author();
            // добавлю автора в список
            Node entry = entries.item(counter);
            thisAuthor.name = ((Node) mXPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
            thisAuthor.uri = explodeByDelimiter(((Node) mXPath.evaluate("./id", entry, XPathConstants.NODE)).getTextContent(), ":" , 3);
            thisAuthor.content = ((Node) mXPath.evaluate("./content", entry, XPathConstants.NODE)).getTextContent();
            result.add(thisAuthor);
            ++counter;
        }
        HashMap<String, ArrayList> answer = new HashMap<>();
        answer.put(searchType, result);
        return answer;
    }

    private HashMap<String, ArrayList> getGenres(NodeList entries, int entriesLength) {
        int counter = 0;
        Genre foundedGenre;
        Node entry;
        ArrayList<Genre> result = new ArrayList<>();
        while (counter < entriesLength) {
            entry = entries.item(counter);
            foundedGenre  = new Genre();
            try {
                foundedGenre.label = ((Node) mXPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
                foundedGenre.term = ((Node) mXPath.evaluate("./link", entry, XPathConstants.NODE)).getAttributes().getNamedItem("href").getTextContent();
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
            result.add(foundedGenre);
            ++counter;
        }
        HashMap<String, ArrayList> answer = new HashMap<>();
        answer.put("genres" , result);
        return answer;
    }

    @NotNull
    private HashMap<String, ArrayList> getBooks(StringBuilder stringBuilder, NodeList entries, int entriesLength) throws XPathExpressionException {
        String searchType;
        int counter = 0;
        int innerCounter;
        Node someNode;
        Author author;
        FoundedSequence sequence;
        String someString;
        Genre genre;
        searchType = "books";
        Node entry;
        NodeList xpathResult;
        ArrayList<FoundedBook> result = new ArrayList<>();
        FoundedBook thisBook;
        Node link;
        NamedNodeMap attributes;
        DownloadLink downloadLink;

        int linksLength;
        int linksCounter;

        while (counter < entriesLength) {
            thisBook = new FoundedBook();
            // добавлю книгу в список
            entry = entries.item(counter);
            thisBook.name= ((Node) mXPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
            xpathResult = (NodeList) mXPath.evaluate("./author", entry, XPathConstants.NODESET);
            if (xpathResult.getLength() > 0) {
                stringBuilder.setLength(0);
                innerCounter = 0;
                while ((someNode = xpathResult.item(innerCounter)) != null) {
                    author = new Author();
                    // найду имя
                    someString = ((Node) mXPath.evaluate("./name", someNode, XPathConstants.NODE)).getTextContent();
                    author.name = someString;
                    stringBuilder.append(someString);
                    stringBuilder.append("\n");
                    author.uri = ((Node) mXPath.evaluate("./uri", someNode, XPathConstants.NODE)).getTextContent();
                    thisBook.authors.add(author);
                    ++innerCounter;

                }
                thisBook.author = stringBuilder.toString();
            }

            // добавлю категории
            xpathResult = (NodeList) mXPath.evaluate("./category", entry, XPathConstants.NODESET);
            if (xpathResult.getLength() > 0) {
                stringBuilder.setLength(0);
                innerCounter = 0;
                while ((someNode = xpathResult.item(innerCounter)) != null) {
                    someString = someNode.getAttributes().getNamedItem("label").getTextContent();
                    stringBuilder.append(someString);
                    stringBuilder.append("\n");
                    // добавлю жанр
                    genre = new Genre();
                    genre.label = someString;
                    genre.term = someNode.getAttributes().getNamedItem("term").getTextContent();
                    ++innerCounter;
                    thisBook.genres.add(genre);
                }
                thisBook.genreComplex = stringBuilder.toString();
            }

            xpathResult = (NodeList) mXPath.evaluate("./content", entry, XPathConstants.NODESET);
            someString = xpathResult.item(0).getTextContent();
            thisBook.downloadsCount = getInfoFromContent(someString, "Скачиваний:");
            thisBook.size = getInfoFromContent(someString, "Размер:");
            thisBook.format = getInfoFromContent(someString, "Формат:");
            thisBook.translate = Grammar.textFromHtml(getInfoFromContent(someString, "Перевод:"));
            thisBook.sequenceComplex = getInfoFromContent(someString, "Серия:");

            // найду ссылки на скачивание книги
            xpathResult = (NodeList) mXPath.evaluate("./link[@rel='http://opds-spec.org/acquisition/open-access']", entry, XPathConstants.NODESET);
            linksLength = xpathResult.getLength();
            linksCounter = 0;
            if (linksLength > 0) {
                while (linksCounter < linksLength) {
                    link = xpathResult.item(linksCounter);
                    attributes = link.getAttributes();
                    downloadLink = new DownloadLink();
                    downloadLink.url = attributes.getNamedItem("href").getTextContent();
                    downloadLink.mime = attributes.getNamedItem("type").getTextContent();
                    downloadLink.name = thisBook.name;
                    thisBook.downloadLinks.add(downloadLink);
                    linksCounter++;
                }
            }
            // найду ссылки на серии
            xpathResult = (NodeList) mXPath.evaluate("./link[@rel='related']", entry, XPathConstants.NODESET);
            linksLength = xpathResult.getLength();
            if (linksLength > 0) {
                linksCounter = 0;
                while (linksCounter < linksLength) {
                    link = xpathResult.item(linksCounter);
                    attributes = link.getAttributes();
                    someString = attributes.getNamedItem("href").getTextContent();
                    if(someString.startsWith("/opds/sequencebooks/")){
                        // Найдена серия
                        sequence = new FoundedSequence();
                        sequence.link = someString;
                        sequence.title = attributes.getNamedItem("title").getTextContent();
                        thisBook.sequences.add(sequence);
                    }
                    linksCounter++;
                }
            }

            result.add(thisBook);
            ++counter;
        }
        HashMap<String, ArrayList> answer = new HashMap<>();
        answer.put(searchType, result);
        Log.d("surprise", "XMLParser getBooks finish parsing");
        return answer;
    }

    private String getInfoFromContent(String item, String s) {
        int start = item.indexOf(s);
        int end = item.indexOf("<br/>", start);
        if (start > 0 && end > 0)
            return item.substring(start, end);
        return "";
    }

    private static String explodeByDelimiter(String s, String delimiter, int offset) {
        String[] result = s.split(delimiter);
        if (result.length < offset) {
            return null;
        }
        return result[offset - 1];
    }

    public String getNextPage() {
        // проверю, есть ли ссылка на следующую страницу
        try {
            Node entry = (Node) mXPath.evaluate("//link[@rel='next']", mDoc, XPathConstants.NODE);
            if (entry != null) {
                return entry.getAttributes().getNamedItem("href").getNodeValue();
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void handleResults() {
        // запущу рабочего, который обработает результаты запроса
        OneTimeWorkRequest downloadBookWorker = new OneTimeWorkRequest.Builder(ParseSearchWorker.class).build();
        WorkManager.getInstance().enqueue(downloadBookWorker);
    }
}
