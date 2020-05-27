package net.veldor.flibustaloader.parsers;

import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.selections.Genre;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

class GenresParser {
    public static ArrayList parse(NodeList entries, XPath xPath) {
        ArrayList<Genre> result = new ArrayList<Genre>();
        Node entry;
        Genre genre;
        int counter = 0;
        int entriesLength = entries.getLength();
        int handledEntryCounter = 0;
        while ((entry = entries.item(counter)) != null) {
            ++handledEntryCounter;
            App.getInstance().mLoadAllStatus.postValue("Обрабатываю жанр " + handledEntryCounter + " из " + entriesLength);
            genre = new Genre();
            try {
                genre.label = ((Node) xPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
                genre.term = ((Node) xPath.evaluate("./link", entry, XPathConstants.NODE)).getAttributes().getNamedItem("href").getTextContent();
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
            result.add(genre);
            counter++;
        }
        // отправлю обработанные результаты
        Log.d("surprise", "GenresParser parse: founded genres: " + result.size());
        return result;
    }
}
