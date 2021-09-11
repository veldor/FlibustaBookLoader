package net.veldor.flibustaloader.parsers

import android.util.Log
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.utils.Grammar
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

class TestParser(private val text: String) {
    var nextPageLink: String? = null

    fun parse(): ArrayList<FoundedEntity> {

        var downlodLink: DownloadLink
        var entity: FoundedEntity
        val authorStringBuilder = StringBuilder()
        val genreStringBuilder = StringBuilder()
        var idFound = false
        var nameFound = false
        var authorContainerFound = false
        var authorFound = false
        var contentFound = false
        var textValue: String?
        var attributeIndex: Int
        var attributeValue: String


        val db = App.instance.mDatabase

        Log.d("surprise", "parse: start parse server response")
        val parsed: ArrayList<FoundedEntity> = arrayListOf()
        try {
            var foundedEntity: FoundedEntity? = null

            val factory: SAXParserFactory = SAXParserFactory.newInstance()
            val saxParser: SAXParser = factory.newSAXParser()

            val handler: DefaultHandler = object : DefaultHandler() {

                // Метод вызывается когда SAXParser "натыкается" на начало тэга
                @Throws(SAXException::class)
                override fun startElement(
                    uri: String?,
                    localName: String?,
                    qName: String,
                    attributes: Attributes?
                ) {
                    if (qName.equals("entry", ignoreCase = true)) {
                        foundedEntity = FoundedEntity()
                    }
                    else if(qName == "id" && foundedEntity != null){
                        idFound = true
                    }
                    else if(qName == "title" && foundedEntity != null){
                        nameFound = true
                    }
                    else if(qName == "author" && foundedEntity != null){
                        authorContainerFound = true
                    }
                    else if(qName == "category" && foundedEntity != null){
                        // add a category
                        attributeIndex = attributes!!.getIndex("label")
                        entity = FoundedEntity()
                        entity.type = TYPE_GENRE
                        entity.name =  attributes.getValue(attributeIndex)
                        foundedEntity!!.genres.add(entity)
                        genreStringBuilder.append(entity.name)
                        genreStringBuilder.append("\n")
                    }
                    else if(qName == "name" && foundedEntity != null && authorContainerFound){
                        authorFound = true
                        authorContainerFound = false
                    }
                    else if(qName == "content" && foundedEntity != null){
                        contentFound = true
                    }
                    else if(qName == "link"){
                        if(foundedEntity == null) {
                            // check it is a link on next results page
                            attributeIndex = attributes!!.getIndex("rel")
                            if (attributeIndex >= 0) {
                                attributeValue = attributes.getValue(attributeIndex)
                                if (attributeValue == "next") {
                                    // found link on next page
                                    attributeIndex = attributes.getIndex("href")
                                    if (attributeIndex >= 0) {
                                        nextPageLink = attributes.getValue(attributeIndex)
                                    }
                                }
                            }
                        }
                        else{
                            // check if it is a download link
                            attributeIndex = attributes!!.getIndex("rel")
                            if (attributeIndex >= 0) {
                                attributeValue = attributes.getValue(attributeIndex)
                                if(attributeValue == "http://opds-spec.org/acquisition/open-access"){
                                    // link found, append it
                                    attributeIndex = attributes.getIndex("href")
                                    downlodLink = DownloadLink()
                                    downlodLink.url = attributes.getValue(attributeIndex)
                                    attributeIndex = attributes.getIndex("type")
                                    downlodLink.mime = attributes.getValue(attributeIndex)
                                    foundedEntity!!.downloadLinks.add(downlodLink)
                                }
                                else{
                                    attributeIndex = attributes.getIndex("rel")
                                    attributeValue = attributes.getValue(attributeIndex)
                                    if(attributeValue.startsWith("/opds/sequencebooks/")){
                                        // found sequence
                                        entity = FoundedEntity()
                                        entity.type = TYPE_SEQUENCE
                                        entity.link = attributeValue
                                        attributeIndex = attributes.getIndex("title")
                                        entity.name = attributes.getValue(attributeIndex)
                                        foundedEntity!!.sequences.add(entity)

                                    }
                                }
                            }
                            else{
                                // if it is a sequence selector- save link
                                if(foundedEntity!!.type == TYPE_SEQUENCE){
                                    attributeIndex = attributes.getIndex("rel")
                                    foundedEntity!!.link = attributes.getValue(attributeIndex)
                                }
                            }
                        }
                    }
                }

                override fun endElement(uri: String?, localName: String?, qName: String?) {
                    if (qName.equals("entry", ignoreCase = true)) {
                        foundedEntity!!.author = authorStringBuilder.removeSuffix("\n").toString()
                        foundedEntity!!.genreComplex = genreStringBuilder.removeSuffix("\n").toString()
                        authorStringBuilder.clear()
                        foundedEntity?.downloadLinks?.forEach {
                            it.author = foundedEntity!!.author
                            it.id = foundedEntity!!.id
                            it.name = foundedEntity!!.name
                            it.size = foundedEntity!!.size
                        }

                        foundedEntity!!.read = db.readBooksDao().getBookById(foundedEntity!!.id) != null
                        foundedEntity!!.downloaded = db.downloadedBooksDao().getBookById(foundedEntity!!.id) != null
                        parsed.add(foundedEntity!!)
                    }
                }

            // Метод вызывается когда SAXParser считывает текст между тэгами
            @Throws(SAXException::class)
            override fun characters(ch: CharArray?, start: Int, length: Int) {
                // Если перед этим мы отметили, что имя тэга NAME - значит нам надо текст использовать.
                if (idFound) {
                    textValue = String(ch!!, start, length)
                    when {
                        textValue!!.contains(TYPE_BOOK) -> {
                            foundedEntity!!.type = TYPE_BOOK
                        }
                        textValue!!.contains(TYPE_AUTHOR) -> {
                            foundedEntity!!.type = TYPE_AUTHOR
                        }
                        textValue!!.contains(TYPE_GENRE) -> {
                            foundedEntity!!.type = TYPE_GENRE
                        }
                        textValue!!.contains(TYPE_SEQUENCE) -> {
                            foundedEntity!!.type = TYPE_SEQUENCE
                        }
                    }
                    foundedEntity?.id = textValue!!
                    idFound = false
                }
                if(nameFound){
                    foundedEntity!!.name = String(ch!!, start, length)
                    nameFound = false
                }
                if(contentFound){
                    parseContent(foundedEntity, String(ch!!, start, length))
                    contentFound = false
                }
                if(authorFound){
                    authorStringBuilder.append(String(ch!!, start, length))
                    authorStringBuilder.append("\n")
                    authorFound = false
                }
            }
                // Стартуем разбор методом parse, которому передаем наследника от DefaultHandler, который будет вызываться в нужные моменты

            }
            saxParser.parse(text.byteInputStream(), handler)
        }
        catch (e: Exception){
            Log.d("surprise", "parse: parse error")
            e.printStackTrace()
        }
        return parsed
    }

    private fun parseContent(foundedEntity: FoundedEntity?, content: String) {
        foundedEntity?.downloadsCount = getInfoFromContent(content, "Скачиваний:")
        foundedEntity?.size = getInfoFromContent(content, "Размер:")
        foundedEntity?.format = getInfoFromContent(content, "Формат:")
        foundedEntity?.translate =
            Grammar.textFromHtml(getInfoFromContent(content, "Перевод:"))
        foundedEntity?.sequencesComplex = getInfoFromContent(content, "Серия:")
    }

    private fun getInfoFromContent(item: String?, s: String): String {
        val start = item!!.indexOf(s)
        val end = item.indexOf("<br/>", start)
        return if (start > 0 && end > 0) item.substring(start, end) else ""
    }

    companion object{
        public const val  TYPE_BOOK = "book"
        public const val  TYPE_AUTHOR = "author"
        public const val  TYPE_GENRE = "genre"
        public const val  TYPE_SEQUENCE = "sequence"
    }

}