package net.veldor.flibustaloader.parsers;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.selections.FoundedSequence;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

class SequencesParser {
    public static ArrayList parse(NodeList entries, XPath xPath) throws XPathExpressionException {
        ArrayList<FoundedSequence> result = new ArrayList<>();
        Node entry;
        FoundedSequence sequence;
        int counter = 0;
        int entriesLength = entries.getLength();
        int handledEntryCounter = 0;
        while ((entry = entries.item(counter)) != null) {
            ++handledEntryCounter;
            App.getInstance().mLoadAllStatus.postValue("Обрабатываю серию " + handledEntryCounter + " из " + entriesLength);
            sequence = new FoundedSequence();
            sequence.title = ((Node) xPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
            sequence.content = ((Node) xPath.evaluate("./content", entry, XPathConstants.NODE)).getTextContent();
            sequence.link = ((Node) xPath.evaluate("./link", entry, XPathConstants.NODE)).getAttributes().getNamedItem("href").getTextContent();
            result.add(sequence);
            counter++;
        }
        return result;
    }
}
