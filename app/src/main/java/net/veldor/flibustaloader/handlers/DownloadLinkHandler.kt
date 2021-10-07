package net.veldor.flibustaloader.handlers

import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.database.dao.BooksDownloadScheduleDao
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.ui.BaseActivity
import net.veldor.flibustaloader.utils.Grammar
import net.veldor.flibustaloader.utils.MimeTypes

class DownloadLinkHandler {
    fun addDownloadSchedule(link: BooksDownloadSchedule) {
        val dao =App.instance.mDatabase.booksDownloadScheduleDao()
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
        newScheduleElement.size = link.size!!
        newScheduleElement.authorDirName = link.authorDirName!!
        newScheduleElement.sequenceDirName = link.sequenceDirName!!
        newScheduleElement.reservedSequenceName = link.reservedSequenceName!!
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
        val bookName =
            link.name!!.replace(" ".toRegex(), "_").replace("[^\\d\\w-_]".toRegex(), "")
        val bookMime = MimeTypes.getDownloadMime(link.mime!!)
        // если сумма символов меньше 255- создаю полное имя
        if (authorLastName.length + bookName.length + bookMime!!.length + 2 < 255 / 2 - 6) {
            newScheduleElement.name =
                authorLastName + "_" + bookName + "_" + Grammar.random + "." + bookMime
        } else {
            // сохраняю книгу по имени автора и тому, что влезет от имени книги
            newScheduleElement.name = authorLastName + "_" + bookName.substring(
                0,
                127 - (authorLastName.length + bookMime.length + 2 + 6)
            ) + "_" + Grammar.random + "." + bookMime
        }
        //===========================================================================
        dao.insert(newScheduleElement)
    }

}