package net.veldor.flibustaloader.workers

import android.content.Context
import android.util.Log
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.database.entity.DownloadedBooks
import net.veldor.flibustaloader.ecxeptions.BookNotFoundException
import net.veldor.flibustaloader.ecxeptions.DownloadsDirNotFoundException
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException
import net.veldor.flibustaloader.handlers.LoadedBookHandler
import net.veldor.flibustaloader.http.ExternalVpnVewClient
import net.veldor.flibustaloader.http.TorWebClient
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.selections.CurrentBookDownloadProgress
import net.veldor.flibustaloader.selections.TotalBookDownloadProgress
import net.veldor.flibustaloader.ui.BaseActivity
import net.veldor.flibustaloader.utils.Grammar
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.RandomString
import net.veldor.flibustaloader.utils.URLHelper
import net.veldor.flibustaloader.view_models.DownloadScheduleViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*

class DownloadBooksWorker(
    context: Context,
    workerParams: WorkerParameters
) :
    Worker(context, workerParams) {

    private val downloadErrors: ArrayList<BooksDownloadSchedule> = arrayListOf()

    override fun doWork(): Result {
        App.instance.liveDownloadState.postValue(DOWNLOAD_IN_PROGRESS)
        val downloadStartTime = System.currentTimeMillis()
        // проверю, есть ли в очереди скачивания книги
        val db = App.instance.mDatabase
        val dao = db.booksDownloadScheduleDao()
        val downloadBooksDao = db.downloadedBooksDao()
        val reDownload = PreferencesHandler.instance.isReDownload
        try {
            // получу количество книг на начало скачивания
            var booksCount = dao.queueSize
            var bookDownloadsWithErrors = 0
            var booksDownloadedYet = 0
            var totalBooksCount: Int
            if (booksCount > 0) {
                // помечу рабочего важным
                val info = createForegroundInfo()
                setForegroundAsync(info)
                var queuedElement: BooksDownloadSchedule?
                // начну скачивание
                // периодически удостовериваюсь, что работа не отменена
                if (isStopped) {
                    // немедленно прекращаю работу
                    NotificationHandler.instance.cancelBookLoadNotification()
                    App.instance.liveDownloadState.postValue(DOWNLOAD_FINISHED)
                    return Result.success()
                }
                var downloadCounter = 1
                // получу оставшееся количество книг для загрузки
                totalBooksCount = booksDownloadedYet + dao.queueSize

                var progress = TotalBookDownloadProgress()
                progress.total = totalBooksCount
                progress.loaded = downloadCounter
                progress.failed = bookDownloadsWithErrors
                DownloadScheduleViewModel.liveFullBookDownloadProgress.postValue(progress)
                NotificationHandler.instance.updateDownloadProgress(
                    totalBooksCount,
                    downloadCounter,
                    downloadStartTime
                )
                while (true) {
                    if (isStopped) {
                        // немедленно прекращаю работу
                        NotificationHandler.instance.cancelBookLoadNotification()
                        App.instance.liveDownloadState.postValue(DOWNLOAD_FINISHED)
                        return Result.success()
                    }
                    // получу первый элемент из очереди
                    queuedElement = dao.firstQueuedBook
                    if (queuedElement == null || isStopped) {
                        break
                    }
                    // получу оставшееся количество книг для загрузки
                    totalBooksCount = booksDownloadedYet + dao.queueSize

                    queuedElement.name = queuedElement.name.replace("\\p{C}".toRegex(), "")

                    progress = TotalBookDownloadProgress()
                    progress.total = totalBooksCount
                    progress.loaded = downloadCounter
                    progress.failed = bookDownloadsWithErrors
                    DownloadScheduleViewModel.liveFullBookDownloadProgress.postValue(progress)
                    NotificationHandler.instance.updateDownloadProgress(
                        totalBooksCount,
                        downloadCounter,
                        downloadStartTime
                    )

                    // проверю, не загружалась ли уже книга, если загружалась и запрещена повторная загрузка- пропущу её
                    if (!reDownload && downloadBooksDao.getBookById(queuedElement.bookId) != null) {
                        dao.delete(queuedElement)
                        // уведомлю, что размер списка закачек изменился
                        BaseActivity.sLiveDownloadScheduleCountChanged.postValue(true)
                        continue
                    }
                    // загружу книгу
                    try {
                        App.instance.liveBookDownloadInProgress.postValue(queuedElement)
                        downloadBook(queuedElement)
                        if (!isStopped) {
                            if (queuedElement.loaded) {
                                // отмечу книгу как скачанную
                                val downloadedBook = DownloadedBooks()
                                downloadedBook.bookId = queuedElement.bookId
                                downloadBooksDao.insert(downloadedBook)
                                // удалю книгу из очереди скачивания
                                dao.delete(queuedElement)
                                // уведомлю, что размер списка закачек изменился
                                BaseActivity.sLiveDownloadScheduleCountChanged.postValue(true)
                                // оповещу о скачанной книге
                                App.instance.liveBookJustLoaded.postValue(queuedElement)
                                App.instance.mLiveDownloadedBookId.postValue(queuedElement.bookId)
                                // если не клянчил донаты- поклянчу :)
                                if (!PreferencesHandler.instance.askedForDonation()) {
                                    NotificationHandler.instance.begDonation()
                                    PreferencesHandler.instance.setDonationBegged()
                                }
                            } else {
                                NotificationHandler.instance.sendBookNotFoundInCurrentFormatNotification(
                                    queuedElement
                                )
                                App.instance.liveBookJustError.postValue(queuedElement)
                                bookDownloadsWithErrors++
                                downloadErrors.add(queuedElement)
                                dao.delete(queuedElement)
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(
                            "surprise",
                            "DownloadBooksWorker doWork 173: catch book not found error"
                        )
                        e.printStackTrace()
                        // ошибка загрузки книг, выведу сообщение об ошибке
                        App.instance.liveBookJustError.postValue(queuedElement)
                        NotificationHandler.instance.sendBookNotFoundInCurrentFormatNotification(
                            queuedElement
                        )
                        bookDownloadsWithErrors++
                        downloadErrors.add(queuedElement)
                        dao.delete(queuedElement)
                        // уведомлю, что размер списка закачек изменился
                        BaseActivity.sLiveDownloadScheduleCountChanged.postValue(true)
                    }
                    ++downloadCounter
                    ++booksDownloadedYet
                }
                // цикл закончился, проверю, все ли книги загружены
                booksCount = dao.queueSize
                if (booksCount == 0 && !isStopped) {
                    // ура, всё загружено, выведу сообщение об успешной загрузке
                    NotificationHandler.instance.showBooksLoadedNotification(bookDownloadsWithErrors)
                }
            }
        } finally {
            if (!downloadErrors.isNullOrEmpty()) {
                downloadErrors.forEach {
                    dao.insert(it)
                }
                BaseActivity.sLiveDownloadScheduleCountChanged.postValue(true)
            }
        }
        NotificationHandler.instance.cancelBookLoadNotification()
        App.instance.liveDownloadState.postValue(DOWNLOAD_FINISHED)
        return Result.success()
    }


    @Throws(
        BookNotFoundException::class,
        TorNotLoadedException::class,
        DownloadsDirNotFoundException::class
    )
    private fun downloadBook(book: BooksDownloadSchedule): Boolean {
        val base = URLHelper.getBaseUrl()
        var link = book.link
        if (base.endsWith("/") && link.startsWith("/")) {
            link = link.substring(1)
        }
        val startTime = System.currentTimeMillis()
        val bookUrl = base + link
        Log.d("surprise", "downloadBook: load $bookUrl")
        // получу response доступным способом
        val response =
            if (PreferencesHandler.instance.isExternalVpn) ExternalVpnVewClient.rawRequest(bookUrl) else TorWebClient().rawRequest(
                bookUrl
            )
        if (response != null) {
            // проверю, что запрос успешен
            if (response.statusLine.statusCode in 200..310) {
                if (book.name.isEmpty()) {
                    // try to get file name with extension
                    val filenameHeader = response.getLastHeader("Content-Disposition")
                    if (filenameHeader != null) {
                        book.name = Grammar.removeExtension(
                            filenameHeader.value.replace(
                                "attachment; filename=",
                                ""
                            )
                        )
                    }
                }
                // ответ сервера получен
                // загружу временный файл
                val content = response.entity.content
                val contentLength = response.entity.contentLength
                if (content != null && response.entity.contentLength > 0) {
                    val tempFile = File.createTempFile(RandomString().nextString(), null)
                    tempFile.deleteOnExit()
                    val out: OutputStream = FileOutputStream(tempFile)
                    var read: Int
                    val buffer = ByteArray(1024)
                    while (content.read(buffer).also { read = it } > 0) {
                        if (isStopped) {
                            NotificationHandler.instance.cancelBookLoadingProgressNotification()
                            // немедленно прекращаю работу
                            NotificationHandler.instance.cancelBookLoadNotification()
                            App.instance.liveDownloadState.postValue(DOWNLOAD_FINISHED)
                            return false
                        }
                        out.write(buffer, 0, read)


                        val progress = CurrentBookDownloadProgress()
                        progress.fullSize = contentLength
                        progress.loadedSize = tempFile.length()
                        DownloadScheduleViewModel.liveCurrentBookDownloadProgress.postValue(progress)
                        if (PreferencesHandler.instance.showDownloadProgress) {
                            NotificationHandler.instance.createBookLoadingProgressNotification(
                                contentLength.toInt(),
                                tempFile.length().toInt(),
                                book.name,
                                startTime
                            )
                        }
                    }
                    out.close()
                    content.close()
                    NotificationHandler.instance.cancelBookLoadingProgressNotification()
                    if (tempFile.length() > 1000) {
                        Log.d(
                            "surprise",
                            "downloadBook: loaded something with content length ${tempFile.length()} and status ${response.statusLine.statusCode}"
                        )
                        // книга загружена, помещу её в нужную папку и правильно назову
                        if (isStopped) {
                            // немедленно прекращаю работу
                            NotificationHandler.instance.cancelBookLoadNotification()
                            App.instance.liveDownloadState.postValue(DOWNLOAD_FINISHED)
                            return false
                        }
                        LoadedBookHandler().saveBook(book, response, tempFile)
                        book.loaded = true
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun createForegroundInfo(): ForegroundInfo {
        // Build a notification
        val notification = NotificationHandler.instance.createMassBookLoadNotification()
        return ForegroundInfo(NotificationHandler.DOWNLOAD_PROGRESS_NOTIFICATION, notification)
    }

    companion object {
        @JvmStatic
        fun dropDownloadsQueue() {
            // удалю из базы данных всю очередь скачивания
            val db = App.instance.mDatabase
            val dao = db.booksDownloadScheduleDao()
            val schedule = dao.allBooks
            if (schedule != null && schedule.isNotEmpty()) {
                for (b in schedule) {
                    dao.delete(b)
                }
            }
            // уведомлю, что размер списка закачек изменился
            BaseActivity.sLiveDownloadScheduleCountChanged.postValue(true)
        }

        @JvmStatic
        fun removeFromQueue(scheduleItem: BooksDownloadSchedule?) {
            val db = App.instance.mDatabase
            val dao = db.booksDownloadScheduleDao()
            dao.delete(scheduleItem)
            // уведомлю, что размер списка закачек изменился
            BaseActivity.sLiveDownloadScheduleCountChanged.postValue(true)
        }

        @JvmStatic
        fun skipFirstBook() {
            val db = App.instance.mDatabase
            val dao = db.booksDownloadScheduleDao()
            dao.delete(dao.firstQueuedBook)
            // уведомлю, что размер списка закачек изменился
            BaseActivity.sLiveDownloadScheduleCountChanged.postValue(true)
        }

        const val DOWNLOAD_IN_PROGRESS = "download in progress"
        const val DOWNLOAD_FINISHED = "download finished"
    }

    override fun onStopped() {
        super.onStopped()
        Log.d("surprise", "onStopped: download interrupted")
    }
}