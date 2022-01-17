package net.veldor.flibustaloader.workers

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.WorkerParameters
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.database.entity.DownloadedBooks
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.ui.BaseActivity
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.util.*


class DownloadBookWorker(
    context: Context,
    workerParams: WorkerParameters
) :
    DownloadBooksWorker(context, workerParams) {

    private val downloadErrors: ArrayList<BooksDownloadSchedule> = arrayListOf()

    override fun doWork(): Result {
        var successLoaded = 0
        val db = App.instance.mDatabase
        val dao = db.booksDownloadScheduleDao()
        val downloadBooksDao = db.downloadedBooksDao()
        val downloadStartTime = System.currentTimeMillis()
        // проверю, есть ли в очереди скачивания книги
        try {
            val data = inputData
            val serialized = data.getString("item")
            val gson = Gson()
            val queuedElement = gson.fromJson(serialized, BooksDownloadSchedule::class.java)
            // помечу рабочего важным
            val info = createForegroundInfo()
            setForegroundAsync(info)
            // начну скачивание
            // периодически удостовериваюсь, что работа не отменена
            if (isStopped) {
                // немедленно прекращаю работу
                NotificationHandler.instance.cancelBookLoadNotification()
                App.instance.liveDownloadState.postValue(DOWNLOAD_FINISHED)
                return Result.success()
            }
            if (isStopped) {
                return Result.success()
            }
            queuedElement.name = queuedElement.name.replace("\\p{C}".toRegex(), "")
            // загружу книгу
            try {
                App.instance.liveBookDownloadInProgress.postValue(queuedElement)
                downloadBook(queuedElement)
                if (!isStopped) {
                    if (queuedElement.loaded) {
                        successLoaded++
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
                runBlocking(Dispatchers.Main) {
                    // any Main thread needs
                    Toast.makeText(
                        App.instance, String.format(
                            Locale.ENGLISH, App.instance.getString(
                                R.string.book_not_loaded_pattern
                            ), queuedElement.name
                        ), Toast.LENGTH_LONG
                    ).show()
                }
                NotificationHandler.instance.sendBookNotFoundInCurrentFormatNotification(
                    queuedElement
                )
                downloadErrors.add(queuedElement)
                dao.delete(queuedElement)
                // уведомлю, что размер списка закачек изменился
                BaseActivity.sLiveDownloadScheduleCountChanged.postValue(true)
            }
        } finally {
            if (!downloadErrors.isNullOrEmpty()) {
                downloadErrors.forEach {
                    dao.insert(it)
                }
                BaseActivity.sLiveNotDownloaded.postValue(downloadErrors)
                BaseActivity.sLiveDownloadScheduleCountChanged.postValue(true)
            }
        }
        NotificationHandler.instance.cancelBookLoadNotification()
        NotificationHandler.instance.showBooksLoadedNotification(downloadErrors, successLoaded)
        return Result.success()
    }
}