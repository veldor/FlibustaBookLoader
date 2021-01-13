package net.veldor.flibustaloader.utils;

import androidx.lifecycle.MutableLiveData;

import net.veldor.flibustaloader.selections.BlacklistItem;

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

public class BlacklistSequences {

    private static final String BLACKLIST_NAME = "sequence";
    private Document mDom;
    private ArrayList<BlacklistItem> mBlacklistValues;
    private final ArrayList<String> mExistentValues = new ArrayList<>();
    public static final MutableLiveData<Boolean> mListRefreshed = new MutableLiveData<>();

    public BlacklistSequences(){
        refreshBlacklist();
    }

    private void refreshBlacklist() {
        // получу данны файла подписок
        String rawData = MyFileReader.getSequencesBlacklist();
        mDom = getDocument(rawData);
        mBlacklistValues = new ArrayList<>();
        if (mDom != null) {
            NodeList values = mDom.getElementsByTagName(BLACKLIST_NAME);
            int counter = 0;
            BlacklistItem blacklistItem;
            while (values.item(counter) != null) {
                blacklistItem = new BlacklistItem();
                blacklistItem.name = values.item(counter).getFirstChild().getNodeValue();
                mExistentValues.add(blacklistItem.name);
                blacklistItem.type = "sequence";
                mBlacklistValues.add(blacklistItem);
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

    public ArrayList<BlacklistItem> getBlacklist() {
        refreshBlacklist();
        return mBlacklistValues;
    }

    public void addValue(String value) {
        if (!mExistentValues.contains(value)) {
            Element elem = mDom.createElement(BLACKLIST_NAME);
            Text text = mDom.createTextNode(value);
            elem.appendChild(text);
            mDom.getDocumentElement().insertBefore(elem, mDom.getDocumentElement().getFirstChild());
            MyFileReader.saveSequencesBlacklist(getStringFromDocument(mDom));
            mListRefreshed.postValue(true);
        }
    }

    public void deleteValue(String name) {
        NodeList sequences = mDom.getElementsByTagName(BLACKLIST_NAME);
        Node item;
        int length = sequences.getLength();
        int counter = 0;
        if(length > 0){
            while (counter < length){
                item = sequences.item(counter);
                if(name.equals(item.getTextContent())){
                    item.getParentNode().removeChild(item);
                    break;
                }
                counter++;
            }
            MyFileReader.saveSequencesBlacklist(getStringFromDocument(mDom));
            mListRefreshed.postValue(true);
        }
    }
}
