package net.veldor.flibustaloader.utils

import android.util.Log
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.database.entity.Bookmark
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.database.entity.DownloadedBooks
import net.veldor.flibustaloader.database.entity.ReadedBooks
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.*
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

object XMLHandler {
    private const val SEARCH_VALUE_NAME = "string"
    fun getSearchAutocomplete(autocompleteText: String?): ArrayList<String> {
        val searchValues = ArrayList<String>()
        val searchList = getDocument(autocompleteText)
        // найду значения строк
        if (searchList != null) {
            val values = searchList.getElementsByTagName(SEARCH_VALUE_NAME)
            var counter = 0
            while (values.item(counter) != null) {
                searchValues.add(values.item(counter).firstChild.nodeValue)
                ++counter
            }
        }
        return searchValues
    }

    private fun getSearchAutocomplete(searchList: Document?): ArrayList<String> {
        val searchValues = ArrayList<String>()
        // найду значения строк
        val values = searchList!!.getElementsByTagName(SEARCH_VALUE_NAME)
        var counter = 0
        while (values.item(counter) != null) {
            searchValues.add(values.item(counter).firstChild.nodeValue)
            ++counter
        }
        return searchValues
    }

    @kotlin.jvm.JvmStatic
    fun putSearchValue(s: String): Boolean {
        // получу содержимое файла
        val rawXml = MyFileReader.getSearchAutocomplete()
        val dom = getDocument(rawXml)!!
        val values = getSearchAutocomplete(dom)
        if (!values.contains(s)) {
            val elem = dom.createElement(SEARCH_VALUE_NAME)
            val text = dom.createTextNode(s)
            elem.appendChild(text)
            dom.documentElement.insertBefore(elem, dom.documentElement.firstChild)
            MyFileReader.saveSearchAutocomplete(getStringFromDocument(dom))
            return true
        } else {
            // перенесу значение на верх списка
            val existentValues = dom.getElementsByTagName(SEARCH_VALUE_NAME)
            if (existentValues != null && existentValues.length > 0) {
                var count = values.size
                while (count > 1) {
                    --count
                    val node = existentValues.item(count)
                    if (node.textContent == s) {
                        dom.documentElement.insertBefore(node, dom.documentElement.firstChild)
                        MyFileReader.saveSearchAutocomplete(getStringFromDocument(dom))
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun getDocument(rawText: String?): Document? {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder: DocumentBuilder
        try {
            dBuilder = dbFactory.newDocumentBuilder()
            val `is` = InputSource(StringReader(rawText))
            return dBuilder.parse(`is`)
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: SAXException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getStringFromDocument(doc: Document?): String {
        val domSource = DOMSource(doc)
        val writer = StringWriter()
        val result = StreamResult(writer)
        val tf = TransformerFactory.newInstance()
        val transformer: Transformer
        try {
            transformer = tf.newTransformer()
            transformer.transform(domSource, result)
        } catch (e: TransformerConfigurationException) {
            e.printStackTrace()
        } catch (e: TransformerException) {
            e.printStackTrace()
        }
        return writer.toString()
    }

    @kotlin.jvm.JvmStatic
    fun handleBackup(zin: ZipInputStream) {
        try {
            val db = App.instance.mDatabase
            val s = StringBuilder()
            val buffer = ByteArray(1024)
            var read: Int
            while (zin.read(buffer, 0, 1024).also { read = it } >= 0) {
                s.append(String(buffer, 0, read))
            }
            val xml = s.toString()
            val document = getDocument(xml)
            val factory = XPathFactory.newInstance()
            val xPath = factory.newXPath()
            var entries =
                xPath.evaluate("/readed_books/book", document, XPathConstants.NODESET) as NodeList
            if (entries.length > 0) {
                var counter = 0
                val entriesLen = entries.length
                var node: Node
                while (counter < entriesLen) {
                    node = entries.item(counter)
                    // получу идентификатор книги
                    val id = node.attributes.getNamedItem("id").textContent
                    val rb = ReadedBooks()
                    rb.bookId = id
                    if (db.readBooksDao().getBookById(id) == null) {
                        db.readBooksDao().insert(rb)
                    }
                    Log.d("surprise", "handleBackup: founded readed book")
                    ++counter
                }
            }
            entries = xPath.evaluate(
                "/downloaded_books/book",
                document,
                XPathConstants.NODESET
            ) as NodeList
            if (entries.length > 0) {
                var counter = 0
                val entriesLen = entries.length
                var node: Node
                while (counter < entriesLen) {
                    node = entries.item(counter)
                    // получу идентификатор книги
                    val id = node.attributes.getNamedItem("id").textContent
                    val rb = DownloadedBooks()
                    rb.bookId = id
                    if (db.downloadedBooksDao().getBookById(id) == null) {
                        db.downloadedBooksDao().insert(rb)
                    }
                    Log.d("surprise", "handleBackup: founded downloaded book")
                    ++counter
                }
            }
            entries =
                xPath.evaluate("/bookmarks/bookmark", document, XPathConstants.NODESET) as NodeList
            if (entries.length > 0) {
                var counter = 0
                val entriesLen = entries.length
                var node: Node
                while (counter < entriesLen) {
                    node = entries.item(counter)
                    // получу идентификатор книги
                    val name = node.attributes.getNamedItem("name").textContent
                    val link = node.attributes.getNamedItem("link").textContent
                    val bookmark = Bookmark()
                    bookmark.name = name
                    bookmark.link = link
                    val duplicate = db.bookmarksDao().getDuplicate(bookmark.name, bookmark.link)
                    if (duplicate == null || duplicate.isEmpty()) {
                        db.bookmarksDao().insert(bookmark)
                    }
                    Log.d("surprise", "handleBackup: founded bookmark")
                    ++counter
                }
            }
            entries = xPath.evaluate("/schedule/item", document, XPathConstants.NODESET) as NodeList
            if (entries.length > 0) {
                var counter = 0
                val entriesLen = entries.length
                var node: Node
                while (counter < entriesLen) {
                    node = entries.item(counter)
                    val scheduleElement = BooksDownloadSchedule()
                    val bookId = node.attributes.getNamedItem("bookId").textContent
                    val link = node.attributes.getNamedItem("link").textContent
                    val name = node.attributes.getNamedItem("name").textContent
                    val size = node.attributes.getNamedItem("size").textContent
                    val author = node.attributes.getNamedItem("author").textContent
                    val format = node.attributes.getNamedItem("format").textContent
                    val authorDirName = node.attributes.getNamedItem("authorDirName").textContent
                    val sequenceDirName =
                        node.attributes.getNamedItem("sequenceDirName").textContent
                    val reservedSequenceName =
                        node.attributes.getNamedItem("reservedSequenceName").textContent
                    scheduleElement.bookId = bookId
                    scheduleElement.link = link
                    scheduleElement.name = name
                    scheduleElement.size = size
                    scheduleElement.author = author
                    scheduleElement.format = format
                    scheduleElement.authorDirName = authorDirName
                    scheduleElement.sequenceDirName = sequenceDirName
                    scheduleElement.reservedSequenceName = reservedSequenceName
                    db.booksDownloadScheduleDao().insert(scheduleElement)
                    Log.d("surprise", "handleBackup: founded schedule element")
                    ++counter
                }
            }
        } catch (e: IOException) {
            Log.d("surprise", "handleBackup: error")
            e.printStackTrace()
        } catch (e: XPathExpressionException) {
            Log.d("surprise", "handleBackup: error1")
            e.printStackTrace()
        }
    }
}