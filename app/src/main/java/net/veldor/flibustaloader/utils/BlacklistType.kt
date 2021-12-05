package net.veldor.flibustaloader.utils

import android.util.Log
import androidx.lifecycle.MutableLiveData
import net.veldor.flibustaloader.selections.BlacklistItem
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class BlacklistType {
    open val blacklistName = "empty"
    open var blacklistFileName = "empty"

    @kotlin.jvm.JvmField
    val liveBlacklistAdd = MutableLiveData<BlacklistItem>()

    @kotlin.jvm.JvmField
    val liveBlacklistRemove = MutableLiveData<BlacklistItem>()

    private var mDom: Document? = null
    private var mBlacklistValues: ArrayList<BlacklistItem> = arrayListOf()
    private val mExistentValues = ArrayList<String>()

    fun refreshBlacklist() {
        // получу данны файла подписок
        val rawData = MyFileReader.getBlacklist(blacklistFileName)
        mDom = getDocument(rawData)
        mBlacklistValues = ArrayList()
        if (mDom != null) {
            val values = mDom!!.getElementsByTagName(blacklistName)
            var counter = 0
            var blacklistItem: BlacklistItem
            while (values.item(counter) != null) {
                blacklistItem = BlacklistItem(
                        values.item(counter).firstChild.nodeValue,
                        blacklistName
                )
                mExistentValues.add(blacklistItem.name)
                mBlacklistValues.add(blacklistItem)
                ++counter
            }
        }
    }

    fun getBlacklist(): ArrayList<BlacklistItem> {
        refreshBlacklist()
        return mBlacklistValues
    }

    fun addValue(value: String) {
        Log.d("surprise", "addValue: add ${value.lowercase()}")
        if (!mExistentValues.contains(value.lowercase())) {
            val elem = mDom!!.createElement(blacklistName)
            val text = mDom!!.createTextNode(value.lowercase())
            elem.appendChild(text)
            mDom!!.documentElement.insertBefore(elem, mDom!!.documentElement.firstChild)
            MyFileReader.saveBlacklist(blacklistFileName, getStringFromDocument(mDom))
            liveBlacklistAdd.postValue(BlacklistItem(value.lowercase(), blacklistName))
        } else {
            Log.d("surprise", "addValue: already in blacklist")
        }
    }

    fun deleteValue(name: String) {
        val books = mDom!!.getElementsByTagName(blacklistName)
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
            MyFileReader.saveBlacklist(blacklistFileName, getStringFromDocument(mDom))
            liveBlacklistRemove.postValue(BlacklistItem(name, blacklistName))
        }
    }

    fun getBlacklist(which: Int): ArrayList<BlacklistItem> {
        val unsorted = getBlacklist()
        when (which) {
            1 -> {
                unsorted.reverse()
            }
            2 -> {
                unsorted.sortBy{it.name}
            }
            3 -> {
                unsorted.sortBy{it.name}
                unsorted.reverse()
            }
        }
        return unsorted
    }

    companion object {

        fun getDocument(rawText: String?): Document? {
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

        fun getStringFromDocument(doc: Document?): String {
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
}