package net.veldor.flibustaloader.parsers

import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.selections.Genre
import android.util.Log
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.ArrayList
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException

internal object GenresParser {
    fun parse(entries: NodeList, xPath: XPath): ArrayList<*> {
        val result = ArrayList<Genre>()
        var entry: Node?
        var genre: Genre
        var counter = 0
        val entriesLength = entries.length
        var handledEntryCounter = 0
        while (entries.item(counter).also { entry = it } != null) {
            ++handledEntryCounter
            genre = Genre()
            try {
                genre.label =
                    (xPath.evaluate("./title", entry, XPathConstants.NODE) as Node).textContent
                genre.term = (xPath.evaluate(
                    "./link",
                    entry,
                    XPathConstants.NODE
                ) as Node).attributes.getNamedItem("href").textContent
            } catch (e: XPathExpressionException) {
                e.printStackTrace()
            }
            result.add(genre)
            counter++
        }
        // отправлю обработанные результаты
        Log.d("surprise", "GenresParser parse: founded genres: " + result.size)
        return result
    }
}