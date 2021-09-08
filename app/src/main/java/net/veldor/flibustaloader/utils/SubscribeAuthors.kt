package net.veldor.flibustaloader.utils

import net.veldor.flibustaloader.App
import androidx.lifecycle.MutableLiveData
import net.veldor.flibustaloader.selections.SubscriptionItem
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

class SubscribeAuthors {
    private var mDom: Document? = null
    private var mSubscribeValues: ArrayList<SubscriptionItem> = arrayListOf()
    private val mExistentValues = ArrayList<String>()
    @kotlin.jvm.JvmField
    val mListRefreshed = MutableLiveData<Boolean>()
    private fun refreshSubscribes() {
        // получу данны файла подписок
        val rawData = MyFileReader.getAuthorsSubscribe()
        mDom = getDocument(rawData)
        mSubscribeValues = ArrayList()
        if (mDom != null) {
            val values = mDom!!.getElementsByTagName(SUBSCRIBE_NAME)
            var counter = 0
            var subscriptionItem: SubscriptionItem
            while (values.item(counter) != null) {
                subscriptionItem = SubscriptionItem()
                subscriptionItem.name = values.item(counter).firstChild.nodeValue
                mExistentValues.add(subscriptionItem.name!!)
                subscriptionItem.type = "author"
                mSubscribeValues.add(subscriptionItem)
                ++counter
            }
        }
    }

    fun getSubscribes(): ArrayList<SubscriptionItem> {
        refreshSubscribes()
        return mSubscribeValues
    }

    fun addValue(value: String) {
        if (!mExistentValues.contains(value)) {
            val elem = mDom!!.createElement(SUBSCRIBE_NAME)
            val text = mDom!!.createTextNode(value)
            elem.appendChild(text)
            mDom!!.documentElement.insertBefore(elem, mDom!!.documentElement.firstChild)
            MyFileReader.saveAuthorsSubscription(getStringFromDocument(mDom))
            App.instance.authorsSubscribe.mListRefreshed.postValue(true)
        }
    }

    fun deleteValue(name: String) {
        val authors = mDom!!.getElementsByTagName(SUBSCRIBE_NAME)
        var book: Node
        val length = authors.length
        var counter = 0
        if (length > 0) {
            while (counter < length) {
                book = authors.item(counter)
                if (name == book.textContent) {
                    book.parentNode.removeChild(book)
                    break
                }
                counter++
            }
            MyFileReader.saveAuthorsSubscription(getStringFromDocument(mDom))
            App.instance.authorsSubscribe.mListRefreshed.postValue(true)
        }
    }

    companion object {
        private const val SUBSCRIBE_NAME = "author"
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
        refreshSubscribes()
    }
}