package net.veldor.flibustaloader.workers

import net.veldor.flibustaloader.utils.MimeTypes.getFullMime
import net.veldor.flibustaloader.utils.MimeTypes.getDownloadMime
import net.veldor.flibustaloader.utils.Grammar.random
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.ui.OPDSActivity
import net.veldor.flibustaloader.selections.FoundedBook
import android.content.Context
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.database.dao.BooksDownloadScheduleDao
import net.veldor.flibustaloader.ui.BaseActivity
import android.util.Log
import androidx.work.*
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.util.ArrayList

class AddBooksToDownloadQueueWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        // проверю, нужно ли подгружать только незагруженные
        val redownload = App.instance.mDownloadUnloaded

        // получу предпочтительный формат
        var preferredFormat = PreferencesHandler.instance.favoriteMime
        if (preferredFormat == null) {
            preferredFormat = App.instance.mSelectedFormat
        }
        // если выбраны книги для загрузки- получу их ID
        val selectedBooksList = App.instance.mDownloadSelectedBooks
        // получу весь список книг
        val mBooks = OPDSActivity.sBooksForDownload
        Log.d("surprise", "AddBooksToDownloadQueueWorker doWork 52: adding " + mBooks!!.size)

        // проверю, что книги есть и готовы к загрузке
        if (mBooks.size == 0) {
            return Result.success()
        }
        var book: FoundedBook
        val prepareForDownload = ArrayList<FoundedBook>()
        // если для загрузки выбран список книг
        if (selectedBooksList != null) {
            for (counter in mBooks.indices) {
                // если книга выбрана для скачивания- добавлю её в список для скачивания
                if (selectedBooksList[counter]) {
                    book = mBooks[counter]
                    book.preferredFormat = preferredFormat
                    prepareForDownload.add(book)
                }
            }
        } else {
            for (counter in mBooks.indices) {
                book = mBooks[counter]
                // тут проверю, если запрещено загружать ранее загруженные книги повторно- пропущу ранее загруженные
                if (redownload && book.downloaded) {
                    continue
                }
                book.preferredFormat = preferredFormat
                prepareForDownload.add(book)
            }
        }
        // теперь добавлю книги в базу данных для загрузки
        val database = App.instance.mDatabase
        val dao = database.booksDownloadScheduleDao()
        // проверю правило загружать строго выбранный формат
        val loadOnlySelectedFormat = PreferencesHandler.instance.saveOnlySelected
        for (book1 in prepareForDownload) {
            // проверю, есть ли предпочтительная ссылка на скачивание книги
            val links = book1.downloadLinks
            if (links.size > 0) {
                var link: DownloadLink? = null
                var fb2Link: DownloadLink? = null
                if (links.size == 1 && links[0].mime!!.contains(preferredFormat!!)) {
                    Log.d(
                        "surprise",
                        "AddBooksToDownloadQueueWorker doWork 98: add to direct link: " + book1.name
                    )
                    link = links[0]
                } else {
                    var counter = 0
                    while (counter < links.size) {
                        if (links[counter].mime!!.contains(preferredFormat!!)) {
                            link = links[counter]
                            Log.d(
                                "surprise",
                                "AddBooksToDownloadQueueWorker doWork 105: add link for counter " + book1.name
                            )
                            break
                        }
                        if (links[counter].mime == getFullMime("fb2")) {
                            fb2Link = links[counter]
                        }
                        counter++
                    }
                    if (counter == links.size - 1) {
                        Log.d(
                            "surprise",
                            "AddBooksToDownloadQueueWorker doWork 114: search all links but not found target"
                        )
                    }
                }
                // если не найдена предпочтительная ссылка на книгу и разрешено загружать книги вне выбранного формата
                if (link == null && !loadOnlySelectedFormat) {
                    // попробую скачать книгу в fb2, как в самом распространённом формате
                    link = fb2Link ?: // просто возьму первую ссылку
                            links[0]
                }
                // если ссылка всё-же не найдена- перехожу к следующей книге
                if (link == null) {
                    Log.d(
                        "surprise",
                        "AddBooksToDownloadQueueWorker doWork 129: not found link for " + book1.name
                    )
                    continue
                }
                addDownloadLink(dao, link)
                // уведомлю, что размер списка закачек изменился
                BaseActivity.sLiveDownloadScheduleCount.postValue(true)
            } else {
                Log.d(
                    "surprise",
                    "AddBooksToDownloadQueueWorker doWork 131: Have no link for " + book1.name
                )
            }
        }
        return Result.success()
    }

    companion object {
        @JvmStatic
        fun addLink(downloadLink: DownloadLink) {
            addDownloadLink(App.instance.mDatabase.booksDownloadScheduleDao(), downloadLink)
            // уведомлю, что размер списка закачек изменился
            BaseActivity.sLiveDownloadScheduleCount.postValue(true)
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
            val bookMime = getDownloadMime(link.mime!!)
            // если сумма символов меньше 255- создаю полное имя
            if (authorLastName.length + bookName.length + bookMime!!.length + 2 < 255 / 2 - 6) {
                newScheduleElement.name =
                    authorLastName + "_" + bookName + "_" + random + "." + bookMime
            } else {
                // сохраняю книгу по имени автора и тому, что влезет от имени книги
                newScheduleElement.name = authorLastName + "_" + bookName.substring(
                    0,
                    127 - (authorLastName.length + bookMime.length + 2 + 6)
                ) + "_" + random + "." + bookMime
            }
            //===========================================================================
            dao.insert(newScheduleElement)
        }
    }
}