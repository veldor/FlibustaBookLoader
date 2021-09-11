package net.veldor.flibustaloader.parsers

import kotlin.Throws
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.selections.FoundedSequence
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.ArrayList
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException

internal object SequencesParser {
    @Throws(XPathExpressionException::class)
    fun parse(entries: NodeList, xPath: XPath): ArrayList<*> {
        val result = ArrayList<FoundedSequence>()
        var entry: Node?
        var sequence: FoundedSequence
        var counter = 0
        val entriesLength = entries.length
        var handledEntryCounter = 0
        while (entries.item(counter).also { entry = it } != null) {
            ++handledEntryCounter
            sequence = FoundedSequence()
            sequence.title =
                (xPath.evaluate("./title", entry, XPathConstants.NODE) as Node).textContent
            sequence.content =
                (xPath.evaluate("./content", entry, XPathConstants.NODE) as Node).textContent
            sequence.link = (xPath.evaluate(
                "./link",
                entry,
                XPathConstants.NODE
            ) as Node).attributes.getNamedItem("href").textContent
            result.add(sequence)
            counter++
        }
        return result
    }
}