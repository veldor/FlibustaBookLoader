package net.veldor.flibustaloader.parsers

import android.util.Log
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.handlers.Filter
import net.veldor.flibustaloader.handlers.PicHandler
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.utils.Grammar
import net.veldor.flibustaloader.utils.PreferencesHandler
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

class TestParser(private val text: String) {
    val filteredList: ArrayList<FoundedEntity> = arrayListOf()
    var filtered: Int = 0
    var nextPageLink: String? = null

    fun parse(): ArrayList<FoundedEntity> {
        var contentType: String? = null
        var downloadLink: DownloadLink
        var entity: FoundedEntity
        var author: FoundedEntity? = null
        val authorStringBuilder = StringBuilder()
        val genreStringBuilder = StringBuilder()
        val simpleStringBuilder = StringBuilder()
        var idFound = false
        var nameFound = false
        var authorContainerFound = false
        var authorFound = false
        var authorUriFound = false
        var contentFound = false
        var textValue: String?
        var attributeIndex: Int
        var attributeValue: String


        val db = App.instance.mDatabase
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
                    } else if (qName == "id" && foundedEntity != null) {
                        idFound = true
                    } else if (qName == "title" && foundedEntity != null) {
                        nameFound = true
                    } else if (qName == "author" && foundedEntity != null) {
                        authorContainerFound = true
                    } else if (qName == "category" && foundedEntity != null) {
                        // add a category
                        attributeIndex = attributes!!.getIndex("label")
                        entity = FoundedEntity()
                        entity.type = TYPE_GENRE
                        entity.name = attributes.getValue(attributeIndex)
                        foundedEntity!!.genres.add(entity)
                        genreStringBuilder.append(entity.name)
                        genreStringBuilder.append("\n")
                    } else if (qName == "name" && foundedEntity != null && authorContainerFound) {
                        authorFound = true
                    } else if (qName == "uri" && foundedEntity != null && authorContainerFound) {
                        authorUriFound = true
                        authorContainerFound = false
                    } else if (qName == "content" && foundedEntity != null) {
                        contentFound = true
                    } else if (qName == "link") {
                        if (foundedEntity == null) {
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
                        } else {
                            // check if it is a download link
                            attributeIndex = attributes!!.getIndex("rel")
                            if (attributeIndex >= 0) {
                                attributeValue = attributes.getValue(attributeIndex)
                                if (attributeValue == "http://opds-spec.org/acquisition/open-access") {
                                    // link found, append it
                                    attributeIndex = attributes.getIndex("href")
                                    downloadLink = DownloadLink()
                                    downloadLink.url = attributes.getValue(attributeIndex)
                                    attributeIndex = attributes.getIndex("type")
                                    downloadLink.mime = attributes.getValue(attributeIndex)
                                    foundedEntity!!.downloadLinks.add(downloadLink)
                                }
                                else if (attributeValue == "http://opds-spec.org/acquisition/disabled") {
                                    // link found, append it
                                    attributeIndex = attributes.getIndex("href")
                                    downloadLink = DownloadLink()
                                    downloadLink.url = attributes.getValue(attributeIndex)
                                    attributeIndex = attributes.getIndex("type")
                                    downloadLink.mime = attributes.getValue(attributeIndex)
                                    foundedEntity!!.downloadLinks.add(downloadLink)
                                } else if (attributeValue == "http://opds-spec.org/image") {
                                    // найдена обложка
                                    foundedEntity!!.coverUrl =
                                        attributes.getValue(attributes.getIndex("href"))
                                } else {
                                    attributeIndex = attributes.getIndex("href")
                                    attributeValue = attributes.getValue(attributeIndex)
                                    if (attributeValue.startsWith("/opds/sequencebooks/")) {
                                        // found sequence
                                        entity = FoundedEntity()
                                        entity.type = TYPE_SEQUENCE
                                        entity.link = attributeValue
                                        attributeIndex = attributes.getIndex("title")
                                        entity.name = attributes.getValue(attributeIndex)
                                        foundedEntity!!.sequences.add(entity)

                                    }
                                }
                            } else {
                                // if it is a sequence selector- save link
                                if (foundedEntity!!.type == TYPE_SEQUENCE || foundedEntity!!.type == TYPE_GENRE || foundedEntity!!.type == TYPE_AUTHORS) {
                                    attributeIndex = attributes.getIndex("href")
                                    foundedEntity!!.link = attributes.getValue(attributeIndex)
                                }
                            }
                        }
                    }
                }

                override fun endElement(uri: String?, localName: String?, qName: String?) {
                    if (qName.equals("entry", ignoreCase = true)) {
                        foundedEntity!!.author = authorStringBuilder.removeSuffix("\n").toString()
                        foundedEntity!!.genreComplex =
                            genreStringBuilder.removeSuffix("\n").toString()
                        genreStringBuilder.clear()
                        authorStringBuilder.clear()

                        foundedEntity!!.read =
                            db.readBooksDao().getBookById(foundedEntity!!.id) != null
                        foundedEntity!!.downloaded =
                            db.downloadedBooksDao().getBookById(foundedEntity!!.id) != null

                        var authorDirName: String = when (foundedEntity!!.authors.size) {
                            0 -> {
                                "Без автора"
                            }
                            1 -> {
                                // создам название папки
                                Grammar.createAuthorDirName(foundedEntity!!.authors[0])
                            }
                            2 -> {
                                Grammar.createAuthorDirName(foundedEntity!!.authors[0]) + " " + Grammar.createAuthorDirName(
                                    foundedEntity!!.authors[1]
                                )
                            }
                            else -> {
                                "Антологии"
                            }
                        }
                        authorDirName = Grammar.clearDirName(authorDirName).trim()

                        foundedEntity?.downloadLinks?.forEach { link ->
                            link.author = foundedEntity!!.author
                            link.id = foundedEntity!!.id
                            link.name = foundedEntity!!.name
                            link.size = foundedEntity!!.size ?: "0"
                            link.authorDirName = authorDirName
                            // так, как книга может входить в несколько серий- совмещу назначения
                            if (foundedEntity!!.sequences.size > 0) {
                                simpleStringBuilder.clear()
                                var prefix = ""
                                foundedEntity!!.sequences.forEach {
                                    simpleStringBuilder.append(prefix)
                                    prefix = "$|$"
                                    simpleStringBuilder.append(
                                        Regex("[^\\d\\w ]").replace(
                                            it.name!!.replace(
                                                "Все книги серии",
                                                ""
                                            ), ""
                                        )
                                    )
                                }
                                link.sequenceDirName = simpleStringBuilder.toString().trim()
                                link.reservedSequenceName = foundedEntity!!.sequencesComplex.trim()
                            } else {
                                link.sequenceDirName = ""
                                link.reservedSequenceName = ""
                            }
                        }
                       val filterResult = Filter.check(foundedEntity!!)
                        if (filterResult.result) {
                            parsed.add(foundedEntity!!)
                            // загружу картинку
                            if (foundedEntity!!.coverUrl != null && foundedEntity!!.coverUrl!!.isNotEmpty() && PreferencesHandler.instance.isPreviews) {
                                // load pic in new Thread
                                PicHandler().loadPic(parsed.last())
                            }
                        } else {
                            foundedEntity!!.filterResult = filterResult
                            filteredList.add(foundedEntity!!)
                            filtered++
                        }
                    } else if (qName.equals("content")) {
                        contentFound = false
                    }
                }

                // Метод вызывается когда SAXParser считывает текст между тэгами
                @Throws(SAXException::class)
                override fun characters(ch: CharArray?, start: Int, length: Int) {
                    // Если перед этим мы отметили, что имя тэга NAME - значит нам надо текст использовать.
                    if (idFound) {
                        idFound = false
                        textValue = String(ch!!, start, length)
                        when {
                            textValue!!.contains(TYPE_BOOK) -> {
                                contentType = TYPE_BOOK
                            }
                            textValue!!.contains(TYPE_SEQUENCE) -> {
                                contentType = TYPE_SEQUENCE
                            }
                            textValue!!.contains(TYPE_AUTHORS) -> {
                                contentType = TYPE_AUTHORS
                            }
                            textValue!!.contains(TYPE_AUTHOR) -> {
                                contentType = TYPE_AUTHOR
                            }
                            textValue!!.contains(TYPE_GENRE) -> {
                                contentType = TYPE_GENRE
                            }
                        }
                        foundedEntity!!.type = contentType!!
                        foundedEntity!!.id = textValue!!
                    }
                    if (nameFound) {
                        foundedEntity!!.name = String(ch!!, start, length)
                        nameFound = false
                    }
                    if (contentFound) {
                        parseContent(foundedEntity, String(ch!!, start, length))
                    }
                    if (authorFound) {
                        author = FoundedEntity()
                        author!!.type = TYPE_AUTHOR
                        author!!.name = String(ch!!, start, length)
                        authorStringBuilder.append(author!!.name)
                        authorStringBuilder.append("\n")
                        authorFound = false
                    }
                    if (authorUriFound) {
                        author!!.link = String(ch!!, start, length)
                        author!!.id = author!!.link
                        foundedEntity!!.authors.add(author!!)
                        authorUriFound = false
                    }
                }
                // Стартуем разбор методом parse, которому передаем наследника от DefaultHandler, который будет вызываться в нужные моменты

            }
            saxParser.parse(text.byteInputStream(), handler)
        } catch (e: Exception) {
            Log.d("surprise", "parse: parse error")
            e.printStackTrace()
        }
        return parsed
    }

    private fun parseContent(foundedEntity: FoundedEntity?, content: String) {
        foundedEntity!!.content = foundedEntity.content + content
        when {
            content.startsWith("Скачиваний") -> {
                foundedEntity.downloadsCount = content
            }
            content.startsWith("Размер") -> {
                foundedEntity.size = content
            }
            content.startsWith("Формат") -> {
                foundedEntity.format = content
            }
            content.startsWith("Перевод") -> {
                foundedEntity.translate = content
            }
            content.startsWith("Серия") -> {
                foundedEntity.sequencesComplex = content
            }
            content.startsWith("Язык") -> {
                foundedEntity.language = content
            }
            content.contains("сери") -> {
                foundedEntity.description = content.substring(0, content.indexOf("сери") + 5)
            }
            content.contains("автора на") -> {
                foundedEntity.description = content
            }
        }
    }

    companion object {
        const val TYPE_BOOK = "book"
        const val TYPE_AUTHOR = "author"
        const val TYPE_AUTHORS = "authors"
        const val TYPE_GENRE = "genre"
        const val TYPE_SEQUENCE = "sequence"
    }
}