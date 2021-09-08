package net.veldor.flibustaloader.parsers

import kotlin.Throws
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.selections.Author
import net.veldor.flibustaloader.ui.OPDSActivity
import android.util.Log
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.ArrayList
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException

internal object AuthorsParser {
    @Throws(XPathExpressionException::class)
    fun parse(entries: NodeList, xPath: XPath): ArrayList<*> {
        val result = ArrayList<Author>()
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
            author.id = (xPath.evaluate("./id", entry, XPathConstants.NODE) as Node).textContent
            Log.d("surprise", "AuthorsParser.java 32 parse: " + author.id)
            // если поиск осуществляется по новинкам- запишу ссылку на новинки, иначе- на автора
            when {
                author.id!!.startsWith("tag:authors") -> {
                    author.uri = (xPath.evaluate(
                        "./link",
                        entry,
                        XPathConstants.NODE
                    ) as Node).attributes.getNamedItem("href").textContent
                }
                App.sSearchType == OPDSActivity.SEARCH_NEW_AUTHORS -> {
                    Log.d("surprise", "AuthorsParser.java 35 parse: parse new author")
                    author.link = (xPath.evaluate(
                        "./link",
                        entry,
                        XPathConstants.NODE
                    ) as Node).attributes.getNamedItem("href").textContent
                }
                else -> {
                    author.uri = explodeByDelimiter(
                        (xPath.evaluate(
                            "./id",
                            entry,
                            XPathConstants.NODE
                        ) as Node).textContent
                    )
                }
            }
            author.content =
                (xPath.evaluate("./content", entry, XPathConstants.NODE) as Node).textContent
            result.add(author)
            counter++
        }
        return result
    }

    private fun explodeByDelimiter(s: String): String? {
        val result = s.split(":").toTypedArray()
        return if (result.size < 3) {
            null
        } else result[3 - 1]
    }
}