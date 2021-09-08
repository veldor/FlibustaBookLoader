package net.veldor.flibustaloader.workers

import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.http.TorWebClient
import android.content.Context
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.ui.BaseActivity
import net.veldor.flibustaloader.database.entity.DownloadedBooks
import net.veldor.flibustaloader.ecxeptions.BookNotFoundException
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException
import kotlin.Throws
import net.veldor.flibustaloader.http.ExternalVpnVewClient
import net.veldor.flibustaloader.ecxeptions.ConnectionLostException
import net.veldor.flibustaloader.view_models.MainViewModel
import androidx.lifecycle.LiveData
import android.util.Log
import androidx.work.*
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.util.ArrayList
import java.util.concurrent.ExecutionException

class DownloadBooksWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private val mNotificator: NotificationHandler = NotificationHandler.instance
    override fun doWork(): Result {
        downloadInProgress = true
        val downloadStartTime = System.currentTimeMillis()
        if (App.instance.useMirror) {
            // оповещу о невозможности скачивания книг с альтернативного зеркала
            mNotificator.notifyUseAlternativeMirror()
        }
        val downloadErrors = ArrayList<BooksDownloadSchedule>()
        // проверю, есть ли в очереди скачивания книги
        val db = App.instance.mDatabase
        val dao = db.booksDownloadScheduleDao()
        val downloadBooksDao = db.downloadedBooksDao()
        val reDownload = PreferencesHandler.instance.isReDownload
        try {
            // получу количество книг на начало скачивания
            var mBooksCount = dao.queueSize
            var bookDownloadsWithErrors = 0
            if (mBooksCount > 0) {
                // помечу рабочего важным
                val info = createForegroundInfo()
                setForegroundAsync(info)
                // создам уведомление о скачивании
                mNotificator.mDownloadScheduleBuilder!!.setProgress(mBooksCount, 0, true)
                mNotificator.mNotificationManager.notify(
                    NotificationHandler.DOWNLOAD_PROGRESS_NOTIFICATION,
                    mNotificator.mDownloadScheduleBuilder!!.build()
                )
                var queuedElement: BooksDownloadSchedule
                // начну скачивание
                // периодически удостовериваюсь, что работа не отменена
                if (isStopped) {
                    // немедленно прекращаю работу
                    mNotificator.cancelBookLoadNotification()
                    downloadInProgress = false
                    return Result.success()
                }
                var downloadCounter = 1
                // пока есть книги в очереди скачивания и работа не остановлена
                while (dao.firstQueuedBook.also { queuedElement = it!! } != null && !isStopped) {
                    queuedElement.name = queuedElement.name.replace("\\p{C}".toRegex(), "")
                    mNotificator.updateDownloadProgress(
                        dao.queueSize + downloadCounter - 1,
                        downloadCounter,
                        downloadStartTime
                    )
                    // проверю, не загружалась ли уже книга, если загружалась и запрещена повторная загрузка- пропущу её
                    if (!reDownload && downloadBooksDao.getBookById(queuedElement.bookId) != null) {
                        dao.delete(queuedElement)
                        // уведомлю, что размер списка закачек изменился
                        BaseActivity.sLiveDownloadScheduleCount.postValue(true)
                        continue
                    }
                    // загружу книгу
                    try {
                        downloadBook(queuedElement)
                        if (!isStopped) {
                            if (queuedElement.loaded) {
                                // отмечу книгу как скачанную
                                val downloadedBook = DownloadedBooks()
                                downloadedBook.bookId = queuedElement.bookId
                                downloadBooksDao.insert(downloadedBook)
                                // удалю книгу из очереди скачивания
                                // покажу уведомление о успешной загрузке
                                mNotificator.sendLoadedBookNotification(queuedElement)
                                dao.delete(queuedElement)
                                // уведомлю, что размер списка закачек изменился
                                BaseActivity.sLiveDownloadScheduleCount.postValue(true)
                                // оповещу о скачанной книге
                                App.instance.mLiveDownloadedBookId.postValue(queuedElement.bookId)
                                // если не клянчил донаты- поклянчу :)
                                if (!PreferencesHandler.instance.askedForDonation()) {
                                    mNotificator.begDonation()
                                    PreferencesHandler.instance.setDonationBegged()
                                }
                            } else {
                                Log.d(
                                    "surprise",
                                    "DownloadBooksWorker doWork 178: book not loaded or loaded with zero size"
                                )
                                mNotificator.sendBookNotFoundInCurrentFormatNotification(
                                    queuedElement
                                )
                                bookDownloadsWithErrors++
                                downloadErrors.add(queuedElement)
                                dao.delete(queuedElement)
                            }
                        }
                    } catch (e: BookNotFoundException) {
                        Log.d(
                            "surprise",
                            "DownloadBooksWorker doWork 173: catch book not found error"
                        )
                        // ошибка загрузки книг, выведу сообщение об ошибке
                        mNotificator.sendBookNotFoundInCurrentFormatNotification(queuedElement)
                        bookDownloadsWithErrors++
                        downloadErrors.add(queuedElement)
                        dao.delete(queuedElement)
                        // уведомлю, что размер списка закачек изменился
                        BaseActivity.sLiveDownloadScheduleCount.postValue(true)
                    } catch (e: TorNotLoadedException) {
                        Log.d(
                            "surprise",
                            "DownloadBooksWorker doWork 172: catch tor load exception, download work stopped"
                        )
                        e.printStackTrace()
                        // при ошибке загрузки TOR остановлю работу
                        if (downloadErrors.size > 0) {
                            for (b in downloadErrors) {
                                dao.insert(b)
                                downloadErrors.remove(b)
                            }
                        }
                        mNotificator.cancelBookLoadNotification()
                        mNotificator.showTorNotLoadedNotification()
                        downloadInProgress = false
                        return Result.success()
                    }
                    ++downloadCounter
                }
                // цикл закончился, проверю, все ли книги загружены
                mBooksCount = dao.queueSize
                if (mBooksCount == 0 && !isStopped) {
                    // ура, всё загружено, выведу сообщение об успешной загрузке
                    mNotificator.showBooksLoadedNotification(bookDownloadsWithErrors)
                    // Добавлю все книги с ошибками обратно в список загрузки
                    for (b in downloadErrors) {
                        dao.insert(b)
                        downloadErrors.remove(b)
                    }
                    // уведомлю, что размер списка закачек изменился
                    BaseActivity.sLiveDownloadScheduleCount.postValue(true)
                }
            }
        } finally {
            if (downloadErrors.size > 0) {
                for (b in downloadErrors) {
                    dao.insert(b)
                    downloadErrors.remove(b)
                }
            }
        }
        mNotificator.cancelBookLoadNotification()
        downloadInProgress = false
        return Result.success()
    }

    @Throws(BookNotFoundException::class, TorNotLoadedException::class)
    private fun downloadBook(book: BooksDownloadSchedule) {
        if (PreferencesHandler.instance.isExternalVpn) {
            Log.d("surprise", "DownloadBooksWorker downloadBook try download trough external vpn")
            ExternalVpnVewClient.downloadBook(book)
        } else {
            // настрою клиент
            try {
                val client = TorWebClient()
                client.downloadBook(book)
            } catch (e: ConnectionLostException) {
                mNotificator.showTorNotLoadedNotification()
                e.printStackTrace()
                throw TorNotLoadedException()
            }
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        // Build a notification
        val notification = mNotificator.createMassBookLoadNotification()
        return ForegroundInfo(NotificationHandler.DOWNLOAD_PROGRESS_NOTIFICATION, notification)
    }

    companion object {
        @JvmField
        var downloadInProgress = false
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
            BaseActivity.sLiveDownloadScheduleCount.postValue(true)
        }

        @JvmStatic
        fun removeFromQueue(scheduleItem: BooksDownloadSchedule?) {
            val db = App.instance.mDatabase
            val dao = db.booksDownloadScheduleDao()
            dao.delete(scheduleItem)
            // уведомлю, что размер списка закачек изменился
            BaseActivity.sLiveDownloadScheduleCount.postValue(true)
        }

        @JvmStatic
        fun noActiveDownloadProcess(): Boolean {
            // проверю наличие активных процессов скачивания
            val info = WorkManager.getInstance(App.instance)
                .getWorkInfosForUniqueWork(MainViewModel.MULTIPLY_DOWNLOAD)
            try {
                val results = info.get()
                if (results == null || results.size == 0) {
                    return true
                }
                for (v in results) {
                    val status = v.state
                    if (status == WorkInfo.State.ENQUEUED || status == WorkInfo.State.RUNNING) {
                        return false
                    }
                }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            return true
        }

        @JvmStatic
        val downloadProgress: LiveData<List<WorkInfo>>
            get() = WorkManager.getInstance(App.instance)
                .getWorkInfosForUniqueWorkLiveData(MainViewModel.MULTIPLY_DOWNLOAD)

        @JvmStatic
        fun skipFirstBook() {
            val db = App.instance.mDatabase
            val dao = db.booksDownloadScheduleDao()
            dao.delete(dao.firstQueuedBook)
            // уведомлю, что размер списка закачек изменился
            BaseActivity.sLiveDownloadScheduleCount.postValue(true)
        }
    }

}