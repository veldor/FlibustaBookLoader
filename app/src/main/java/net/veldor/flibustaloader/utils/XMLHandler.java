package net.veldor.flibustaloader.utils;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XMLHandler {
    private static final String SEARCH_VALUE_NAME = "string";

    public static ArrayList<String> getSearchAutocomplete(String autocompleteText){

        ArrayList<String> searchValues = new ArrayList<>();
            Document searchList = getDocument(autocompleteText);
            // найду значения строк
        if(searchList != null){
            NodeList values = searchList.getElementsByTagName(SEARCH_VALUE_NAME);
            int counter = 0;

            while (values.item(counter) != null){
                searchValues.add(values.item(counter).getFirstChild().getNodeValue());
                ++counter;
            }
        }
        return searchValues;
    }
    private static ArrayList<String> getSearchAutocomplete(Document searchList){

        ArrayList<String> searchValues = new ArrayList<>();
            // найду значения строк
            NodeList values = searchList.getElementsByTagName(SEARCH_VALUE_NAME);
            int counter = 0;

            while (values.item(counter) != null){
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
        if(!values.contains(s)){
            Element elem = dom.createElement(SEARCH_VALUE_NAME);
            Text text = dom.createTextNode(s);
            elem.appendChild(text);
            dom.getDocumentElement().insertBefore(elem, dom.getDocumentElement().getFirstChild());
            MyFileReader.saveSearchAutocomplete(getStringFromDocument(dom));
            return true;
        }
        return false;
    }

    private static Document getDocument(String rawText){
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

    private static String getStringFromDocument(Document doc){
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
}
