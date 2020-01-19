package net.veldor.flibustaloader.utils;

import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.selections.SubscriptionItem;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class SubscribeAuthors {

    private static final String SUBSCRIBE_NAME = "author";
    private Document mDom;
    private ArrayList<SubscriptionItem> mSubscribeValues;
    private ArrayList<String> mExistentValues = new ArrayList<>();
    public MutableLiveData<Boolean> mListRefreshed = new MutableLiveData<>();

    public SubscribeAuthors(){
        refreshSubscribes();
    }

    private void refreshSubscribes() {
        // получу данны файла подписок
        String rawData = MyFileReader.getAuthorsSubscribe();
        mDom = getDocument(rawData);
        mSubscribeValues = new ArrayList<>();
        if (mDom != null) {
            NodeList values = mDom.getElementsByTagName(SUBSCRIBE_NAME);
            int counter = 0;
            SubscriptionItem subscriptionItem;
            while (values.item(counter) != null) {
                subscriptionItem = new SubscriptionItem();
                subscriptionItem.name = values.item(counter).getFirstChild().getNodeValue();
                mExistentValues.add(subscriptionItem.name);
                subscriptionItem.type = "author";
                mSubscribeValues.add(subscriptionItem);
                ++counter;
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

    public ArrayList<SubscriptionItem> getSubscribes() {
        refreshSubscribes();
        return mSubscribeValues;
    }

    public void addValue(String value) {
        if (!mExistentValues.contains(value)) {
            Element elem = mDom.createElement(SUBSCRIBE_NAME);
            Text text = mDom.createTextNode(value);
            elem.appendChild(text);
            mDom.getDocumentElement().insertBefore(elem, mDom.getDocumentElement().getFirstChild());
            MyFileReader.saveAuthorsSubscription(getStringFromDocument(mDom));
            App.getInstance().getAuthorsSubscribe().mListRefreshed.postValue(true);
        }
    }

    public void deleteValue(String name) {
        NodeList authors = mDom.getElementsByTagName(SUBSCRIBE_NAME);
        Node book;
        int length = authors.getLength();
        int counter = 0;
        if(length > 0){
            while (counter < length){
                book = authors.item(counter);
                if(name.equals(book.getTextContent())){
                    book.getParentNode().removeChild(book);
                    break;
                }
                counter++;
            }
            MyFileReader.saveAuthorsSubscription(getStringFromDocument(mDom));
            App.getInstance().getAuthorsSubscribe().mListRefreshed.postValue(true);
        }
    }
}
