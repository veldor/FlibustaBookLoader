package net.veldor.flibustaloader.utils;

import android.content.Intent;
import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.ui.OPDSActivity;
import net.veldor.flibustaloader.workers.LoadSubscriptionsWorker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static net.veldor.flibustaloader.MyWebViewClient.TOR_CONNECT_ERROR_ACTION;
import static net.veldor.flibustaloader.http.TorWebClient.ERROR_DETAILS;

public class XMLParser {


    private static final String BOOK_TYPE = "tag:book";
    private static final String GENRE_TYPE = "tag:root:genre";
    private static final String AUTHOR_TYPE = "tag:author";
    private static final String SEQUENCES_TYPE = "tag:sequences";
    private static final String SEQUENCE_TYPE = "tag:sequence";
    private static final CharSequence AUTHOR_SEQUENCE_TYPE = ":sequence:";
    private static final String NEW_GENRES = "tag:search:new:genres";
    private static final String NEW_SEQUENCES = "tag:search:new:sequence";
    private static final String NEW_AUTHORS = "tag:search:new:author";
    private static final String READ_TYPE = "read";

    private static Document getDocument(String rawText) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(rawText));
            return dBuilder.parse(is);
        } catch (Exception e) {
            // ошибка поиска, предположу, что страница недоступна
            // отправлю оповещение об ошибке загрузки TOR
            Intent finishLoadingIntent = new Intent(TOR_CONNECT_ERROR_ACTION);
            finishLoadingIntent.putExtra(ERROR_DETAILS, "Ошибка обработки переданных данных");
            App.getInstance().sendBroadcast(finishLoadingIntent);
            e.printStackTrace();
        }
        return null;
    }

    public static void handleSearchResults(ArrayList<FoundedItem> result, String answer) {
        // получу документ
        Document document = getDocument(answer);
        // заполню ссылку на следующую страницу
        // попробую xpath
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        Node entry;
        try {
            entry = (Node) xPath.evaluate("/feed/link[@rel='next']", document, XPathConstants.NODE);
            LoadSubscriptionsWorker.sNextPage = entry == null ? null : entry.getAttributes().getNamedItem("href").getNodeValue();
            // получу сущности
            NodeList entries = (NodeList) xPath.evaluate("/feed/entry", document, XPathConstants.NODESET);
            if (entries.getLength() > 0) {
                App.getInstance().mSearchTitle.postValue(((Node) xPath.evaluate("/feed/title", document, XPathConstants.NODE)).getTextContent());
                // определю тип содержимого
                identificationSearchType(entries.item(0), xPath);
                // обработаю данные
                switch (App.sSearchType) {
                    case OPDSActivity
                            .SEARCH_BOOKS:
                        handleBooks(entries, result, xPath);
                        break;
                    case OPDSActivity
                            .SEARCH_AUTHORS:
                    case OPDSActivity
                            .SEARCH_NEW_AUTHORS:
                        handleAuthors(entries, result, xPath);
                        break;
                    case OPDSActivity
                            .SEARCH_GENRE:
                        handleGenres(entries, result, xPath);
                        break;
                    case OPDSActivity
                            .SEARCH_SEQUENCE:
                        handleSequences(entries, result, xPath);
                        break;
                }
            }

        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
    }


    private static void identificationSearchType(Node item, XPath xPath) throws XPathExpressionException {
        // получу идентификатор
        String id = ((Node) xPath.evaluate("./id", item, XPathConstants.NODE)).getTextContent();
        //Log.d("surprise", "ParseSearchWorker identificationSearchType " + id);
        if (id.startsWith(BOOK_TYPE)) {
            App.sSearchType = OPDSActivity.SEARCH_BOOKS;
        } else if (id.startsWith(GENRE_TYPE)) {
            App.sSearchType = OPDSActivity.SEARCH_GENRE;
        } else if (id.startsWith(AUTHOR_TYPE)) {
            // проверю на возможность, что загружены серии
            if (id.contains(AUTHOR_SEQUENCE_TYPE)) {
                //Log.d("surprise", "ParseSearchWorker identificationSearchType author sequence");
                App.sSearchType = OPDSActivity.SEARCH_SEQUENCE;
            } else {
                App.sSearchType = OPDSActivity.SEARCH_AUTHORS;
            }
        } else if (id.startsWith(SEQUENCES_TYPE)) {
            //Log.d("surprise", "ParseSearchWorker identificationSearchType sequenceS");
            App.sSearchType = OPDSActivity.SEARCH_SEQUENCE;
        }else if (id.startsWith(SEQUENCE_TYPE)) {
            //Log.d("surprise", "ParseSearchWorker identificationSearchType sequence");
            App.sSearchType = OPDSActivity.SEARCH_SEQUENCE;
        } else if (id.startsWith(NEW_GENRES)) {
            App.sSearchType = OPDSActivity.SEARCH_GENRE;
        } else if (id.startsWith(NEW_SEQUENCES)) {
            App.sSearchType = OPDSActivity.SEARCH_SEQUENCE;
        } else if (id.startsWith(NEW_AUTHORS)) {
            App.sSearchType = OPDSActivity.SEARCH_NEW_AUTHORS;
        }
        else{
            Log.d("surprise", "ParseSearchWorker identificationSearchType я ничего не понял " + id);
        }
        //Log.d("surprise", "ParseSearchWorker identificationSearchType " + App.sSearchType);
    }


    private static void handleSequences(NodeList entries, ArrayList<FoundedItem> result, XPath xPath) throws XPathExpressionException {
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
    }

    private static void handleGenres(NodeList entries, ArrayList<FoundedItem> result, XPath xPath) {
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
    }

    private static void handleBooks(NodeList entries, ArrayList<FoundedItem> result, XPath xPath) throws XPathExpressionException {
        boolean isLoadPreviews = App.getInstance().isPreviews();
        // обработаю найденные книги
        Node entry;
        Node someNode;
        String someString;
        NamedNodeMap someAttributes;
        NodeList xpathResult;
        FoundedBook book;
        DownloadLink downloadLink;
        Author author;
        Genre genre;

        StringBuilder stringBuilder = new StringBuilder();
        int counter = 0;
        int innerCounter;
        FoundedSequence sequence;

        boolean hideRead = App.getInstance().isHideRead();

        int entriesLength = entries.getLength();
        int handledEntryCounter = 0;

        while ((entry = entries.item(counter)) != null) {
            ++handledEntryCounter;
            App.getInstance().mLoadAllStatus.postValue("Обрабатываю книгу " + handledEntryCounter + " из " + entriesLength);
            book = new FoundedBook();
            book.id = ((Node) xPath.evaluate("./id", entry, XPathConstants.NODE)).getTextContent();
            // узнаю, прочитана ли книга
            AppDatabase db = App.getInstance().mDatabase;
            book.read = db.readedBooksDao().getBookById(book.id) != null;
            book.downloaded = db.downloadedBooksDao().getBookById(book.id) != null;
            if (book.read && hideRead) {
                counter++;
                continue;
            }
            book.name = ((Node) xPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
            counter++;
            result.add(book);
            xpathResult = (NodeList) xPath.evaluate("./author", entry, XPathConstants.NODESET);
            if (xpathResult.getLength() > 0) {
                stringBuilder.setLength(0);
                innerCounter = 0;
                while ((someNode = xpathResult.item(innerCounter)) != null) {
                    author = new Author();
                    // найду имя
                    someString = ((Node) xPath.evaluate("./name", someNode, XPathConstants.NODE)).getTextContent();
                    author.name = someString;
                    stringBuilder.append(someString);
                    stringBuilder.append("\n");
                    author.uri = ((Node) xPath.evaluate("./uri", someNode, XPathConstants.NODE)).getTextContent().substring(3);
                    book.authors.add(author);
                    ++innerCounter;

                }
                book.author = stringBuilder.toString();
            }
            // добавлю категории
            xpathResult = (NodeList) xPath.evaluate("./category", entry, XPathConstants.NODESET);
            if (xpathResult.getLength() > 0) {
                stringBuilder.setLength(0);
                innerCounter = 0;
                while ((someNode = xpathResult.item(innerCounter)) != null) {
                    someAttributes = someNode.getAttributes();
                    someString = someAttributes.getNamedItem("label").getTextContent();
                    stringBuilder.append(someString);
                    stringBuilder.append("\n");
                    // добавлю жанр
                    genre = new Genre();
                    genre.label = someString;
                    genre.term = someAttributes.getNamedItem("term").getTextContent();
                    ++innerCounter;
                    book.genres.add(genre);
                }
                book.genreComplex = stringBuilder.toString();
            }

            // разберу информацию о книге
            someString = ((Node) xPath.evaluate("./content", entry, XPathConstants.NODE)).getTextContent();
            book.bookInfo = someString;
            book.downloadsCount = getInfoFromContent(someString, "Скачиваний:");
            book.size = getInfoFromContent(someString, "Размер:");
            book.format = getInfoFromContent(someString, "Формат:");
            book.translate = Grammar.textFromHtml(getInfoFromContent(someString, "Перевод:"));
            book.sequenceComplex = getInfoFromContent(someString, "Серия:");

            // найду ссылки на скачивание книги
            xpathResult = (NodeList) xPath.evaluate("./link[@rel='http://opds-spec.org/acquisition/open-access']", entry, XPathConstants.NODESET);
            innerCounter = 0;
            while ((someNode = xpathResult.item(innerCounter)) != null) {
                someAttributes = someNode.getAttributes();
                downloadLink = new DownloadLink();
                downloadLink.id = book.id;
                downloadLink.url = someAttributes.getNamedItem("href").getTextContent();
                downloadLink.mime = someAttributes.getNamedItem("type").getTextContent();
                downloadLink.name = book.name;
                downloadLink.author = book.author;
                downloadLink.size = book.size;
                book.downloadLinks.add(downloadLink);
                innerCounter++;
            }
            // найду ссылки на серии
            xpathResult = (NodeList) xPath.evaluate("./link[@rel='related']", entry, XPathConstants.NODESET);
            innerCounter = 0;
            while ((someNode = xpathResult.item(innerCounter)) != null) {
                someAttributes = someNode.getAttributes();
                someString = someAttributes.getNamedItem("href").getTextContent();
                if (someString.startsWith("/opds/sequencebooks/")) {
                    // Найдена серия
                    sequence = new FoundedSequence();
                    sequence.link = someString;
                    sequence.title = someAttributes.getNamedItem("title").getTextContent();
                    book.sequences.add(sequence);
                }
                innerCounter++;
            }

            // если назначена загрузка превью- гружу их
            if(isLoadPreviews){
                someNode = ((Node) xPath.evaluate("./link[@rel='http://opds-spec.org/image']", entry, XPathConstants.NODE));
                if(someNode != null){
                    someString = someNode.getAttributes().getNamedItem("href").getTextContent();
                    if(someString != null && !someString.isEmpty()){
                        book.previewUrl = someString;
                    }
                }
            }
        }
    }

    private static void handleAuthors(NodeList entries, ArrayList<FoundedItem> result, XPath xPath) throws XPathExpressionException {
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
            // если поиск осуществляется по новинкам- запишу ссылку на новинки, иначе- на автора
            if(App.sSearchType == OPDSActivity.SEARCH_NEW_AUTHORS){
                author.link = ((Node) xPath.evaluate("./link", entry, XPathConstants.NODE)).getAttributes().getNamedItem("href").getTextContent();
            }
            else{
                author.uri = explodeByDelimiter(((Node) xPath.evaluate("./id", entry, XPathConstants.NODE)).getTextContent());
            }

            author.content = ((Node) xPath.evaluate("./content", entry, XPathConstants.NODE)).getTextContent();
            result.add(author);
            counter++;
        }
    }

    private static String getInfoFromContent(String item, String s) {
        int start = item.indexOf(s);
        int end = item.indexOf("<br/>", start);
        if (start > 0 && end > 0)
            return item.substring(start, end);
        return "";
    }

    private static String explodeByDelimiter(String s) {
        String[] result = s.split(":");
        if (result.length < 3) {
            return null;
        }
        return result[3 - 1];
    }

    public static void searchDownloadLinks(InputStream content) {
        try {
        org.jsoup.nodes.Document dom;
        String url = "http://rutracker.org";
            dom = Jsoup.parse(content, "UTF-8", url);
            // попробую найти форму входа. Если она найдена- значит, вход не выполнен. В этом случае удалю идентификационную куку
            if(MyPreferences.getInstance().getAuthCookie() != null){
                Elements loginForm = dom.select("form#user-login-form");
                if(loginForm != null && loginForm.size() == 1){
                    Log.d("surprise", "XMLParser.java 378 searchDownloadLinks: founded login FORM!!!!!!!!!!!");
                    MyPreferences.getInstance().removeAuthCookie();
                    App.sResetLoginCookie.postValue(true);
                }
            }
            Elements links = dom.select("a");
            if(links != null){
                Pattern linkPattern  = Pattern.compile("^/b/[0-9]+/([a-z0-9]+)$");
                String href;
                Matcher result;
                String type;
                ArrayList<String> types = new ArrayList<>();
                HashMap<String , String > linksList = new HashMap<>();
                for(Element link : links){
                    // проверю ссылку на соответствие формату скачивания
                    href = link.attr("href");
                    result = linkPattern.matcher(href);
                   if(result.matches()){
                       type = result.group(1);
                       if(type != null && !type.isEmpty() && !type.equals(READ_TYPE)){
                           // добавлю тип в список типов
                           if(!types.contains(type)){
                               types.add(type);
                           }
                           linksList.put(result.group(), type);
                       }

                   }
                }
                Log.d("surprise", "XMLParser searchDownloadLinks 398: found download links on page: " + linksList.size());
            }
            else{
                Log.d("surprise", "XMLParser searchDownloadLinks: not found links");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
