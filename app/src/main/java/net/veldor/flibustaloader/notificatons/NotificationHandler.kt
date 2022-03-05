package net.veldor.flibustaloader.notificatons

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.receivers.BookActionReceiver
import net.veldor.flibustaloader.receivers.BookLoadedReceiver
import net.veldor.flibustaloader.receivers.MiscActionsReceiver
import net.veldor.flibustaloader.ui.DownloadScheduleActivity
import net.veldor.flibustaloader.ui.MainActivity
import net.veldor.flibustaloader.ui.SubscriptionsActivity
import net.veldor.flibustaloader.utils.MimeTypes.getDownloadMime
import java.util.*

@SuppressLint("UnspecifiedImmutableFlag")
class NotificationHandler private constructor(private var context: Context) {

    private var lastDownloadTickTime: Long = 0L

    // system notification manager
    @JvmField
    val mNotificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var mDownloadScheduleBuilder: NotificationCompat.Builder? = null

    private var bookLoadedId = 100
    private var pendingRequestId = 100

    fun sendLoadedBookNotification(name: String, type: String?) {

        // Добавлю группу
        val mBuilder = NotificationCompat.Builder(context, BOOK_DOWNLOADS_CHANNEL_ID)
            .setContentInfo("Загруженные книги")
            .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
            .setGroup(DOWNLOADED_BOOKS_GROUP)
            .setGroupSummary(true)
        mNotificationManager.notify(-100, mBuilder.build())

        // создам интент для функции отправки файла
        val shareIntent = Intent(context, BookActionReceiver::class.java)
        shareIntent.putExtra(
            BookLoadedReceiver.EXTRA_ACTION_TYPE,
            BookLoadedReceiver.ACTION_TYPE_SHARE
        )
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, type)
        shareIntent.putExtra(BookActionReceiver.EXTRA_NOTIFICATION_ID, bookLoadedId)
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, name)
        val sharePendingIntent = PendingIntent.getBroadcast(
            context,
            START_SHARING_REQUEST_CODE,
            shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // создам интент для функции открытия файла
        val openIntent = Intent(context, BookActionReceiver::class.java)
        openIntent.putExtra(
            BookLoadedReceiver.EXTRA_ACTION_TYPE,
            BookLoadedReceiver.ACTION_TYPE_OPEN
        )
        openIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, type)
        openIntent.putExtra(BookActionReceiver.EXTRA_NOTIFICATION_ID, bookLoadedId)
        openIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, name)
        val openPendingIntent = PendingIntent.getBroadcast(
            context,
            START_OPEN_REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openMainIntent = Intent(context, MainActivity::class.java)
        val startMainPending = PendingIntent.getActivity(
            context,
            START_APP_CODE,
            openMainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationBuilder = NotificationCompat.Builder(context, BOOKS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_book_black_24dp)
            .setContentTitle(String.format(context.getString(R.string.loaded_books_title), name))
            .setStyle(NotificationCompat.BigTextStyle().bigText("$name :успешно загружено"))
            .setContentIntent(startMainPending)
            .setDefaults(Notification.DEFAULT_ALL)
            .setGroup(DOWNLOADED_BOOKS_GROUP)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_share_white_24dp, "Отправить", sharePendingIntent)
            .addAction(R.drawable.ic_open_black_24dp, "Открыть", openPendingIntent)
        mNotificationManager.notify(bookLoadedId, notificationBuilder.build())
        ++bookLoadedId
    }

    fun sendBookNotFoundInCurrentFormatNotification(book: BooksDownloadSchedule) {
        // Добавлю группу
        val mBuilder = NotificationCompat.Builder(context, BOOK_DOWNLOADS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning_red_24dp)
            .setContentInfo("Загруженные книги")
            .setContentTitle("Не удалось загрузить книги")
            .setGroup(ERROR_DOWNLOAD_BOOKS_GROUP)
            .setGroupSummary(true)
        mNotificationManager.notify(-101, mBuilder.build())
        val notificationBuilder = NotificationCompat.Builder(context, BOOKS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning_red_24dp)
            .setContentTitle(
                String.format(
                    Locale.ENGLISH,
                    context.getString(R.string.error_load_message),
                    book.name
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    String.format(
                        Locale.ENGLISH,
                        context.getString(R.string.book_unreachable_message),
                        book.name,
                        getDownloadMime(book.format)
                    )
                )
            )
            .setDefaults(Notification.DEFAULT_ALL)
            .setGroup(ERROR_DOWNLOAD_BOOKS_GROUP)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        mNotificationManager.notify(bookLoadedId, notificationBuilder.build())
        ++bookLoadedId
    }

    fun sendFoundSubscribesNotification() {
        val openSubscriptionsIntent = Intent(context, SubscriptionsActivity::class.java)
        openSubscriptionsIntent.putExtra(
            SubscriptionsActivity.START_FRAGMENT,
            SubscriptionsActivity.START_RESULTS
        )
        val startMainPending = PendingIntent.getActivity(
            context,
            START_APP_CODE,
            openSubscriptionsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationBuilder = NotificationCompat.Builder(context, SUBSCRIBES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_book_black_24dp)
            .setContentTitle("Найдены новые книги")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Есть новые поступления по темам, которые вас интересуют")
            )
            .setContentIntent(startMainPending)
            .setAutoCancel(true)
        val notification = notificationBuilder.build()
        mNotificationManager.notify(SUBSCRIBE_NOTIFICATION, notification)
    }

    fun createMassBookLoadNotification(): Notification {
        cancelTorErrorMessage()
        // при нажатии на уведомление- открою экран ожидания очереди
        val openWindowIntent = Intent(context, DownloadScheduleActivity::class.java)
        openWindowIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val showWindowPending = PendingIntent.getActivity(
            context,
            START_APP_CODE,
            openWindowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // интент отмены скачивания
        val cancelIntent = Intent(context, MiscActionsReceiver::class.java)
        cancelIntent.putExtra(
            MiscActionsReceiver.EXTRA_ACTION_TYPE,
            MiscActionsReceiver.ACTION_CANCEL_MASS_DOWNLOAD
        )
        val cancelMassDownloadPendingIntent = PendingIntent.getBroadcast(
            context,
            CANCEL_CODE,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // интент паузы скачивания
        val pauseIntent = Intent(context, MiscActionsReceiver::class.java)
        pauseIntent.putExtra(
            MiscActionsReceiver.EXTRA_ACTION_TYPE,
            MiscActionsReceiver.ACTION_PAUSE_MASS_DOWNLOAD
        )
        val pauseMassDownloadPendingIntent = PendingIntent.getBroadcast(
            context,
            PAUSE_CODE,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        mDownloadScheduleBuilder = NotificationCompat.Builder(context, BOOK_DOWNLOADS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
            .setContentTitle(context.getString(R.string.notification_downloader_header))
            .setOngoing(true)
            .setContentText(context.getString(R.string.notification_downloader_prepare_body))
            .setProgress(0, 0, true)
            .addAction(R.drawable.ic_list_white_24dp, "Очередь", showWindowPending)
            .addAction(R.drawable.fp_ic_action_cancel, "Отмена", cancelMassDownloadPendingIntent)
            .addAction(R.drawable.ic_pause_white_24dp, "Пауза", pauseMassDownloadPendingIntent)
            .setContentIntent(showWindowPending)
            .setAutoCancel(false)
        return mDownloadScheduleBuilder!!.build()
    }

    fun cancelBookLoadNotification() {
        mNotificationManager.cancel(DOWNLOAD_PROGRESS_NOTIFICATION)
    }

    fun showBooksLoadedNotification(
        bookDownloadsWithErrors: ArrayList<BooksDownloadSchedule>,
        downloadCounter: Int
    ) {
        val downloadCompleteBuilder =
            NotificationCompat.Builder(context, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                .setContentTitle("Скачивание книг завершено")
        if (bookDownloadsWithErrors.size == 0) {
            downloadCompleteBuilder.setContentText("Скачано книг: $downloadCounter")
        } else {
            downloadCompleteBuilder.setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Скачано: $downloadCounter, ${bookDownloadsWithErrors.size} скачать не удалось. Они оставлены в очереди скачивания."
                )
            )
            // интент очистки очереди скачивания
            val cancelIntent = Intent(context, MiscActionsReceiver::class.java)
            cancelIntent.putExtra(
                MiscActionsReceiver.EXTRA_ACTION_TYPE,
                MiscActionsReceiver.ACTION_CLEAN_DOWNLOAD_QUEUE
            )
            val cancelMassDownloadPendingIntent = PendingIntent.getBroadcast(
                context,
                CLEAR_DOWNLOAD_QUEUE_CODE,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val resumeIntent = Intent(context, MiscActionsReceiver::class.java)
            resumeIntent.putExtra(
                MiscActionsReceiver.EXTRA_ACTION_TYPE,
                MiscActionsReceiver.ACTION_RESUME_MASS_DOWNLOAD
            )
            val resumeMassDownloadPendingIntent = PendingIntent.getBroadcast(
                context,
                RESUME_DOWNLOAD_CODE,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            downloadCompleteBuilder.addAction(
                R.drawable.ic_close_grayscale_24dp, context.getString(
                    R.string.drop_queue_action
                ), cancelMassDownloadPendingIntent
            )
            downloadCompleteBuilder.addAction(
                R.drawable.ic_baseline_restore_black_24, context.getString(
                    R.string.requme_download_action
                ), resumeMassDownloadPendingIntent
            )
        }
        mNotificationManager.notify(BOOKS_SUCCESS_NOTIFICATION, downloadCompleteBuilder.build())
    }

//    fun createMassDownloadStoppedNotification() {
//        cancelTorErrorMessage()
//        val downloadStoppedBuilder = NotificationCompat.Builder(context, BOOK_DOWNLOADS_CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
//            .setContentTitle("Скачивание книг")
//            .setContentText("Скачивание книг остановлено!")
//        mNotificationManager.notify(
//            DOWNLOAD_PROGRESS_NOTIFICATION,
//            downloadStoppedBuilder.build()
//        )
//    }

    fun createMassDownloadPausedNotification() {
        // интент возобновления скачивания
        val resumeIntent = Intent(context, MiscActionsReceiver::class.java)
        resumeIntent.putExtra(
            MiscActionsReceiver.EXTRA_ACTION_TYPE,
            MiscActionsReceiver.ACTION_RESUME_MASS_DOWNLOAD
        )
        val resumeMassDownloadPendingIntent = PendingIntent.getBroadcast(
            context,
            RESUME_DOWNLOAD_CODE,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val downloadCompleteBuilder =
            NotificationCompat.Builder(context, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pause_white_24dp)
                .setContentTitle("Скачивание книг")
                .addAction(
                    R.drawable.ic_play_arrow_white_24dp,
                    "Возобновить",
                    resumeMassDownloadPendingIntent
                )
                .setContentText("Скачивание приостановлено!")
        mNotificationManager.notify(DOWNLOAD_PAUSED_NOTIFICATION, downloadCompleteBuilder.build())
    }

    fun showTorNotLoadedNotification() {
        Log.d("surprise", "Notificator showTorNotLoadedNotification: tor can't")
        // интент возобновления скачивания
        val restartTorIntent = Intent(context, MiscActionsReceiver::class.java)
        restartTorIntent.putExtra(
            MiscActionsReceiver.EXTRA_ACTION_TYPE,
            MiscActionsReceiver.ACTION_RESTART_TOR
        )
        val restartTorPendingIntent = PendingIntent.getBroadcast(
            context,
            RESTART_TOR_CODE,
            restartTorIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val downloadCompleteBuilder = NotificationCompat.Builder(context, MISC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning_red_24dp)
            .setContentTitle(context.getString(R.string.tor_init_error_message))
            .addAction(
                R.drawable.ic_play_arrow_white_24dp,
                context.getString(R.string.request_restart_tor_message),
                restartTorPendingIntent
            )
            .setContentText(context.getString(R.string.download_paused_message))
        mNotificationManager.notify(TOR_LOAD_ERROR_NOTIFICATION, downloadCompleteBuilder.build())
    }

    private fun cancelTorErrorMessage() {
        mNotificationManager.cancel(TOR_LOAD_ERROR_NOTIFICATION)
    }

    fun showMassDownloadInQueueMessage() {
        // интент паузы скачивания
        val pauseIntent = Intent(context, MiscActionsReceiver::class.java)
        pauseIntent.putExtra(
            MiscActionsReceiver.EXTRA_ACTION_TYPE,
            MiscActionsReceiver.ACTION_PAUSE_MASS_DOWNLOAD
        )
        val pauseMassDownloadPendingIntent = PendingIntent.getBroadcast(
            context,
            PAUSE_CODE,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // интент отмены скачивания
        val cancelIntent = Intent(context, MiscActionsReceiver::class.java)
        cancelIntent.putExtra(
            MiscActionsReceiver.EXTRA_ACTION_TYPE,
            MiscActionsReceiver.ACTION_CANCEL_MASS_DOWNLOAD
        )
        val cancelMassDownloadPendingIntent = PendingIntent.getBroadcast(
            context,
            CANCEL_CODE,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // отменю уведомление об ошибке TOR
        cancelTorErrorMessage()
        // интент возобновления скачивания
        val restartTorIntent = Intent(context, MiscActionsReceiver::class.java)
        restartTorIntent.putExtra(
            MiscActionsReceiver.EXTRA_ACTION_TYPE,
            MiscActionsReceiver.ACTION_RESTART_TOR
        )
        val restartTorPendingIntent = PendingIntent.getBroadcast(
            context,
            RESTART_TOR_CODE,
            restartTorIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // добавлю интент для отображения экрана очереди скачивания
        val openScheduleIntent = Intent(context, DownloadScheduleActivity::class.java)
        openScheduleIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val openSchedulePendingIntent = PendingIntent.getActivity(
            context,
            OPEN_SCHEDULE_CODE,
            openScheduleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // покажу уведомление об ожидании подключения
        val notificationBuilder = NotificationCompat.Builder(context, BOOK_DOWNLOADS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pause_white_24dp)
            .setContentIntent(openSchedulePendingIntent)
            .setContentTitle(context.getString(R.string.download_paused_message))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.waiting_internet_message))
            )
            .setProgress(0, 0, true)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_play_arrow_white_24dp,
                context.getString(R.string.restart_tor_from_notification_message),
                restartTorPendingIntent
            )
            .addAction(
                R.drawable.fp_ic_action_cancel,
                context.getString(R.string.cancel_download_from_notification_message),
                cancelMassDownloadPendingIntent
            )
            .addAction(
                R.drawable.ic_pause_white_24dp,
                context.getString(R.string.later_message),
                pauseMassDownloadPendingIntent
            )
            .setAutoCancel(false)
        mNotificationManager.notify(
            MASS_DOWNLOAD_PAUSED_NOTIFICATION,
            notificationBuilder.build()
        )
    }

    fun hideMassDownloadInQueueMessage() {
        mNotificationManager.cancel(MASS_DOWNLOAD_PAUSED_NOTIFICATION)
    }

    val checkSubscribesNotification: Notification
        get() {
            val notificationBuilder =
                NotificationCompat.Builder(context, SUBSCRIBE_CHECK_SERVICE_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_book_black_24dp)
                    .setContentTitle(
                        App.instance.getString(R.string.check_flibusta_subscriptions_message)
                    )
                    .setProgress(0, 0, true)
                    .setOngoing(true)
            return notificationBuilder.build()
        }
    val startTorNotification: Notification
        get() {
            val notificationBuilder =
                NotificationCompat.Builder(context, SUBSCRIBE_CHECK_SERVICE_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_book_black_24dp)
                    .setContentTitle(
                        App.instance.getString(R.string.start_tor_message)
                    )
                    .setProgress(0, 0, true)
                    .setOngoing(true)
            return notificationBuilder.build()
        }

    fun updateDownloadProgress(mBooksCount: Int, currentDownload: Int, beginningTime: Long) {
        val left = System.currentTimeMillis() - beginningTime
        val forBook = left / currentDownload
        (mBooksCount - currentDownload) * forBook / 1000
        // при нажатии на уведомление- открою экран ожидания очереди
        val openWindowIntent = Intent(context, DownloadScheduleActivity::class.java)
        openWindowIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val showWindowPending = PendingIntent.getActivity(
            context,
            START_APP_CODE,
            openWindowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // интент отмены скачивания
        val cancelIntent = Intent(context, MiscActionsReceiver::class.java)
        cancelIntent.putExtra(
            MiscActionsReceiver.EXTRA_ACTION_TYPE,
            MiscActionsReceiver.ACTION_CANCEL_MASS_DOWNLOAD
        )
        val cancelMassDownloadPendingIntent = PendingIntent.getBroadcast(
            context,
            CANCEL_CODE,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // интент паузы скачивания
        val pauseIntent = Intent(context, MiscActionsReceiver::class.java)
        pauseIntent.putExtra(
            MiscActionsReceiver.EXTRA_ACTION_TYPE,
            MiscActionsReceiver.ACTION_PAUSE_MASS_DOWNLOAD
        )
        val pauseMassDownloadPendingIntent = PendingIntent.getBroadcast(
            context,
            PAUSE_CODE,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        mDownloadScheduleBuilder = NotificationCompat.Builder(context, BOOK_DOWNLOADS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
            .setContentTitle("Скачивание книг")
            .setOngoing(true)
            .setContentText("Загружаю книгу $currentDownload из $mBooksCount")
            .setProgress(mBooksCount, currentDownload, false)
            .addAction(R.drawable.ic_list_white_24dp, "Очередь", showWindowPending)
            .addAction(R.drawable.fp_ic_action_cancel, "Отмена", cancelMassDownloadPendingIntent)
            .addAction(R.drawable.ic_pause_white_24dp, "Пауза", pauseMassDownloadPendingIntent)
            .setContentIntent(showWindowPending)
            .setAutoCancel(false)
        mDownloadScheduleBuilder!!.build()
        mNotificationManager.notify(
            DOWNLOAD_PROGRESS_NOTIFICATION,
            mDownloadScheduleBuilder!!.build()
        )
    }


    fun sendLoadedBookNotification(bookName: String) {

        val shareIntent = Intent(context, BookActionReceiver::class.java)
        shareIntent.putExtra(
            BookLoadedReceiver.EXTRA_ACTION_TYPE,
            BookLoadedReceiver.ACTION_TYPE_SHARE
        )
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, bookName)
        shareIntent.putExtra(BookActionReceiver.EXTRA_NOTIFICATION_ID, bookLoadedId)
        var pendingRequestId = this.pendingRequestId++
        val sharePendingIntent = PendingIntent.getBroadcast(
            context,
            pendingRequestId,
            shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openIntent = Intent(context, BookActionReceiver::class.java)
        openIntent.putExtra(
            BookLoadedReceiver.EXTRA_ACTION_TYPE,
            BookLoadedReceiver.ACTION_TYPE_OPEN
        )
        pendingRequestId = this.pendingRequestId++
        openIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, bookName)
        openIntent.putExtra(BookActionReceiver.EXTRA_NOTIFICATION_ID, bookLoadedId)
        val openPendingIntent = PendingIntent.getBroadcast(
            context,
            pendingRequestId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(context, BOOKS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_book_black_24dp)
            .setContentTitle(
                String.format(
                    context.getString(R.string.loaded_books_title),
                    bookName
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$bookName :успешно загружено")
            )
            .setDefaults(Notification.DEFAULT_ALL)
            .setGroup(DOWNLOADED_BOOKS_GROUP)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_share_white_24dp, "Отправить", sharePendingIntent)
            .addAction(R.drawable.ic_open_black_24dp, "Открыть", openPendingIntent)
        mNotificationManager.notify(bookLoadedId, notificationBuilder.build())
        ++bookLoadedId
    }

    fun begDonation() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://money.yandex.ru/to/41001269882689")
        val openPendingIntent = PendingIntent.getActivity(
            context,
            DONATE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationBuilder = NotificationCompat.Builder(context, MISC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_attach_money_24)
            .setContentTitle("Поддержать разработку")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Если вам понравилось приложение- можете кинуть мне денег на кофе. Если у вас есть предложения/пожелания- можете написать их в комментарии к переводу :)")
            )
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_baseline_attach_money_24, "Пожертвовать", openPendingIntent)
            .setAutoCancel(true)
        val notification = notificationBuilder.build()
        mNotificationManager.notify(MISC_CODE, notification)
    }

    fun createBookLoadingProgressNotification(
        contentLength: Int,
        loaded: Int,
        name: String,
        startTime: Long
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDownloadTickTime < 1000) {
            return
        }
        lastDownloadTickTime = currentTime
        // пересчитаю байты в килобайты
        val total = contentLength.toDouble() / 1024
        val nowLoaded = loaded.toDouble() / 1024
        var percentDone = 0.0
        if (loaded > 0) {
            percentDone = loaded.toDouble() / contentLength.toDouble() * 100
        }
        var timeLeftInMillis = 0
        val left = currentTime - startTime
        if (percentDone >= 1) {
            val timeForPercent = left.toInt() / percentDone.toInt()
            val percentsLeft = 100 - percentDone.toInt()
            timeLeftInMillis = percentsLeft * timeForPercent
        }
        val timeLeftInSeconds = timeLeftInMillis / 1000
        val textLeft: String = if (timeLeftInSeconds / 60 > 0) {
            (timeLeftInSeconds / 60).toString() + " мин. " + timeLeftInSeconds % 60 + " сек."
        } else {
            (timeLeftInSeconds % 60).toString() + " сек."
        }
        val notification = NotificationCompat.Builder(context, BOOK_DOWNLOAD_PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
            .setContentTitle("Качаю $name")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    String.format(
                        Locale.ENGLISH,
                        App.instance.getString(R.string.loaded_message),
                        nowLoaded,
                        total,
                        percentDone,
                        textLeft
                    )
                )
            )
            .setOngoing(true)
            .setProgress(contentLength, loaded, false)
            .setAutoCancel(false).build()
        mNotificationManager.notify(BOOK_DOWNLOAD_PROGRESS, notification)
    }

    fun cancelBookLoadingProgressNotification() {
        mNotificationManager.cancel(BOOK_DOWNLOAD_PROGRESS)
    }

    fun notifyFlibustaIsBack() {
        val contentIntent = Intent(App.instance, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            App.instance, 0,
            contentIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationBuilder = NotificationCompat.Builder(App.instance, MISC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_bookmark_24)
            .setContentTitle("Флибуста вернулась!")
            .setContentText("Нажмите сюда, чтобы начать сёрфить")
            .setColor(Color.GREEN)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
        val incomingCallNotification = notificationBuilder.build()
        mNotificationManager.notify(FLIBUSTA_IS_BACK_NOTIFICATION, incomingCallNotification)
    }

    fun notifyUseAlternativeMirror() {
        val notificationBuilder = NotificationCompat.Builder(App.instance, MISC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_bookmark_24)
            .setContentTitle("Используется альтернативное зеркало!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Сейчас используется зеркало, на котором доступ к большинству книг ограничен. Могут возникнуть проблемы со скачиванием!")
            )
            .setColor(Color.RED)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        val incomingCallNotification = notificationBuilder.build()
        mNotificationManager.notify(MIRROR_DOWNLOAD_USING_NOTIFICATION, incomingCallNotification)
    }

    val checkAvailabilityNotification: Notification
        get() {
            val notificationBuilder = NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_black_24dp)
                .setContentTitle(
                    App.instance.getString(R.string.check_flibusta_availability_message)
                )
                .setProgress(0, 0, true)
                .setOngoing(true)
            return notificationBuilder.build()
        }

    fun showTestVersionNotification() {
        // интент отправки логов
        val sendLogsToMailIntent = Intent(context, MiscActionsReceiver::class.java)
        sendLogsToMailIntent.putExtra(
            MiscActionsReceiver.EXTRA_ACTION_TYPE,
            MiscActionsReceiver.ACTION_SEND_LOGS_TO_MAIL
        )
        val sendLogsToMailPendingIntent = PendingIntent.getBroadcast(
            context,
            SEND_LOG_TO_MAIL_CODE,
            sendLogsToMailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // интент отправки логов
        val sendLogsIntent = Intent(context, MiscActionsReceiver::class.java)
        sendLogsIntent.putExtra(
            MiscActionsReceiver.EXTRA_ACTION_TYPE,
            MiscActionsReceiver.ACTION_SEND_LOGS
        )
        val sendLogsPendingIntent = PendingIntent.getBroadcast(
            context,
            SEND_LOG_CODE,
            sendLogsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationBuilder = NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_bookmark_24)
            .setContentTitle(App.instance.getString(R.string.test_version_message))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Спасибо, что помогаете с тестированием. Если у вас возникли проблемы- вы можете получить логи, нажав кнопку ниже, и отправить мне удобным способом")
            )
            .addAction(
                R.drawable.ic_baseline_bookmark_24,
                App.instance.getString(R.string.send_log_message),
                sendLogsPendingIntent
            )
            .addAction(
                R.drawable.ic_baseline_bookmark_24,
                App.instance.getString(R.string.send_log_to_mail_message),
                sendLogsToMailPendingIntent
            )
        mNotificationManager.notify(IS_TEST_VERSION_NOTIFICATION, notificationBuilder.build())
    }

    fun updateTorStarter(lastLog: String) {
        val notificationBuilder =
            NotificationCompat.Builder(context, SUBSCRIBE_CHECK_SERVICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_black_24dp)
                .setContentTitle(
                    App.instance.getString(R.string.start_tor_message)
                )
                .setProgress(0, 0, true)
                .setContentTitle(lastLog)
                .setOngoing(true)
        mNotificationManager.notify(START_TOR_WORKER_NOTIFICATION, notificationBuilder.build())
    }


    companion object {

        private const val BOOKS_CHANNEL_ID = "books"
        private const val SUBSCRIBES_CHANNEL_ID = "subscribes"
        private const val MISC_CHANNEL_ID = "misc"
        private const val SUBSCRIBE_CHECK_SERVICE_CHANNEL_ID = "subscribes check"
        private const val BOOK_DOWNLOADS_CHANNEL_ID = "book downloads"
        private const val FOREGROUND_CHANNEL_ID = "foreground"
        private const val START_SHARING_REQUEST_CODE = 2
        private const val START_OPEN_REQUEST_CODE = 3
        private const val START_APP_CODE = 4
        private const val CANCEL_CODE = 6
        private const val PAUSE_CODE = 7
        private const val OPEN_SCHEDULE_CODE = 8
        private const val SUBSCRIBE_NOTIFICATION = 2
        const val DOWNLOAD_PROGRESS_NOTIFICATION = 5
        private const val TOR_LOAD_ERROR_NOTIFICATION = 6
        const val DOWNLOAD_PAUSED_NOTIFICATION = 7
        const val RESUME_DOWNLOAD_NOTIFICATION = 18
        private const val BOOKS_SUCCESS_NOTIFICATION = 8
        private const val MASS_DOWNLOAD_PAUSED_NOTIFICATION = 9
        const val CHECK_SUBSCRIBES_WORKER_NOTIFICATION = 10
        const val START_TOR_WORKER_NOTIFICATION = 19
        const val FLIBUSTA_IS_BACK_NOTIFICATION = 12
        private const val RESTART_TOR_CODE = 11
        private const val MISC_CODE = 11
        private const val BOOK_DOWNLOAD_PROGRESS = 11
        private const val DOWNLOADED_BOOKS_GROUP = "downloaded books"
        private const val ERROR_DOWNLOAD_BOOKS_GROUP = "error download books"
        private const val DONATE_REQUEST_CODE = 4
        private const val BOOK_DOWNLOAD_PROGRESS_CHANNEL_ID = "book download progress"
        private const val MIRROR_DOWNLOAD_USING_NOTIFICATION = 13
        const val CHECK_AVAILABILITY_NOTIFICATION = 15
        const val IS_TEST_VERSION_NOTIFICATION = 16
        const val RESUME_DOWNLOAD_CODE = 17
        private const val SEND_LOG_CODE = 5
        private const val SEND_LOG_TO_MAIL_CODE = 18
        private const val CLEAR_DOWNLOAD_QUEUE_CODE = 18

        @JvmStatic
        var instance: NotificationHandler = NotificationHandler(App.instance)
            private set
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // создам канал уведомлений о скачанных книгах
            var nc = NotificationChannel(
                BOOKS_CHANNEL_ID,
                context.getString(R.string.books_loaded_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nc.description = context.getString(R.string.shifts_reminder)
            nc.enableLights(true)
            nc.lightColor = Color.RED
            nc.enableVibration(true)
            mNotificationManager.createNotificationChannel(nc)
            // создам канал уведомлений о скачанных книгах
            var nc1 = NotificationChannel(
                SUBSCRIBES_CHANNEL_ID,
                context.getString(R.string.subscribes_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nc1.description = context.getString(R.string.subscribe_description)
            nc1.enableLights(true)
            nc1.lightColor = Color.BLUE
            nc1.enableVibration(true)
            mNotificationManager.createNotificationChannel(nc1)
            // создам канал различных уведомлений
            nc1 = NotificationChannel(
                MISC_CHANNEL_ID,
                context.getString(R.string.misc_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nc1.description = context.getString(R.string.misc_description)
            nc1.enableLights(true)
            nc1.lightColor = Color.BLUE
            nc1.enableVibration(true)
            mNotificationManager.createNotificationChannel(nc1)
            // создам канал уведомления о проверке подписок
            nc = NotificationChannel(
                SUBSCRIBE_CHECK_SERVICE_CHANNEL_ID,
                context.getString(R.string.subscribes_check_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nc.enableVibration(false)
            nc.enableLights(false)
            nc.setSound(null, null)
            nc.description = context.getString(R.string.subscribes_check_channel_description)
            mNotificationManager.createNotificationChannel(nc)
            // создам канал уведомления со скачиванием книг
            nc = NotificationChannel(
                BOOK_DOWNLOADS_CHANNEL_ID,
                context.getString(R.string.books_download_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            nc.enableVibration(false)
            nc.enableLights(false)
            nc.setSound(null, null)
            nc.description = context.getString(R.string.books_download_channel_description)
            mNotificationManager.createNotificationChannel(nc)
            // создам канал уведомления о прогрессе скачивания книги
            nc = NotificationChannel(
                BOOK_DOWNLOAD_PROGRESS_CHANNEL_ID,
                context.getString(R.string.books_download_progress_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nc.enableVibration(false)
            nc.enableLights(false)
            nc.setSound(null, null)
            nc.description =
                context.getString(R.string.books_download_progress_channel_description)
            mNotificationManager.createNotificationChannel(nc)
            // создам канал фоновых уведомлений
            nc = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                context.getString(R.string.foreground_channel_description),
                NotificationManager.IMPORTANCE_MIN
            )
            nc.description = context.getString(R.string.foreground_channel_description)
            nc.enableLights(false)
            nc.enableVibration(false)
            mNotificationManager.createNotificationChannel(nc)
        }
    }
}