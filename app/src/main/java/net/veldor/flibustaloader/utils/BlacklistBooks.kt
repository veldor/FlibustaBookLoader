package net.veldor.flibustaloader.utils

import net.veldor.flibustaloader.selections.BlacklistItem
import androidx.lifecycle.MutableLiveData
import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.ArrayList
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class BlacklistBooks {
    private var mDom: Document? = null
    private var mBlacklistValues: ArrayList<BlacklistItem>? = null
    private val mExistentValues = ArrayList<String>()
    private fun refreshBlacklist() {
        // получу данны файла подписок
        val rawData = MyFileReader.getBooksBlacklist()
        mDom = getDocument(rawData)
        mBlacklistValues = ArrayList()
        if (mDom != null) {
            val values = mDom!!.getElementsByTagName(BLACKLIST_NAME)
            var counter = 0
            var blacklistItem: BlacklistItem
            while (values.item(counter) != null) {
                blacklistItem = BlacklistItem()
                blacklistItem.name = values.item(counter).firstChild.nodeValue
                mExistentValues.add(blacklistItem.name!!)
                blacklistItem.type = "book"
                mBlacklistValues!!.add(blacklistItem)
                ++counter
            }
        }
    }

    fun getBlacklist(): ArrayList<BlacklistItem>? {
        refreshBlacklist()
        return mBlacklistValues
    }

    fun addValue(value: String) {
        Log.d("surprise", "BlacklistBooks.java 104 addValue: adding value")
        if (!mExistentValues.contains(value)) {
            val elem = mDom!!.createElement(BLACKLIST_NAME)
            val text = mDom!!.createTextNode(value)
            elem.appendChild(text)
            mDom!!.documentElement.insertBefore(elem, mDom!!.documentElement.firstChild)
            MyFileReader.saveBooksBlacklist(getStringFromDocument(mDom))
            mListRefreshed.postValue(true)
        }
    }

    fun deleteValue(name: String) {
        val books = mDom!!.getElementsByTagName(BLACKLIST_NAME)
        var book: Node
        val length = books.length
        var counter = 0
        if (length > 0) {
            while (counter < length) {
                book = books.item(counter)
                if (name == book.textContent) {
                    book.parentNode.removeChild(book)
                    break
                }
                counter++
            }
            MyFileReader.saveBooksBlacklist(getStringFromDocument(mDom))
            mListRefreshed.postValue(true)
        }
    }

    companion object {
        private const val BLACKLIST_NAME = "book"
        @kotlin.jvm.JvmField
        val mListRefreshed = MutableLiveData<Boolean>()
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
    }

    init {
        refreshBlacklist()
    }
}