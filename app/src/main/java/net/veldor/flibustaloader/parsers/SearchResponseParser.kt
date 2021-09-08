package net.veldor.flibustaloader.parsers

import kotlin.Throws
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.ui.OPDSActivity
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.lang.Exception
import java.util.ArrayList
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

class SearchResponseParser(answer: String) {
    private val mEntries: NodeList?
    private val mXpath: XPath
    @Throws(XPathExpressionException::class)
    fun parseResponse(reservedSequenceName: String?): ArrayList<*>? {
        // получу сущности
        if (mEntries != null && mEntries.length > 0) {
            // в зависимости от типа сущностей запущу парсер
            val contentType = identificationSearchType(mEntries.item(0), mXpath)
            if (contentType > 0) {
                when (contentType) {
                    OPDSActivity.SEARCH_GENRE -> return GenresParser.parse(mEntries, mXpath)
                    OPDSActivity.SEARCH_SEQUENCE -> return SequencesParser.parse(mEntries, mXpath)
                    OPDSActivity.SEARCH_AUTHORS, OPDSActivity.SEARCH_NEW_AUTHORS -> return AuthorsParser.parse(
                        mEntries,
                        mXpath
                    )
                    OPDSActivity.SEARCH_BOOKS -> return BooksParser.parse(
                        mEntries,
                        mXpath,
                        reservedSequenceName
                    )
                }
            }
        }
        return null
    }

    @Throws(XPathExpressionException::class)
    private fun identificationSearchType(item: Node, xPath: XPath): Int {
        // получу идентификатор
        val id = (xPath.evaluate("./id", item, XPathConstants.NODE) as Node).textContent
        when {
            id.startsWith(BOOK_TYPE) -> {
                return OPDSActivity.SEARCH_BOOKS
            }
            id.startsWith(GENRE_TYPE) -> {
                return OPDSActivity.SEARCH_GENRE
            }
            id.startsWith(AUTHOR_TYPE) -> {
                // проверю на возможность, что загружены серии
                return if (id.contains(AUTHOR_SEQUENCE_TYPE)) {
                    OPDSActivity.SEARCH_SEQUENCE
                } else {
                    OPDSActivity.SEARCH_AUTHORS
                }
            }
            id.startsWith(SEQUENCES_TYPE) -> {
                return OPDSActivity.SEARCH_SEQUENCE
            }
            id.startsWith(SEQUENCE_TYPE) -> {
                return OPDSActivity.SEARCH_SEQUENCE
            }
            id.startsWith(NEW_GENRES) -> {
                return OPDSActivity.SEARCH_GENRE
            }
            id.startsWith(NEW_SEQUENCES) -> {
                return OPDSActivity.SEARCH_SEQUENCE
            }
            id.startsWith(NEW_AUTHORS) -> {
                return OPDSActivity.SEARCH_NEW_AUTHORS
            }
            else -> return -1
        }
    }

    @get:Throws(XPathExpressionException::class)
    val type: Int
        get() = if (mEntries!!.length > 0) {
            identificationSearchType(mEntries.item(0), mXpath)
        } else -1

    companion object {
        private const val BOOK_TYPE = "tag:book"
        private const val GENRE_TYPE = "tag:root:genre"
        private const val AUTHOR_TYPE = "tag:author"
        private const val SEQUENCES_TYPE = "tag:sequences"
        private const val SEQUENCE_TYPE = "tag:sequence"
        private val AUTHOR_SEQUENCE_TYPE: CharSequence = ":sequence:"
        private const val NEW_GENRES = "tag:search:new:genres"
        private const val NEW_SEQUENCES = "tag:search:new:sequence"
        private const val NEW_AUTHORS = "tag:search:new:author"
        private fun getDocument(rawText: String): Document? {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder: DocumentBuilder
            try {
                dBuilder = dbFactory.newDocumentBuilder()
                val `is` = InputSource(StringReader(rawText))
                return dBuilder.parse(`is`)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    init {
        val document = getDocument(answer)
        mXpath = XPathFactory.newInstance().newXPath()
        App.instance.mSearchTitle.postValue(
            (mXpath.evaluate(
                "/feed/title",
                document,
                XPathConstants.NODE
            ) as Node).textContent
        )
        mEntries = mXpath.evaluate("/feed/entry", document, XPathConstants.NODESET) as NodeList
    }
}