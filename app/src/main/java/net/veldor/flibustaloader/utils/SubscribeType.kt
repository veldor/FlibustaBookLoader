package net.veldor.flibustaloader.utils

import androidx.lifecycle.MutableLiveData
import net.veldor.flibustaloader.selections.SubscriptionItem
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

abstract class SubscribeType {

    open val subscribeName = "empty"
    open var subscribeFileName = "empty"

    private fun refreshSubscribes() {
        // получу данны файла подписок
        val rawData = MyFileReader.getSubscribe(subscribeFileName)
        mDom = getDocument(rawData)
        mSubscribeValues = ArrayList()
        if (mDom != null) {
            val values = mDom!!.getElementsByTagName(subscribeName)
            var counter = 0
            var subscriptionItem: SubscriptionItem
            while (values.item(counter) != null) {
                subscriptionItem =
                    SubscriptionItem(values.item(counter).firstChild.nodeValue, subscribeName)
                mExistentValues.add(subscriptionItem.name)
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
        if (!mExistentValues.contains(value.lowercase())) {
            val elem = mDom!!.createElement(subscribeName)
            val text = mDom!!.createTextNode(value.lowercase())
            elem.appendChild(text)
            mDom!!.documentElement.insertBefore(elem, mDom!!.documentElement.firstChild)
            MyFileReader.saveSubscription(subscribeFileName, getStringFromDocument(mDom))
            liveSubscribeListAdd.postValue(SubscriptionItem(value.lowercase(), subscribeName))
        }
    }

    fun deleteValue(name: String) {
        val sequences = mDom!!.getElementsByTagName(subscribeName)
        var book: Node
        val length = sequences.length
        var counter = 0
        if (length > 0) {
            while (counter < length) {
                book = sequences.item(counter)
                if (name == book.textContent) {
                    book.parentNode.removeChild(book)
                    break
                }
                counter++
            }
            MyFileReader.saveSubscription(subscribeFileName, getStringFromDocument(mDom))
            liveSubscribeListRemove.postValue(SubscriptionItem(name, subscribeName))
        }
    }


    private var mDom: Document? = null
    private var mSubscribeValues: ArrayList<SubscriptionItem> = arrayListOf()
    private val mExistentValues = ArrayList<String>()

    @kotlin.jvm.JvmField
    val liveSubscribeListAdd = MutableLiveData<SubscriptionItem>()

    @kotlin.jvm.JvmField
    val liveSubscribeListRemove = MutableLiveData<SubscriptionItem>()

    companion object {
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