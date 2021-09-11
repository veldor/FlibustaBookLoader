package net.veldor.flibustaloader.parsers

import net.veldor.flibustaloader.utils.Grammar.createAuthorDirName
import net.veldor.flibustaloader.utils.Grammar.clearDirName
import net.veldor.flibustaloader.utils.Grammar.textFromHtml
import kotlin.Throws
import net.veldor.flibustaloader.selections.FoundedBook
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.Author
import net.veldor.flibustaloader.selections.Genre
import net.veldor.flibustaloader.selections.BlacklistItem
import net.veldor.flibustaloader.selections.FoundedSequence
import android.util.Log
import net.veldor.flibustaloader.utils.PreferencesHandler
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.lang.StringBuilder
import java.util.*
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException

internal object BooksParser {
    @Throws(XPathExpressionException::class)
    fun parse(
        entries: NodeList,
        xPath: XPath,
        reservedSequenceName: String?
    ): ArrayList<FoundedBook> {
        val result = ArrayList<FoundedBook>()
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
        var filteredCounter = 0
        val isFilter = PreferencesHandler.instance.isUseFilter
        val onlyRussian = PreferencesHandler.instance.isOnlyRussian
        var filterBooks: ArrayList<BlacklistItem>? = ArrayList()
        var filterAuthors: ArrayList<BlacklistItem>? = ArrayList()
        var filterSequences: ArrayList<BlacklistItem>? = ArrayList()
        var filterGenres: ArrayList<BlacklistItem>? = ArrayList()
        if (isFilter) {
            // фильтры
            filterBooks = App.instance.booksBlacklist.getBlacklist()
            filterAuthors = App.instance.authorsBlacklist.getBlacklist()
            filterSequences = App.instance.sequencesBlacklist.getBlacklist()
            filterGenres = App.instance.genresBlacklist.getBlacklist()
        }
        val stringBuilder = StringBuilder()
        var counter = 0
        var innerCounter: Int
        var sequence: FoundedSequence
        val hideRead = PreferencesHandler.instance.isHideRead
        val hideDownloaded = PreferencesHandler.instance.isHideDownloaded
        val hideDigests = PreferencesHandler.instance.isHideDigests
        val entriesLength = entries.length
        var handledEntryCounter = 0
        while (entries.item(counter).also { entry = it } != null) {
            var skip = false
            ++handledEntryCounter
            book = FoundedBook()
            book.id = (xPath.evaluate("./id", entry, XPathConstants.NODE) as Node).textContent
            // узнаю, прочитана ли книга
            val db = App.instance.mDatabase
            book.read = db.readBooksDao().getBookById(book.id) != null
            book.downloaded = db.downloadedBooksDao().getBookById(book.id) != null
            if (book.read && hideRead || book.downloaded && hideDownloaded) {
                counter++
                continue
            }
            book.name = (xPath.evaluate("./title", entry, XPathConstants.NODE) as Node).textContent
            if (isFilter && filterBooks!!.size > 0) {
                for (item in filterBooks) {
                    if (book.name!!
                            .lowercase(Locale.getDefault())
                            .contains(item.name!!.lowercase(Locale.getDefault()))) {
                        Log.d(
                            "surprise",
                            "BooksParser.java 84 parse: skipped " + book.name + " to " + item.name
                        )
                        skip = true
                        filteredCounter++
                        break
                    }
                }
                if (skip) {
                    // пропускаю книгу
                    counter++
                    continue
                }
            }
            counter++
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
                    if (filterAuthors!!.size > 0) {
                        for (item in filterAuthors) {
                            if (author.name!!
                                    .lowercase(Locale.getDefault())
                                    .contains(item.name!!.lowercase(
                                    Locale.getDefault()
                                ))) {
                                Log.d(
                                    "surprise",
                                    "BooksParser.java 84 parse: skipped " + author.name + " by " + item.name
                                )
                                skip = true
                                filteredCounter++
                                break
                            }
                        }
                    }
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
                if (skip) {
                    // пропускаю книгу
                    continue
                }
                if (hideDigests && book.authors.size > 3) {
                    continue
                }
                book.author = stringBuilder.toString()
            }
            // создам название папки с именем автора
            var authorDirName: String = when (book.authors.size) {
                0 -> {
                    "Без автора"
                }
                1 -> {
                    // создам название папки
                    createAuthorDirName(book.authors[0])
                }
                2 -> {
                    createAuthorDirName(book.authors[0]) + " " + createAuthorDirName(
                        book.authors[1]
                    )
                }
                else -> {
                    "Антологии"
                }
            }
            authorDirName = clearDirName(authorDirName)
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
                    if (filterGenres!!.size > 0) {
                        for (item in filterGenres) {
                            if (genre.label!!.lowercase(Locale.getDefault()).contains(
                                    item.name!!.lowercase(
                                        Locale.getDefault()
                                    )
                                )
                            ) {
                                skip = true
                                filteredCounter++
                                Log.d(
                                    "surprise",
                                    "BooksParser.java 84 parse: skipped " + genre.label + " by " + item.name
                                )
                                break
                            }
                        }
                    }
                    genre.term = someAttributes.getNamedItem("term").textContent
                    ++innerCounter
                    book.genres.add(genre)
                }
                if (skip) {
                    // пропускаю книгу
                    Log.d("surprise", "BooksParser.java 85 parse: skipped by filter genre")
                    continue
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
            book.translate = textFromHtml(getInfoFromContent(someString, "Перевод:"))
            book.sequenceComplex = getInfoFromContent(someString, "Серия:")
            var sequenceName: String? = null
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
                    if (filterSequences!!.size > 0) {
                        for (item in filterSequences) {
                            if (sequence.title!!.lowercase(Locale.getDefault()).contains(
                                    item.name!!.lowercase(
                                        Locale.ROOT
                                    )
                                )
                            ) {
                                filteredCounter++
                                Log.d(
                                    "surprise",
                                    "BooksParser.java 84 parse: skipped " + sequence.title + " by " + item.name
                                )
                                skip = true
                                break
                            }
                        }
                    }
                    book.sequences.add(sequence)
                }
                innerCounter++
            }
            if (skip) {
                // пропускаю книгу
                continue
            }
            if (book.sequenceComplex != null) {
                // буду учитывать только первую серию
                // после хеша идёт номер книги в серии, он нам не нужен
                sequenceName = book.sequenceComplex!!
                if (sequenceName.indexOf("#") > 0) {
                    sequenceName = sequenceName.substring(0, sequenceName.indexOf("#"))
                }
                if (sequenceName.length > 100) {
                    sequenceName = sequenceName.substring(0, 100)
                }
                if (sequenceName.startsWith("Серия: ")) {
                    sequenceName = sequenceName.substring(7)
                }
                sequenceName = clearDirName(sequenceName)
            }

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
                downloadLink.reservedSequenceName = reservedSequenceName
                downloadLink.authorDirName = authorDirName
                if (sequenceName != null) {
                    downloadLink.sequenceDirName = sequenceName
                }
                book.downloadLinks.add(downloadLink)
                innerCounter++
            }

            // найду ссылку на книгу на сайте
            xpathResult = xPath.evaluate(
                "./link[@title='Книга на сайте']",
                entry,
                XPathConstants.NODESET
            ) as NodeList
            if (xpathResult.length == 1) {
                book.bookLink = xpathResult.item(0).attributes.getNamedItem("href").textContent
            }

            // язык
            xpathResult = xPath.evaluate("./language", entry, XPathConstants.NODESET) as NodeList
            if (xpathResult.length == 1) {
                book.bookLanguage = xpathResult.item(0).textContent
                if (onlyRussian && book.bookLanguage != "ru") {
                    filteredCounter++
                    continue
                }
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
            result.add(book)
        }
        if (isFilter) {
            Log.d("surprise", "BooksParser.java 276 parse: filtered $filteredCounter")
        }
        return result
    }

    private fun getInfoFromContent(item: String?, s: String): String {
        val start = item!!.indexOf(s)
        val end = item.indexOf("<br/>", start)
        return if (start > 0 && end > 0) item.substring(start, end) else ""
    }
}