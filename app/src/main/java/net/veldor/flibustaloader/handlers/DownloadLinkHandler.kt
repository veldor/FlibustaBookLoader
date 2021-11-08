package net.veldor.flibustaloader.handlers

import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.database.dao.BooksDownloadScheduleDao
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.ui.BaseActivity
import net.veldor.flibustaloader.utils.Grammar
import net.veldor.flibustaloader.utils.MimeTypes
import net.veldor.flibustaloader.utils.PreferencesHandler

class DownloadLinkHandler {
    fun addDownloadSchedule(link: BooksDownloadSchedule) {
        val dao = App.instance.mDatabase.booksDownloadScheduleDao()
        dao.insert(link)
    }


    fun addLink(downloadLink: DownloadLink) {
        addDownloadLink(App.instance.mDatabase.booksDownloadScheduleDao(), downloadLink)
        // уведомлю, что размер списка закачек изменился
        BaseActivity.sLiveDownloadScheduleCountChanged.postValue(true)
    }

    private fun addDownloadLink(dao: BooksDownloadScheduleDao, link: DownloadLink) {
        // добавлю книги в очередь скачивания
        val newScheduleElement = BooksDownloadSchedule()
        newScheduleElement.bookId = link.id!!
        newScheduleElement.author = link.author!!
        newScheduleElement.link = link.url!!
        newScheduleElement.format = link.mime!!
        newScheduleElement.size = link.size ?: "0"
        newScheduleElement.authorDirName = link.authorDirName!!
        newScheduleElement.sequenceDirName = link.sequenceDirName!!
        newScheduleElement.reservedSequenceName = link.reservedSequenceName
        // определю имя ссылки для скачивания =======================================
        val authorLastName: String
        if (link.author != null && link.author!!.isNotEmpty()) {
            newScheduleElement.author = link.author!!
            val delimiter = link.author!!.indexOf(" ")
            authorLastName = if (delimiter >= 0) {
                link.author!!.substring(0, delimiter)
            } else {
                link.author!!
            }
        } else {
            authorLastName = "Автор неизвестен"
            newScheduleElement.author = "Автор неизвестен"
        }

        var bookName =
            link.name!!.replace(" ".toRegex(), "_").replace("[^\\d\\w-_]".toRegex(), "")
        val bookMime = MimeTypes.getDownloadMime(link.mime!!)

        if (PreferencesHandler.instance.isAuthorInBookName) {
            if (bookName.length / 2 + authorLastName.length / 2 < 110) {
                bookName = authorLastName + "_" + bookName
            }
        }
        if (PreferencesHandler.instance.isSequenceInBookName) {
            if (bookName.length / 2 + link.reservedSequenceName.length / 2 < 110) {
                bookName = bookName + "_" + link.reservedSequenceName
            }
        }
        if (bookName.length / 2 > 220) {
            bookName = bookName.substring(0, 110) + "..."
        }
        newScheduleElement.name = bookName + "_" + Grammar.random + "." + bookMime
        //===========================================================================
        dao.insert(newScheduleElement)
    }

}