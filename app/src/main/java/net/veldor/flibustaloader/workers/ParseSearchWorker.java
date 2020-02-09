package net.veldor.flibustaloader.workers;

import android.content.Context;
import androidx.annotation.NonNull;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.OPDSActivity;
import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.utils.Grammar;
import net.veldor.flibustaloader.utils.SortHandler;

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
    private static final String SEQUENCES_TYPE = "tag:sequences";
    private static final String SEQUENCE_TYPE = "tag:sequence";
    private static final CharSequence AUTHOR_SEQUENCE_TYPE = ":sequence:";
    private static final String NEW_GENRES = "tag:search:new:genres";
    private static final String NEW_SEQUENCES = "tag:search:new:sequence";
    private static final String NEW_AUTHORS = "tag:search:new:author";
    private XPath mXPath;
    private boolean mIsStopped;

    public ParseSearchWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        App app = App.getInstance();
        // получу ответ
        String text = app.mResponse;
        if (text != null) {
            // получу документ
            Document document = getDocument(text);
            app.mResponse = null;
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
                    identificationSearchType(entries.item(0));
                    // обработаю данные
                    switch (App.sSearchType) {
                        case OPDSActivity
                                .SEARCH_BOOKS:
                            handleBooks(entries);
                            break;
                        case OPDSActivity
                                .SEARCH_AUTHORS:
                        case OPDSActivity
                                .SEARCH_NEW_AUTHORS:
                            handleAuthors(entries);
                            break;
                        case OPDSActivity
                                .SEARCH_GENRE:
                            handleGenres(entries);
                            break;
                        case OPDSActivity
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

    private void identificationSearchType(Node item) throws XPathExpressionException {
        // получу идентификатор
        String id = ((Node) mXPath.evaluate("./id", item, XPathConstants.NODE)).getTextContent();
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
        } else if (id.startsWith(SEQUENCE_TYPE)) {
            //Log.d("surprise", "ParseSearchWorker identificationSearchType sequence");
            App.sSearchType = OPDSActivity.SEARCH_SEQUENCE;
        } else if (id.startsWith(NEW_GENRES)) {
            App.sSearchType = OPDSActivity.SEARCH_GENRE;
        } else if (id.startsWith(NEW_SEQUENCES)) {
            App.sSearchType = OPDSActivity.SEARCH_SEQUENCE;
        } else if (id.startsWith(NEW_AUTHORS)) {
            App.sSearchType = OPDSActivity.SEARCH_NEW_AUTHORS;
        }
        //Log.d("surprise", "ParseSearchWorker identificationSearchType " + App.sSearchType);
    }

    private void handleSequences(NodeList entries) throws XPathExpressionException {
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
        Node entry;
        FoundedSequence sequence;
        int counter = 0;
        int entriesLength = entries.getLength();
        int handledEntryCounter = 0;
        while ((entry = entries.item(counter)) != null && !mIsStopped) {
            ++handledEntryCounter;
            App.getInstance().mLoadAllStatus.postValue("Обрабатываю серию " + handledEntryCounter + " из " + entriesLength);
            sequence = new FoundedSequence();
            sequence.title = ((Node) mXPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
            sequence.content = ((Node) mXPath.evaluate("./content", entry, XPathConstants.NODE)).getTextContent();
            sequence.link = ((Node) mXPath.evaluate("./link", entry, XPathConstants.NODE)).getAttributes().getNamedItem("href").getTextContent();
            result.add(sequence);
            counter++;
        }
        SortHandler.sortSequences(result);
        if (!mIsStopped)
            App.getInstance().mParsedResult.postValue(result);
    }

    private void handleGenres(NodeList entries) {
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
        Node entry;
        Genre genre;
        int counter = 0;
        int entriesLength = entries.getLength();
        int handledEntryCounter = 0;
        while ((entry = entries.item(counter)) != null && !mIsStopped) {
            ++handledEntryCounter;
            App.getInstance().mLoadAllStatus.postValue("Обрабатываю жанр " + handledEntryCounter + " из " + entriesLength);
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
        // отсортирую результат
        SortHandler.sortGenres(result);
        if (!mIsStopped)
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

        boolean hideRead = App.getInstance().isHideRead();

        int entriesLength = entries.getLength();
        int handledEntryCounter = 0;

        while ((entry = entries.item(counter)) != null && !mIsStopped) {
            ++handledEntryCounter;
            App.getInstance().mLoadAllStatus.postValue("Обрабатываю книгу " + handledEntryCounter + " из " + entriesLength);
            book = new FoundedBook();
            book.id = ((Node) mXPath.evaluate("./id", entry, XPathConstants.NODE)).getTextContent();
            // узнаю, прочитана ли книга
            AppDatabase db = App.getInstance().mDatabase;
            book.read = db.readBooksDao().getBookById(book.id) != null;
            book.downloaded = db.downloadedBooksDao().getBookById(book.id) != null;
            if (book.read && hideRead) {
                counter++;
                continue;
            }
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
            while ((someNode = xpathResult.item(innerCounter)) != null && !mIsStopped) {
                someAttributes = someNode.getAttributes();
                downloadLink = new DownloadLink();
                downloadLink.id = book.id;
                downloadLink.url = someAttributes.getNamedItem("href").getTextContent();
                downloadLink.mime = someAttributes.getNamedItem("type").getTextContent();
                downloadLink.name = book.name;
                downloadLink.author = book.author;
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
        SortHandler.sortBooks(result);
        if (!mIsStopped)
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
        int entriesLength = entries.getLength();
        int handledEntryCounter = 0;
        Node entry;
        Author author;
        while ((entry = entries.item(counter)) != null && !mIsStopped) {
            ++handledEntryCounter;
            App.getInstance().mLoadAllStatus.postValue("Обрабатываю автора " + handledEntryCounter + " из " + entriesLength);
            author = new Author();
            author.name = ((Node) mXPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
            // если поиск осуществляется по новинкам- запишу ссылку на новинки, иначе- на автора
            if (App.sSearchType == OPDSActivity.SEARCH_NEW_AUTHORS) {
                author.link = ((Node) mXPath.evaluate("./link", entry, XPathConstants.NODE)).getAttributes().getNamedItem("href").getTextContent();
            } else {
                author.uri = explodeByDelimiter(((Node) mXPath.evaluate("./id", entry, XPathConstants.NODE)).getTextContent());
            }

            author.content = ((Node) mXPath.evaluate("./content", entry, XPathConstants.NODE)).getTextContent();
            result.add(author);
            counter++;
        }
        SortHandler.sortAuthors(result);
        if (!mIsStopped)
            App.getInstance().mParsedResult.postValue(result);
    }

    private String getInfoFromContent(String item, String s) {
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

    @Override
    public void onStopped() {
        super.onStopped();
        mIsStopped = true;
        // остановлю процесс
    }
}
