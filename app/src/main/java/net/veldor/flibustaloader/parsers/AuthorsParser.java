package net.veldor.flibustaloader.parsers;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.ui.OPDSActivity;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

class AuthorsParser {
    public static ArrayList parse(NodeList entries, XPath xPath) throws XPathExpressionException {
        ArrayList<Author> result = new ArrayList<Author>();
        int counter = 0;
        Node entry;
        Author author;
        int entriesLength = entries.getLength();
        int handledEntryCounter = 0;
        while ((entry = entries.item(counter)) != null) {
            ++handledEntryCounter;
            App.getInstance().mLoadAllStatus.postValue("Обрабатываю автора " + handledEntryCounter + " из " + entriesLength);
            author = new Author();
            author.name = ((Node) xPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
            author.id = ((Node) xPath.evaluate("./id", entry, XPathConstants.NODE)).getTextContent();
            // если поиск осуществляется по новинкам- запишу ссылку на новинки, иначе- на автора
            if(author.id.startsWith("tag:authors")){
                author.uri = ((Node) xPath.evaluate("./link", entry, XPathConstants.NODE)).getAttributes().getNamedItem("href").getTextContent();
            }
            else if(App.sSearchType == OPDSActivity.SEARCH_NEW_AUTHORS){
                author.link = ((Node) xPath.evaluate("./link", entry, XPathConstants.NODE)).getAttributes().getNamedItem("href").getTextContent();
            }
            else{
                author.uri = explodeByDelimiter(((Node) xPath.evaluate("./id", entry, XPathConstants.NODE)).getTextContent());
            }

            author.content = ((Node) xPath.evaluate("./content", entry, XPathConstants.NODE)).getTextContent();
            result.add(author);
            counter++;
        }
        return result;
    }


    private static String explodeByDelimiter(String s) {
        String[] result = s.split(":");
        if (result.length < 3) {
            return null;
        }
        return result[3 - 1];
    }
}
