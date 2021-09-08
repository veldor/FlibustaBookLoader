package net.veldor.flibustaloader.utils

import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.selections.Author
import net.veldor.flibustaloader.ui.OPDSActivity
import android.content.Intent
import net.veldor.flibustaloader.MyWebViewClient
import net.veldor.flibustaloader.http.TorWebClient
import net.veldor.flibustaloader.selections.FoundedItem
import net.veldor.flibustaloader.workers.LoadSubscriptionsWorker
import kotlin.Throws
import net.veldor.flibustaloader.selections.FoundedSequence
import net.veldor.flibustaloader.selections.Genre
import net.veldor.flibustaloader.selections.FoundedBook
import net.veldor.flibustaloader.selections.DownloadLink
import org.jsoup.Jsoup
import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.lang.Exception
import java.lang.StringBuilder
import java.util.ArrayList
import java.util.HashMap
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

object XMLParser {
    private const val BOOK_TYPE = "tag:book"
    private const val GENRE_TYPE = "tag:root:genre"
    private const val AUTHOR_TYPE = "tag:author"
    private const val SEQUENCES_TYPE = "tag:sequences"
    private const val SEQUENCE_TYPE = "tag:sequence"
    private val AUTHOR_SEQUENCE_TYPE: CharSequence = ":sequence:"
    private const val NEW_GENRES = "tag:search:new:genres"
    private const val NEW_SEQUENCES = "tag:search:new:sequence"
    private const val NEW_AUTHORS = "tag:search:new:author"
    private const val READ_TYPE = "read"
    private fun getDocument(rawText: String): Document? {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder: DocumentBuilder
        try {
            dBuilder = dbFactory.newDocumentBuilder()
            val `is` = InputSource(StringReader(rawText))
            return dBuilder.parse(`is`)
        } catch (e: Exception) {
            // ошибка поиска, предположу, что страница недоступна
            // отправлю оповещение об ошибке загрузки TOR
            val finishLoadingIntent = Intent(MyWebViewClient.TOR_CONNECT_ERROR_ACTION)
            finishLoadingIntent.putExtra(
                TorWebClient.ERROR_DETAILS,
                "Ошибка обработки переданных данных"
            )
            App.instance.sendBroadcast(finishLoadingIntent)
            e.printStackTrace()
        }
        return null
    }

    @kotlin.jvm.JvmStatic
    fun handleSearchResults(result: ArrayList<FoundedItem>, answer: String) {
        // получу документ
        val document = getDocument(answer)
        // заполню ссылку на следующую страницу
        // попробую xpath
        val factory = XPathFactory.newInstance()
        val xPath = factory.newXPath()
        val entry: Node
        try {
            entry = xPath.evaluate("/feed/link[@rel='next']", document, XPathConstants.NODE) as Node
            LoadSubscriptionsWorker.sNextPage =
                entry.attributes.getNamedItem("href").nodeValue
            // получу сущности
            val entries =
                xPath.evaluate("/feed/entry", document, XPathConstants.NODESET) as NodeList
            if (entries.length > 0) {
                App.instance.mSearchTitle.postValue(
                    (xPath.evaluate(
                        "/feed/title",
                        document,
                        XPathConstants.NODE
                    ) as Node).textContent
                )
                // определю тип содержимого
                identificationSearchType(entries.item(0), xPath)
                when (App.sSearchType) {
                    OPDSActivity.SEARCH_BOOKS -> handleBooks(entries, result, xPath)
                    OPDSActivity.SEARCH_AUTHORS, OPDSActivity.SEARCH_NEW_AUTHORS -> handleAuthors(
                        entries,
                        result,
                        xPath
                    )
                    OPDSActivity.SEARCH_GENRE -> handleGenres(entries, result, xPath)
                    OPDSActivity.SEARCH_SEQUENCE -> handleSequences(entries, result, xPath)
                }
            }
        } catch (e: XPathExpressionException) {
            e.printStackTrace()
        }
    }

    @Throws(XPathExpressionException::class)
    private fun identificationSearchType(item: Node, xPath: XPath) {
        // получу идентификатор
        val id = (xPath.evaluate("./id", item, XPathConstants.NODE) as Node).textContent
        //Log.d("surprise", "ParseSearchWorker identificationSearchType " + id);
        if (id.startsWith(BOOK_TYPE)) {
            App.sSearchType = OPDSActivity.SEARCH_BOOKS
        } else if (id.startsWith(GENRE_TYPE)) {
            App.sSearchType = OPDSActivity.SEARCH_GENRE
        } else if (id.startsWith(AUTHOR_TYPE)) {
            // проверю на возможность, что загружены серии
            if (id.contains(AUTHOR_SEQUENCE_TYPE)) {
                //Log.d("surprise", "ParseSearchWorker identificationSearchType author sequence");
                App.sSearchType = OPDSActivity.SEARCH_SEQUENCE
            } else {
                App.sSearchType = OPDSActivity.SEARCH_AUTHORS
            }
        } else if (id.startsWith(SEQUENCES_TYPE)) {
            //Log.d("surprise", "ParseSearchWorker identificationSearchType sequenceS");
            App.sSearchType = OPDSActivity.SEARCH_SEQUENCE
        } else if (id.startsWith(SEQUENCE_TYPE)) {
            //Log.d("surprise", "ParseSearchWorker identificationSearchType sequence");
            App.sSearchType = OPDSActivity.SEARCH_SEQUENCE
        } else if (id.startsWith(NEW_GENRES)) {
            App.sSearchType = OPDSActivity.SEARCH_GENRE
        } else if (id.startsWith(NEW_SEQUENCES)) {
            App.sSearchType = OPDSActivity.SEARCH_SEQUENCE
        } else if (id.startsWith(NEW_AUTHORS)) {
            App.sSearchType = OPDSActivity.SEARCH_NEW_AUTHORS
        } else {
            Log.d("surprise", "ParseSearchWorker identificationSearchType я ничего не понял $id")
        }
        //Log.d("surprise", "ParseSearchWorker identificationSearchType " + App.sSearchType);
    }

    @Throws(XPathExpressionException::class)
    private fun handleSequences(entries: NodeList, result: ArrayList<FoundedItem>, xPath: XPath) {
        var entry: Node?
        var sequence: FoundedSequence
        var counter = 0
        val entriesLength = entries.length
        var handledEntryCounter = 0
        while (entries.item(counter).also { entry = it } != null) {
            ++handledEntryCounter
            App.instance.mLoadAllStatus.postValue("Обрабатываю серию $handledEntryCounter из $entriesLength")
            sequence = FoundedSequence()
            sequence.title =
                (xPath.evaluate("./title", entry, XPathConstants.NODE) as Node).textContent
            sequence.content =
                (xPath.evaluate("./content", entry, XPathConstants.NODE) as Node).textContent
            sequence.link = (xPath.evaluate(
                "./link",
                entry,
                XPathConstants.NODE
            ) as Node).attributes.getNamedItem("href").textContent
            result.add(sequence)
            counter++
        }
    }

    private fun handleGenres(entries: NodeList, result: ArrayList<FoundedItem>, xPath: XPath) {
        var entry: Node?
        var genre: Genre
        var counter = 0
        val entriesLength = entries.length
        var handledEntryCounter = 0
        while (entries.item(counter).also { entry = it } != null) {
            ++handledEntryCounter
            App.instance.mLoadAllStatus.postValue("Обрабатываю жанр $handledEntryCounter из $entriesLength")
            genre = Genre()
            try {
                genre.label =
                    (xPath.evaluate("./title", entry, XPathConstants.NODE) as Node).textContent
                genre.term = (xPath.evaluate(
                    "./link",
                    entry,
                    XPathConstants.NODE
                ) as Node).attributes.getNamedItem("href").textContent
            } catch (e: XPathExpressionException) {
                e.printStackTrace()
            }
            result.add(genre)
            counter++
        }
    }

    @Throws(XPathExpressionException::class)
    private fun handleBooks(entries: NodeList, result: ArrayList<FoundedItem>, xPath: XPath) {
        val isLoadPreviews = PreferencesHandler.instance.isPreviews
        // обработаю найденные книги
        var entry: Node?
        var someNode: Node?
        var someString: String?
        var someAttributes: NamedNodeMap
        var xpathResult: NodeList
        var book: FoundedBook
        var downloadLink: DownloadLink
        var author: Author
        var genre: Genre
        val stringBuilder = StringBuilder()
        var counter = 0
        var innerCounter: Int
        var sequence: FoundedSequence
        val hideRead = PreferencesHandler.instance.isHideRead
        val entriesLength = entries.length
        var handledEntryCounter = 0
        while (entries.item(counter).also { entry = it } != null) {
            ++handledEntryCounter
            App.instance.mLoadAllStatus.postValue("Обрабатываю книгу $handledEntryCounter из $entriesLength")
            book = FoundedBook()
            book.id = (xPath.evaluate("./id", entry, XPathConstants.NODE) as Node).textContent
            // узнаю, прочитана ли книга
            val db = App.instance.mDatabase
            book.read = db.readBooksDao().getBookById(book.id) != null
            book.downloaded = db.downloadedBooksDao().getBookById(book.id) != null
            if (book.read && hideRead) {
                counter++
                continue
            }
            book.name = (xPath.evaluate("./title", entry, XPathConstants.NODE) as Node).textContent
            counter++
            result.add(book)
            xpathResult = xPath.evaluate("./author", entry, XPathConstants.NODESET) as NodeList
            if (xpathResult.length > 0) {
                stringBuilder.setLength(0)
                innerCounter = 0
                while (xpathResult.item(innerCounter).also { someNode = it } != null) {
                    author = Author()
                    // найду имя
                    someString = (xPath.evaluate(
                        "./name",
                        someNode,
                        XPathConstants.NODE
                    ) as Node).textContent
                    author.name = someString
                    stringBuilder.append(someString)
                    stringBuilder.append("\n")
                    author.uri = (xPath.evaluate(
                        "./uri",
                        someNode,
                        XPathConstants.NODE
                    ) as Node).textContent.substring(3)
                    book.authors.add(author)
                    ++innerCounter
                }
                book.author = stringBuilder.toString()
            }
            // добавлю категории
            xpathResult = xPath.evaluate("./category", entry, XPathConstants.NODESET) as NodeList
            if (xpathResult.length > 0) {
                stringBuilder.setLength(0)
                innerCounter = 0
                while (xpathResult.item(innerCounter).also { someNode = it } != null) {
                    someAttributes = someNode!!.attributes
                    someString = someAttributes.getNamedItem("label").textContent
                    stringBuilder.append(someString)
                    stringBuilder.append("\n")
                    // добавлю жанр
                    genre = Genre()
                    genre.label = someString
                    genre.term = someAttributes.getNamedItem("term").textContent
                    ++innerCounter
                    book.genres.add(genre)
                }
                book.genreComplex = stringBuilder.toString()
            }

            // разберу информацию о книге
            someString =
                (xPath.evaluate("./content", entry, XPathConstants.NODE) as Node).textContent
            book.bookInfo = someString
            book.downloadsCount = getInfoFromContent(someString, "Скачиваний:")
            book.size = getInfoFromContent(someString, "Размер:")
            book.format = getInfoFromContent(someString, "Формат:")
            book.translate = Grammar.textFromHtml(getInfoFromContent(someString, "Перевод:"))
            book.sequenceComplex = getInfoFromContent(someString, "Серия:")

            // найду ссылки на скачивание книги
            xpathResult = xPath.evaluate(
                "./link[@rel='http://opds-spec.org/acquisition/open-access']",
                entry,
                XPathConstants.NODESET
            ) as NodeList
            innerCounter = 0
            while (xpathResult.item(innerCounter).also { someNode = it } != null) {
                someAttributes = someNode!!.attributes
                downloadLink = DownloadLink()
                downloadLink.id = book.id
                downloadLink.url = someAttributes.getNamedItem("href").textContent
                downloadLink.mime = someAttributes.getNamedItem("type").textContent
                downloadLink.name = book.name
                downloadLink.author = book.author
                downloadLink.size = book.size
                book.downloadLinks.add(downloadLink)
                innerCounter++
            }
            // найду ссылки на серии
            xpathResult =
                xPath.evaluate("./link[@rel='related']", entry, XPathConstants.NODESET) as NodeList
            innerCounter = 0
            while (xpathResult.item(innerCounter).also { someNode = it } != null) {
                someAttributes = someNode!!.attributes
                someString = someAttributes.getNamedItem("href").textContent
                if (someString.startsWith("/opds/sequencebooks/")) {
                    // Найдена серия
                    sequence = FoundedSequence()
                    sequence.link = someString
                    sequence.title = someAttributes.getNamedItem("title").textContent
                    book.sequences.add(sequence)
                }
                innerCounter++
            }

            // если назначена загрузка превью- гружу их
            if (isLoadPreviews) {
                someNode = xPath.evaluate(
                    "./link[@rel='http://opds-spec.org/image']",
                    entry,
                    XPathConstants.NODE
                ) as Node
                if (someNode != null) {
                    someString = someNode!!.attributes.getNamedItem("href").textContent
                    if (someString != null && someString.isNotEmpty()) {
                        book.previewUrl = someString
                    }
                }
            }
        }
    }

    @Throws(XPathExpressionException::class)
    private fun handleAuthors(entries: NodeList, result: ArrayList<FoundedItem>, xPath: XPath) {
        var counter = 0
        var entry: Node?
        var author: Author
        val entriesLength = entries.length
        var handledEntryCounter = 0
        while (entries.item(counter).also { entry = it } != null) {
            ++handledEntryCounter
            App.instance.mLoadAllStatus.postValue("Обрабатываю автора $handledEntryCounter из $entriesLength")
            author = Author()
            author.name =
                (xPath.evaluate("./title", entry, XPathConstants.NODE) as Node).textContent
            // если поиск осуществляется по новинкам- запишу ссылку на новинки, иначе- на автора
            if (App.sSearchType == OPDSActivity.SEARCH_NEW_AUTHORS) {
                author.link = (xPath.evaluate(
                    "./link",
                    entry,
                    XPathConstants.NODE
                ) as Node).attributes.getNamedItem("href").textContent
            } else {
                author.uri = explodeByDelimiter(
                    (xPath.evaluate(
                        "./id",
                        entry,
                        XPathConstants.NODE
                    ) as Node).textContent
                )
            }
            author.content =
                (xPath.evaluate("./content", entry, XPathConstants.NODE) as Node).textContent
            result.add(author)
            counter++
        }
    }

    private fun getInfoFromContent(item: String?, s: String): String {
        val start = item!!.indexOf(s)
        val end = item.indexOf("<br/>", start)
        return if (start > 0 && end > 0) item.substring(start, end) else ""
    }

    private fun explodeByDelimiter(s: String): String? {
        val result = s.split(":").toTypedArray()
        return if (result.size < 3) {
            null
        } else result[3 - 1]
    }

    @kotlin.jvm.JvmStatic
    fun searchDownloadLinks(content: InputStream?) {
        try {
            val dom: org.jsoup.nodes.Document
            val url = "http://rutracker.org"
            dom = Jsoup.parse(content, "UTF-8", url)
            // попробую найти форму входа. Если она найдена- значит, вход не выполнен. В этом случае удалю идентификационную куку
            if (PreferencesHandler.instance.authCookie != null) {
                val loginForm = dom.select("form#user-login-form")
                if (loginForm != null && loginForm.size == 1) {
                    Log.d(
                        "surprise",
                        "XMLParser.java 378 searchDownloadLinks: founded login FORM!!!!!!!!!!!"
                    )
                    PreferencesHandler.instance.authCookie = null
                    App.sResetLoginCookie.postValue(true)
                }
            }
            val links = dom.select("a")
            if (links != null) {
                val linkPattern = Pattern.compile("^/b/[0-9]+/([a-z0-9]+)$")
                var href: String
                var result: Matcher
                var type: String?
                val types = ArrayList<String>()
                val linksList = HashMap<String, String>()
                for (link in links) {
                    // проверю ссылку на соответствие формату скачивания
                    href = link.attr("href")
                    result = linkPattern.matcher(href)
                    if (result.matches()) {
                        type = result.group(1)
                        if (type != null && type.isNotEmpty() && type != READ_TYPE) {
                            // добавлю тип в список типов
                            if (!types.contains(type)) {
                                types.add(type)
                            }
                            linksList[result.group()] = type
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}