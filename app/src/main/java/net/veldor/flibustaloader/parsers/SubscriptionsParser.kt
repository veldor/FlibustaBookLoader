package net.veldor.flibustaloader.parsers

import net.veldor.flibustaloader.utils.Grammar.textFromHtml
import kotlin.Throws
import net.veldor.flibustaloader.selections.FoundedBook
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.Author
import net.veldor.flibustaloader.selections.Genre
import net.veldor.flibustaloader.selections.FoundedSequence
import net.veldor.flibustaloader.selections.SubscriptionItem
import android.content.Intent
import android.util.Log
import net.veldor.flibustaloader.MyWebViewClient
import net.veldor.flibustaloader.http.TorWebClient
import org.w3c.dom.Document
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.lang.StringBuilder
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

object SubscriptionsParser {
    @Throws(XPathExpressionException::class)
    fun handleSearchResults(
        result: ArrayList<FoundedBook>,
        answer: String,
        subscribes: ArrayList<SubscriptionItem>?
    ) {
        val document = getDocument(answer)
        val xpath = XPathFactory.newInstance().newXPath()
        if (subscribes != null && subscribes.size > 0) {
            for (si in subscribes) {
                // проверю тип подписки.
                when (si.type) {
                    "author" -> foundAuthor(document, xpath, si.name!!, result)
                    "book" -> foundBook(document, xpath, si.name!!, result)
                    "sequence" -> foundSequence(document, xpath, si.name!!, result)
                }
            }
        }
    }

    @Throws(XPathExpressionException::class)
    private fun foundSequence(
        document: Document?,
        xpath: XPath,
        incomingName: String,
        result: ArrayList<FoundedBook>
    ) {
        var name = incomingName
        name = name.lowercase(Locale.getDefault())
        var value: String
        // Найду автора по данным
        val contentWithSequences = xpath.evaluate(
            "/feed/entry/content[contains(text(),'Серия: ')]",
            document,
            XPathConstants.NODESET
        ) as NodeList
        if (contentWithSequences.length > 0) {
            var counter = 0
            var content: Node
            while (contentWithSequences.item(counter).also { content = it } != null) {
                value = content.textContent.lowercase(Locale.getDefault())
                if (value.substring(value.lastIndexOf("серия: ")).contains(name)) {
                    // добавлю найденный результат
                    addBookToResult(content.parentNode, result, xpath)
                }
                counter++
            }
        }
    }

    @Throws(XPathExpressionException::class)
    private fun foundAuthor(
        document: Document?,
        xpath: XPath,
        incomingName: String,
        result: ArrayList<FoundedBook>
    ) {
        var name = incomingName
        name = name.lowercase(Locale.getDefault())
        Log.d("surprise", "SubscriptionsParser foundAuthor 80: search author $name")
        var value: String
        // Найду автора по данным
        val authors =
            xpath.evaluate("/feed/entry/author/name", document, XPathConstants.NODESET) as NodeList
        if (authors.length > 0) {
            var counter = 0
            var author: Node
            while (authors.item(counter).also { author = it } != null) {
                value = author.textContent.lowercase(Locale.getDefault())
                Log.d("surprise", "SubscriptionsParser foundAuthor 88: found author $value")
                if (value.contains(name)) {
                    // добавлю найденный результат
                    addBookToResult(author.parentNode.parentNode, result, xpath)
                }
                counter++
            }
        }
    }

    @Throws(XPathExpressionException::class)
    private fun foundBook(
        document: Document?,
        xpath: XPath,
        incomingName: String,
        result: ArrayList<FoundedBook>
    ) {
        var name = incomingName
        name = name.lowercase(Locale.getDefault())
        var value: String
        // Найду title, в котором пристутствует выбранное слово
        val titles =
            xpath.evaluate("/feed/entry/title", document, XPathConstants.NODESET) as NodeList
        if (titles.length > 0) {
            var counter = 0
            var title: Node
            while (titles.item(counter).also { title = it } != null) {
                value = title.textContent.lowercase(Locale.getDefault())
                if (value.contains(name)) {
                    // добавлю найденный результат
                    addBookToResult(title.parentNode, result, xpath)
                    return
                }
                counter++
            }
        }
    }

    private fun getDocument(rawText: String): Document? {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder: DocumentBuilder
        try {
            dBuilder = dbFactory.newDocumentBuilder()
            val `is` = InputSource(StringReader(rawText))
            return dBuilder.parse(`is`)
        } catch (e: ParserConfigurationException) {
            // ошибка поиска, предположу, что страница недоступна
            // отправлю оповещение об ошибке загрузки TOR
            val finishLoadingIntent = Intent(MyWebViewClient.TOR_CONNECT_ERROR_ACTION)
            finishLoadingIntent.putExtra(TorWebClient.ERROR_DETAILS, e.message)
            App.instance.sendBroadcast(finishLoadingIntent)
            e.printStackTrace()
        } catch (e: IOException) {
            val finishLoadingIntent = Intent(MyWebViewClient.TOR_CONNECT_ERROR_ACTION)
            finishLoadingIntent.putExtra(TorWebClient.ERROR_DETAILS, e.message)
            App.instance.sendBroadcast(finishLoadingIntent)
            e.printStackTrace()
        } catch (e: SAXException) {
            val finishLoadingIntent = Intent(MyWebViewClient.TOR_CONNECT_ERROR_ACTION)
            finishLoadingIntent.putExtra(TorWebClient.ERROR_DETAILS, e.message)
            App.instance.sendBroadcast(finishLoadingIntent)
            e.printStackTrace()
        }
        return null
    }

    @Throws(XPathExpressionException::class)
    private fun addBookToResult(bookNode: Node, result: ArrayList<FoundedBook>, xPath: XPath) {
        var someNode: Node
        var someString: String
        var someAttributes: NamedNodeMap
        var downloadLink: DownloadLink
        var author: Author
        var genre: Genre
        val stringBuilder = StringBuilder()
        var innerCounter: Int
        var sequence: FoundedSequence
        val book = FoundedBook()
        book.id = (xPath.evaluate("./id", bookNode, XPathConstants.NODE) as Node).textContent
        book.name = (xPath.evaluate("./title", bookNode, XPathConstants.NODE) as Node).textContent
        var xpathResult: NodeList = xPath.evaluate("./author", bookNode, XPathConstants.NODESET) as NodeList
        if (xpathResult.length > 0) {
            stringBuilder.setLength(0)
            innerCounter = 0
            while (xpathResult.item(innerCounter).also { someNode = it } != null) {
                author = Author()
                // найду имя
                someString =
                    (xPath.evaluate("./name", someNode, XPathConstants.NODE) as Node).textContent
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
        xpathResult = xPath.evaluate("./category", bookNode, XPathConstants.NODESET) as NodeList
        if (xpathResult.length > 0) {
            stringBuilder.setLength(0)
            innerCounter = 0
            while (xpathResult.item(innerCounter).also { someNode = it } != null) {
                someAttributes = someNode.attributes
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
            (xPath.evaluate("./content", bookNode, XPathConstants.NODE) as Node).textContent
        book.bookInfo = someString
        book.downloadsCount = getInfoFromContent(someString, "Скачиваний:")
        book.size = getInfoFromContent(someString, "Размер:")
        book.format = getInfoFromContent(someString, "Формат:")
        book.translate = textFromHtml(getInfoFromContent(someString, "Перевод:"))
        book.sequenceComplex = getInfoFromContent(someString, "Серия:")

        // найду ссылки на скачивание книги
        xpathResult = xPath.evaluate(
            "./link[@rel='http://opds-spec.org/acquisition/open-access']",
            bookNode,
            XPathConstants.NODESET
        ) as NodeList
        innerCounter = 0
        while (xpathResult.item(innerCounter).also { someNode = it } != null) {
            someAttributes = someNode.attributes
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
            xPath.evaluate("./link[@rel='related']", bookNode, XPathConstants.NODESET) as NodeList
        innerCounter = 0
        while (xpathResult.item(innerCounter).also { someNode = it } != null) {
            someAttributes = someNode.attributes
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
        result.add(book)
    }

    private fun getInfoFromContent(item: String, s: String): String {
        val start = item.indexOf(s)
        val end = item.indexOf("<br/>", start)
        return if (start > 0 && end > 0) item.substring(start, end) else ""
    }
}