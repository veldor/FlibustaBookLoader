package net.veldor.flibustaloader.parsers;

import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.utils.Grammar;
import net.veldor.flibustaloader.utils.MyPreferences;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

class BooksParser {
    public static ArrayList<FoundedBook> parse(NodeList entries, XPath xPath) throws XPathExpressionException {
        ArrayList<FoundedBook> result = new ArrayList<>();
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
        boolean hideDownloaded = MyPreferences.getInstance().isDownloadedHide();
        boolean hideDigests = MyPreferences.getInstance().isDigestsHide();

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
            if ((book.read && hideRead) || (book.downloaded && hideDownloaded)) {
                counter++;
                continue;
            }
            book.name = ((Node) xPath.evaluate("./title", entry, XPathConstants.NODE)).getTextContent();
            counter++;
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
                if (hideDigests && book.authors.size() > 3) {
                    continue;
                }
                book.author = stringBuilder.toString();
            }
            // создам название папки с именем автора
            String authorDirName;
            if (book.authors.size() == 0) {
                authorDirName = "Без автора";
            } else if (book.authors.size() == 1) {
                // создам название папки
                authorDirName = Grammar.createAuthorDirName(book.authors.get(0));
            } else if (book.authors.size() == 2) {
                authorDirName = Grammar.createAuthorDirName(book.authors.get(0)) + " " + Grammar.createAuthorDirName(book.authors.get(1));
            } else {
                authorDirName = "Антологии";
            }
            Log.d("surprise", "BooksParser parse 100: finded autor " + authorDirName);
            authorDirName = Grammar.clearDirName(authorDirName);
            Log.d("surprise", "BooksParser parse 100: author dir name is " + authorDirName);
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

            String sequenceName = null;
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
            if (book.sequenceComplex != null) {
                // буду учитывать только первую серию
                // после хеша идёт номер книги в серии, он нам не нужен
                sequenceName = book.sequenceComplex;
                if (sequenceName.indexOf("#") > 0) {
                    sequenceName = sequenceName.substring(0, sequenceName.indexOf("#"));
                }
                if (sequenceName.length() > 100) {
                    sequenceName = sequenceName.substring(0, 100);
                }
                if(sequenceName.startsWith("Серия: ")){
                    sequenceName = sequenceName.substring(7);
                }
                sequenceName = Grammar.clearDirName(sequenceName);
            }
            Log.d("surprise", "BooksParser parse 173: sequence is " + sequenceName);

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
                downloadLink.authorDirName = authorDirName;
                if(sequenceName != null){
                    downloadLink.sequenceDirName = sequenceName;
                }
                book.downloadLinks.add(downloadLink);
                innerCounter++;
            }

            // если назначена загрузка превью- гружу их
            if (isLoadPreviews) {
                someNode = ((Node) xPath.evaluate("./link[@rel='http://opds-spec.org/image']", entry, XPathConstants.NODE));
                if (someNode != null) {
                    someString = someNode.getAttributes().getNamedItem("href").getTextContent();
                    if (someString != null && !someString.isEmpty()) {
                        book.previewUrl = someString;
                    }
                }
            }
            result.add(book);
        }
        return result;
    }

    private static String getInfoFromContent(String item, String s) {
        int start = item.indexOf(s);
        int end = item.indexOf("<br/>", start);
        if (start > 0 && end > 0)
            return item.substring(start, end);
        return "";
    }
}
