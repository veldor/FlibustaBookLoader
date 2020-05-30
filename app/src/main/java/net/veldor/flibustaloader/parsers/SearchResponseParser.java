package net.veldor.flibustaloader.parsers;

import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ui.OPDSActivity;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class SearchResponseParser {

    private static final String BOOK_TYPE = "tag:book";
    private static final String GENRE_TYPE = "tag:root:genre";
    private static final String AUTHOR_TYPE = "tag:author";
    private static final String SEQUENCES_TYPE = "tag:sequences";
    private static final String SEQUENCE_TYPE = "tag:sequence";
    private static final CharSequence AUTHOR_SEQUENCE_TYPE = ":sequence:";
    private static final String NEW_GENRES = "tag:search:new:genres";
    private static final String NEW_SEQUENCES = "tag:search:new:sequence";
    private static final String NEW_AUTHORS = "tag:search:new:author";
    private NodeList mEntries;
    private XPath mXpath;

    public SearchResponseParser(String answer) throws XPathExpressionException {
        Document document = getDocument(answer);
        mXpath = XPathFactory.newInstance().newXPath();
        App.getInstance().mSearchTitle.postValue(((Node) mXpath.evaluate("/feed/title", document, XPathConstants.NODE)).getTextContent());
        mEntries = (NodeList) mXpath.evaluate("/feed/entry", document, XPathConstants.NODESET);
    }

    public ArrayList parseResponse() throws XPathExpressionException {
        // получу сущности
        if (mEntries != null && mEntries.getLength() > 0) {
            // в зависимости от типа сущностей запущу парсер
            int contentType = identificationSearchType(mEntries.item(0), mXpath);
            if (contentType > 0) {
                switch (contentType) {
                    case OPDSActivity.SEARCH_GENRE:
                        return GenresParser.parse(mEntries, mXpath);
                    case OPDSActivity.SEARCH_SEQUENCE:
                        return SequencesParser.parse(mEntries, mXpath);
                    case OPDSActivity
                            .SEARCH_AUTHORS:
                    case OPDSActivity
                            .SEARCH_NEW_AUTHORS:
                        return AuthorsParser.parse(mEntries, mXpath);
                    case OPDSActivity.SEARCH_BOOKS:
                        return BooksParser.parse(mEntries, mXpath);
                }
            }
        }
        return null;
    }

    private static Document getDocument(String rawText) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(rawText));
            return dBuilder.parse(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private int identificationSearchType(Node item, XPath xPath) throws XPathExpressionException {
        // получу идентификатор
        String id = ((Node) xPath.evaluate("./id", item, XPathConstants.NODE)).getTextContent();
        Log.d("surprise", "SearchResponseParser identificationSearchType 82: identification id is " + id);
        if (id.startsWith(BOOK_TYPE)) {
            return OPDSActivity.SEARCH_BOOKS;
        } else if (id.startsWith(GENRE_TYPE)) {
            return OPDSActivity.SEARCH_GENRE;
        } else if (id.startsWith(AUTHOR_TYPE)) {
            // проверю на возможность, что загружены серии
            if (id.contains(AUTHOR_SEQUENCE_TYPE)) {
                return OPDSActivity.SEARCH_SEQUENCE;
            } else {
                return OPDSActivity.SEARCH_AUTHORS;
            }
        } else if (id.startsWith(SEQUENCES_TYPE)) {
            return OPDSActivity.SEARCH_SEQUENCE;
        } else if (id.startsWith(SEQUENCE_TYPE)) {
            return OPDSActivity.SEARCH_SEQUENCE;
        } else if (id.startsWith(NEW_GENRES)) {
            return OPDSActivity.SEARCH_GENRE;
        } else if (id.startsWith(NEW_SEQUENCES)) {
            return OPDSActivity.SEARCH_SEQUENCE;
        } else if (id.startsWith(NEW_AUTHORS)) {
            return OPDSActivity.SEARCH_NEW_AUTHORS;
        }
        return -1;
    }

    public int getType() throws XPathExpressionException {
        if(mEntries.getLength() > 0){
            return identificationSearchType(mEntries.item(0), mXpath);
        }
        return -1;
    }
}
