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

class SubscribeBooks private constructor() : SubscribeType(){
    override val subscribeName = "book"

    companion object {
        @JvmStatic
        var instance: SubscribeBooks = SubscribeBooks()
            private set
    }


    init {
        subscribeFileName = MyFileReader.BOOKS_SUBSCRIBE_FILE
    }
}