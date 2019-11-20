package net.veldor.flibustaloader.utils;

import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedAuthor;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.Genre;

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

    private final Document mDoc;
    private XPath mXPath;

    public XMLParser(String xml) {
        mDoc = getDocument(xml);
    }

    private static Document getDocument(String rawText) {
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

        // попробую xpath
        XPathFactory factory = XPathFactory.newInstance();
        mXPath = factory.newXPath();

        // объявлю переменные
        int innerCounter = 0;
        Node someNode;
        StringBuilder stringBuilder = new StringBuilder();
        Genre genre;
        Author author;
        String someString;
        try {
            NodeList entries = (NodeList) mXPath.evaluate("//entry", mDoc, XPathConstants.NODESET);
            int entriesLength = entries.getLength();
            if (entriesLength > 0) {
                // получу тип поиска
                NodeList id = (NodeList) mXPath.evaluate("./id", mDoc.getDocumentElement(), XPathConstants.NODESET);
                String searchType = explodeByDelimiter(id.item(0).getTextContent(), ":", 3);
                if (searchType != null) {
                    int counter = 0;
                    if (searchType.equals("authors")) {
                        ArrayList<FoundedAuthor> result = new ArrayList<>();
                        FoundedAuthor thisAuthor;
                        // создам массив писателей
                        while (counter < entriesLength) {
                            thisAuthor = new FoundedAuthor();
                            // добавлю автора в список
                            Node entry = entries.item(counter);
                            NodeList entryChildren = entry.getChildNodes();
                            int entryChildrenLength = entryChildren.getLength();
                            int childrenCounter = 0;
                            while (childrenCounter < entryChildrenLength) {
                                // заполню данные об авторе
                                Node childNode = entryChildren.item(childrenCounter);
                                String childNodeName = childNode.getNodeName();
                                switch (childNodeName) {
                                    case "title":
                                        thisAuthor.name = childNode.getTextContent();
                                        break;
                                    case "link":
                                        NamedNodeMap attributes = childNode.getAttributes();
                                        Node title = attributes.getNamedItem("title");
                                        if (title != null) {
                                            String titleValue = title.getNodeValue();
                                            if (titleValue.equals("Книги автора по сериям")) {
                                                thisAuthor.booksBySeries = attributes.getNamedItem("href").getNodeValue();
                                            } else if (titleValue.equals("Книги автора вне серий")) {
                                                thisAuthor.booksOutSeries = attributes.getNamedItem("href").getNodeValue();
                                            }
                                        } else {
                                            // проверю начало ссылки, найду ссылку на страницу автора
                                            String href = attributes.getNamedItem("href").getNodeValue();
                                            String linkBegin = href.substring(0, 13);
                                            if (linkBegin.equals("/opds/author/")) {
                                                thisAuthor.link = href;
                                            }
                                        }
                                        break;
                                    case "content":
                                        thisAuthor.booksQuantity = childNode.getTextContent();
                                        break;
                                }
                                ++childrenCounter;
                            }
                            result.add(thisAuthor);
                            ++counter;
                        }
                        HashMap<String, ArrayList> answer = new HashMap<>();
                        answer.put(searchType, result);
                        return answer;
                    } else if(searchType.equals("books")) {
                        ArrayList<FoundedBook> result = new ArrayList<>();
                        FoundedBook thisBook;
                        while (counter < entriesLength) {
                            thisBook = new FoundedBook();
                            // добавлю книгу в список
                            Node entry = entries.item(counter);
                            NodeList xpathResult = (NodeList) mXPath.evaluate("./title", entry, XPathConstants.NODESET);
                            thisBook.name = xpathResult.item(0).getTextContent();

                            xpathResult = (NodeList) mXPath.evaluate("./author", entry, XPathConstants.NODESET);
                            if(xpathResult.getLength() > 0){
                                stringBuilder.setLength(0);
                                innerCounter = 0;
                                while ((someNode = xpathResult.item(innerCounter) )!= null){
                                    author = new Author();
                                    // найду имя
                                    Node authorName = (Node) mXPath.evaluate("./name", someNode, XPathConstants.NODE);
                                    someString = authorName.getTextContent();
                                    author.name = someString;
                                    stringBuilder.append(someString);
                                    stringBuilder.append("\n");
                                    Node authorUri = (Node) mXPath.evaluate("./uri", someNode, XPathConstants.NODE);
                                    author.uri = authorUri.getTextContent();
                                    thisBook.authors.add(author);
                                    ++innerCounter;

                                }
                                thisBook.author = stringBuilder.toString();
                            }

                            // добавлю категории
                            xpathResult = (NodeList) mXPath.evaluate("./category", entry, XPathConstants.NODESET);


                            if(xpathResult.getLength() > 0){
                                stringBuilder.setLength(0);
                                innerCounter = 0;
                                while ((someNode = xpathResult.item(innerCounter) )!= null){
                                    someString = someNode.getAttributes().getNamedItem("label").getTextContent();
                                    stringBuilder.append(someString);
                                    stringBuilder.append("\n");
                                    // добавлю жанр
                                    genre = new Genre();
                                    genre.label = someString;
                                    genre.term = someNode.getAttributes().getNamedItem("term").getTextContent();
                                    ++innerCounter;
                                }
                                thisBook.genreComplex = stringBuilder.toString();
                            }

                            xpathResult = (NodeList) mXPath.evaluate("./content", entry, XPathConstants.NODESET);
                            someString = xpathResult.item(0).getTextContent();
                            thisBook.downloadsCount = getInfoFromContent(someString, "Скачиваний:");
                            thisBook.size = getInfoFromContent(someString, "Размер:");
                            thisBook.format = getInfoFromContent(someString, "Формат:");
                            thisBook.translate = getInfoFromContent(someString, "Перевод:");
                            thisBook.sequenceComplex = getInfoFromContent(someString, "Серия:");

                            // найду ссылки на скачивание книги
                            xpathResult = (NodeList) mXPath.evaluate("./link[@rel='http://opds-spec.org/acquisition/open-access']", entry, XPathConstants.NODESET);
                            int linksLength = xpathResult.getLength();
                            if(linksLength > 0){
                                int linksCounter = 0;
                                while (linksCounter < linksLength){
                                    Node link = xpathResult.item(linksCounter);
                                    NamedNodeMap attributes = link.getAttributes();
                                    DownloadLink downloadLink = new DownloadLink();
                                    downloadLink.url = attributes.getNamedItem("href").getTextContent();
                                    downloadLink.mime = attributes.getNamedItem("type").getTextContent();
                                    downloadLink.name = thisBook.name;
                                    thisBook.downloadLinks.add(downloadLink);
                                    linksCounter++;
                                }
                            }

                            result.add(thisBook);
                            ++counter;
                        }
                        HashMap<String, ArrayList> answer = new HashMap<>();
                        answer.put(searchType, result);
                        return answer;
                    }
                }
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getInfoFromContent(String item, String s) {
        int start = item.indexOf(s);
        int end = item.indexOf("<br/>", start);
        if(start > 0 && end > 0)
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
            if(entry != null){
                return entry.getAttributes().getNamedItem("href").getNodeValue();
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
