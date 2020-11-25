package net.veldor.flibustaloader.utils;

import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.database.entity.Bookmark;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.database.entity.DownloadedBooks;
import net.veldor.flibustaloader.database.entity.ReadedBooks;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class XMLHandler {
    private static final String SEARCH_VALUE_NAME = "string";

    public static ArrayList<String> getSearchAutocomplete(String autocompleteText) {

        ArrayList<String> searchValues = new ArrayList<>();
        Document searchList = getDocument(autocompleteText);
        // найду значения строк
        if (searchList != null) {
            NodeList values = searchList.getElementsByTagName(SEARCH_VALUE_NAME);
            int counter = 0;

            while (values.item(counter) != null) {
                searchValues.add(values.item(counter).getFirstChild().getNodeValue());
                ++counter;
            }
        }
        return searchValues;
    }

        private static ArrayList<String> getSearchAutocomplete(Document searchList) {

            ArrayList<String> searchValues = new ArrayList<>();
            // найду значения строк
            NodeList values = searchList.getElementsByTagName(SEARCH_VALUE_NAME);
            int counter = 0;

            while (values.item(counter) != null) {
                searchValues.add(values.item(counter).getFirstChild().getNodeValue());
                ++counter;
            }
            return searchValues;
        }

    public static boolean putSearchValue(String s) {
        // получу содержимое файла
        String rawXml = MyFileReader.getSearchAutocomplete();
        Document dom = getDocument(rawXml);
        assert dom != null;
        ArrayList<String> values = getSearchAutocomplete(dom);
        if (!values.contains(s)) {
            Element elem = dom.createElement(SEARCH_VALUE_NAME);
            Text text = dom.createTextNode(s);
            elem.appendChild(text);
            dom.getDocumentElement().insertBefore(elem, dom.getDocumentElement().getFirstChild());
            MyFileReader.saveSearchAutocomplete(getStringFromDocument(dom));
            return true;
        }
        else{
            // перенесу значение на верх списка
            NodeList existentValues = dom.getElementsByTagName(SEARCH_VALUE_NAME);
            if(existentValues != null && existentValues.getLength() > 0){
                int count = values.size();
                while (count > 1){
                    --count;
                    Node node = existentValues.item(count);
                    if(node.getTextContent().equals(s)){
                        dom.getDocumentElement().insertBefore(node, dom.getDocumentElement().getFirstChild());
                        MyFileReader.saveSearchAutocomplete(getStringFromDocument(dom));
                        return true;
                    }
                }
            }
        }
        return false;
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

    private static String getStringFromDocument(Document doc) {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
            transformer.transform(domSource, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return writer.toString();
    }

    public static void handleBackup(ZipInputStream zin) {
        try {
            AppDatabase db = App.getInstance().mDatabase;
            StringBuilder s = new StringBuilder();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = zin.read(buffer, 0, 1024)) >= 0) {
                s.append(new String(buffer, 0, read));
            }
            String xml = s.toString();
            Document document = XMLHandler.getDocument(xml);
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            NodeList entries = (NodeList) xPath.evaluate("/readed_books/book", document, XPathConstants.NODESET);
            if (entries != null && entries.getLength() > 0) {
                int counter = 0;
                int entriesLen = entries.getLength();
                Node node;
                while (counter < entriesLen) {
                    node = entries.item(counter);
                    // получу идентификатор книги
                    String id = node.getAttributes().getNamedItem("id").getTextContent();
                    ReadedBooks rb = new ReadedBooks();
                    rb.bookId = id;
                    if(db.readedBooksDao().getBookById(id) == null){
                        db.readedBooksDao().insert(rb);
                    }
                    Log.d("surprise", "handleBackup: founded readed book");
                    ++counter;
                }
            }
            entries = (NodeList) xPath.evaluate("/downloaded_books/book", document, XPathConstants.NODESET);
            if (entries != null && entries.getLength() > 0) {
                int counter = 0;
                int entriesLen = entries.getLength();
                Node node;
                while (counter < entriesLen) {
                    node = entries.item(counter);
                    // получу идентификатор книги
                    String id = node.getAttributes().getNamedItem("id").getTextContent();
                    DownloadedBooks rb = new DownloadedBooks();
                    rb.bookId = id;
                    if(db.downloadedBooksDao().getBookById(id) == null){
                        db.downloadedBooksDao().insert(rb);
                    }
                    Log.d("surprise", "handleBackup: founded downloaded book");
                    ++counter;
                }
            }
            entries = (NodeList) xPath.evaluate("/bookmarks/bookmark", document, XPathConstants.NODESET);
            if (entries != null && entries.getLength() > 0) {
                int counter = 0;
                int entriesLen = entries.getLength();
                Node node;
                while (counter < entriesLen) {
                    node = entries.item(counter);
                    // получу идентификатор книги
                    String name = node.getAttributes().getNamedItem("name").getTextContent();
                    String link = node.getAttributes().getNamedItem("link").getTextContent();
                    Bookmark bookmark = new Bookmark();
                    bookmark.name = name;
                    bookmark.link = link;
                    List<Bookmark> duplicate = db.bookmarksDao().getDuplicate(bookmark.name, bookmark.link);
                    if(duplicate == null || duplicate.size() == 0){
                        db.bookmarksDao().insert(bookmark);
                    }
                    Log.d("surprise", "handleBackup: founded bookmark");
                    ++counter;
                }
            }
            entries = (NodeList) xPath.evaluate("/schedule/item", document, XPathConstants.NODESET);
            if (entries != null && entries.getLength() > 0) {
                int counter = 0;
                int entriesLen = entries.getLength();
                Node node;
                while (counter < entriesLen) {
                    node = entries.item(counter);
                    BooksDownloadSchedule scheduleElement = new BooksDownloadSchedule();
                    String bookId = node.getAttributes().getNamedItem("bookId").getTextContent();
                    String link = node.getAttributes().getNamedItem("link").getTextContent();
                    String name = node.getAttributes().getNamedItem("name").getTextContent();
                    String size = node.getAttributes().getNamedItem("size").getTextContent();
                    String author = node.getAttributes().getNamedItem("author").getTextContent();
                    String format = node.getAttributes().getNamedItem("format").getTextContent();
                    String authorDirName = node.getAttributes().getNamedItem("authorDirName").getTextContent();
                    String sequenceDirName = node.getAttributes().getNamedItem("sequenceDirName").getTextContent();
                    String reservedSequenceName = node.getAttributes().getNamedItem("reservedSequenceName").getTextContent();
                    scheduleElement.bookId = bookId;
                    scheduleElement.link = link;
                    scheduleElement.name = name;
                    scheduleElement.size = size;
                    scheduleElement.author = author;
                    scheduleElement.format = format;
                    scheduleElement.authorDirName = authorDirName;
                    scheduleElement.sequenceDirName = sequenceDirName;
                    scheduleElement.reservedSequenceName = reservedSequenceName;
                    db.booksDownloadScheduleDao().insert(scheduleElement);
                    Log.d("surprise", "handleBackup: founded schedule element");
                    ++counter;
                }
            }

        } catch (IOException e) {
            Log.d("surprise", "handleBackup: error");
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            Log.d("surprise", "handleBackup: error1");
            e.printStackTrace();
        }

    }
}
