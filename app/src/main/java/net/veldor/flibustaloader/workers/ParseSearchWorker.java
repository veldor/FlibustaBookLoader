package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ODPSActivity;
import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.utils.Grammar;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static net.veldor.flibustaloader.utils.XMLParser.getDocument;

public class ParseSearchWorker extends Worker {
    private static final String BOOK_TYPE = "tag:book";
    private static final String GENRE_TYPE = "tag:root:genre";
    private static final String AUTHOR_TYPE = "tag:author";
    private static final String SEQUENCE_TYPE = "tag:sequences";
    private static final CharSequence AUTHOR_SEQUENCE_TYPE = ":sequence:";
    private XPath mXPath;

    public ParseSearchWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        App app = App.getInstance();
        // получу ответ
        String text = app.mResponce;
        if (text != null) {
            // получу документ
            Document document = getDocument(text);
            app.mResponce = null;
            // заполню ссылку на следующую страницу
            // попробую xpath
            XPathFactory factory = XPathFactory.newInstance();
            mXPath = factory.newXPath();
            Node entry;
            try {
                entry = (Node) mXPath.evaluate("/feed/link[@rel='next']", document, XPathConstants.NODE);
                if (entry != null) {
                    app.mNextPageUrl = entry.getAttributes().getNamedItem("href").getNodeValue();
                } else {
                    app.mNextPageUrl = null;
                }
                // получу сущности
                NodeList entries = (NodeList) mXPath.evaluate("/feed/entry", document, XPathConstants.NODESET);
                if (entries.getLength() > 0) {
                    App.getInstance().mSearchTitle.postValue(((Node) mXPath.evaluate("/feed/title", document, XPathConstants.NODE)).getTextContent());
                    // определю тип содержимого
                    identificateSearchType(entries.item(0));

                    // обработаю данные
                    switch (App.sSearchType) {
                        case ODPSActivity
                                .SEARCH_BOOKS:
                            handleBooks(entries);
                            break;
                        case ODPSActivity
                                .SEARCH_AUTHORS:
                            handleAuthors(entries);
                            break;
                        case ODPSActivity
                                .SEARCH_GENRE:
                            handleGenres(entries);
                            break;
                        case ODPSActivity
                                .SEARCH_SEQUENCE:
                            handleSequences(entries);
                            break;
                    }
                } else {
                    app.mParsedResult.postValue(null);
                }

            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
        }
        return Result.success();
    }

    private void identificateSearchType(Node item) throws XPathExpressionException {
        // получу идентификатор
        String id = ((Node) mXPath.evaluate("./id", item, XPathConstants.NODE)).getTextContent();
        if (id.startsWith(BOOK_TYPE)) {
            App.sSearchType = ODPSActivity.SEARCH_BOOKS;
        } else if (id.startsWith(GENRE_TYPE)) {
            App.sSearchType = ODPSActivity.SEARCH_GENRE;
        } else if (id.startsWith(AUTHOR_TYPE)) {
            // проверю на возможность, что загружены серии
            if (id.contains(AUTHOR_SEQUENCE_TYPE)) {
                App.sSearchType = ODPSActivity.SEARCH_SEQUENCE;
            } else {
                App.sSearchType = ODPSActivity.SEARCH_AUTHORS;
            }
        } else if (id.startsWith(SEQUENCE_TYPE)) {
            App.sSearchType = ODPSActivity.SEARCH_SEQUENCE;
        }

    }

    private void handleSequences(NodeList entries) throws XPathExpressionException {
        ArrayList<FoundedItem> result = new ArrayList<>();
        Node entry;
        FoundedSequence sequence;
        int counter = 0;
        while ((entry = entries.item(counter)) != null) {
            sequence = new FoundedSequence();
            sequence.title = ((Node) mXPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
            sequence.content = ((Node) mXPath.evaluate("./content", entry, XPathConstants.NODE)).getTextContent();
            sequence.link = ((Node) mXPath.evaluate("./link", entry, XPathConstants.NODE)).getAttributes().getNamedItem("href").getTextContent();
            result.add(sequence);
            counter++;
        }
        App.getInstance().mParsedResult.postValue(result);
    }

    private void handleGenres(NodeList entries) {
        ArrayList<FoundedItem> result = new ArrayList<>();
        Node entry;
        Genre genre;
        int counter = 0;
        while ((entry = entries.item(counter)) != null) {
            genre = new Genre();
            try {
                genre.label = ((Node) mXPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
                genre.term = ((Node) mXPath.evaluate("./link", entry, XPathConstants.NODE)).getAttributes().getNamedItem("href").getTextContent();
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
            result.add(genre);
            counter++;
        }
        App.getInstance().mParsedResult.postValue(result);
    }

    private void handleBooks(NodeList entries) throws XPathExpressionException {
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
        ArrayList<FoundedItem> result;
        // если книги добавляются к списку- вместо нового массива возьму уже имеющийся
        if (App.getInstance().mResultsEscalate) {
            result = App.getInstance().mParsedResult.getValue();
            if (result == null) {
                result = new ArrayList<>();
            }
        } else {
            result = new ArrayList<>();
        }

        StringBuilder stringBuilder = new StringBuilder();
        int counter = 0;
        int innerCounter;
        FoundedSequence sequence;

        while ((entry = entries.item(counter)) != null) {
            book = new FoundedBook();
            book.id = ((Node) mXPath.evaluate("./id", entry, XPathConstants.NODE)).getTextContent();
            // узнаю, прочитана ли книга
            AppDatabase db = App.getInstance().mDatabase;
            book.readed = db.readedBooksDao().getBookById(book.id) != null;
            book.name = ((Node) mXPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
            counter++;
            result.add(book);
            xpathResult = (NodeList) mXPath.evaluate("./author", entry, XPathConstants.NODESET);
            if (xpathResult.getLength() > 0) {
                stringBuilder.setLength(0);
                innerCounter = 0;
                while ((someNode = xpathResult.item(innerCounter)) != null) {
                    author = new Author();
                    // найду имя
                    someString = ((Node) mXPath.evaluate("./name", someNode, XPathConstants.NODE)).getTextContent();
                    author.name = someString;
                    stringBuilder.append(someString);
                    stringBuilder.append("\n");
                    author.uri = ((Node) mXPath.evaluate("./uri", someNode, XPathConstants.NODE)).getTextContent().substring(3);
                    book.authors.add(author);
                    ++innerCounter;

                }
                book.author = stringBuilder.toString();
            }
            // добавлю категории
            xpathResult = (NodeList) mXPath.evaluate("./category", entry, XPathConstants.NODESET);
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
            someString = ((Node) mXPath.evaluate("./content", entry, XPathConstants.NODE)).getTextContent();
            book.bookInfo = someString;
            book.downloadsCount = getInfoFromContent(someString, "Скачиваний:");
            book.size = getInfoFromContent(someString, "Размер:");
            book.format = getInfoFromContent(someString, "Формат:");
            book.translate = Grammar.textFromHtml(getInfoFromContent(someString, "Перевод:"));
            book.sequenceComplex = getInfoFromContent(someString, "Серия:");

            // найду ссылки на скачивание книги
            xpathResult = (NodeList) mXPath.evaluate("./link[@rel='http://opds-spec.org/acquisition/open-access']", entry, XPathConstants.NODESET);
            innerCounter = 0;
            while ((someNode = xpathResult.item(innerCounter)) != null) {
                someAttributes = someNode.getAttributes();
                downloadLink = new DownloadLink();
                downloadLink.url = someAttributes.getNamedItem("href").getTextContent();
                downloadLink.mime = someAttributes.getNamedItem("type").getTextContent();
                downloadLink.name = book.name;
                book.downloadLinks.add(downloadLink);
                innerCounter++;
            }
            // найду ссылки на серии
            xpathResult = (NodeList) mXPath.evaluate("./link[@rel='related']", entry, XPathConstants.NODESET);
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
        }
        App.getInstance().mParsedResult.postValue(result);
    }

    private void handleAuthors(NodeList entries) throws XPathExpressionException {
        ArrayList<FoundedItem> result;
        // если книги добавляются к списку- вместо нового массива возьму уже имеющийся
        if (App.getInstance().mResultsEscalate) {
            result = App.getInstance().mParsedResult.getValue();
            if (result == null) {
                result = new ArrayList<>();
            }
        } else {
            result = new ArrayList<>();
        }
        int counter = 0;
        Node entry;
        Author author;
        while ((entry = entries.item(counter)) != null) {
            author = new Author();
            author.name = ((Node) mXPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
            author.uri = explodeByDelimiter(((Node) mXPath.evaluate("./id", entry, XPathConstants.NODE)).getTextContent(), ":", 3);
            author.content = ((Node) mXPath.evaluate("./content", entry, XPathConstants.NODE)).getTextContent();
            result.add(author);
            counter++;
        }
        App.getInstance().mParsedResult.postValue(result);
    }

    private String getInfoFromContent(String item, String s) {
        int start = item.indexOf(s);
        int end = item.indexOf("<br/>", start);
        if (start > 0 && end > 0)
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
}
