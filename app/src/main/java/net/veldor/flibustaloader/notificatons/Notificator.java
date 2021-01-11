package net.veldor.flibustaloader.notificatons;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.receivers.BookActionReceiver;
import net.veldor.flibustaloader.receivers.BookLoadedReceiver;
import net.veldor.flibustaloader.receivers.MiscActionsReceiver;
import net.veldor.flibustaloader.ui.ActivityBookDownloadSchedule;
import net.veldor.flibustaloader.ui.MainActivity;
import net.veldor.flibustaloader.ui.SubscriptionsActivity;
import net.veldor.flibustaloader.utils.MimeTypes;

import java.util.Locale;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static net.veldor.flibustaloader.receivers.MiscActionsReceiver.EXTRA_ACTION_TYPE;

public class Notificator {
    private static final String BOOKS_CHANNEL_ID = "books";
    private static final String SUBSCRIBES_CHANNEL_ID = "subscribes";
    private static final String MISC_CHANNEL_ID = "misc";
    private static final String SUBSCRIBE_CHECK_SERVICE_CHANNEL_ID = "subscribes check";
    private static final String BOOK_DOWNLOADS_CHANNEL_ID = "book downloads";
    private static final int START_SHARING_REQUEST_CODE = 2;
    private static final int START_OPEN_REQUEST_CODE = 3;
    private static final int START_APP_CODE = 4;
    private static final int CANCEL_CODE = 6;
    private static final int PAUSE_CODE = 7;
    private static final int OPEN_SCHEDULE_CODE = 8;
    private static final int SUBSCRIBE_NOTIFICATION = 2;
    public static final int DOWNLOAD_PROGRESS_NOTIFICATION = 5;
    private static final int TOR_LOAD_ERROR_NOTIFICATION = 6;
    public static final int DOWNLOAD_PAUSED_NOTIFICATION = 7;
    private static final int BOOKS_SUCCESS_NOTIFICATION = 8;
    private static final int MASS_DOWNLOAD_PAUSED_NOTIFICATION = 9;
    public static final int CHECK_SUBSCRIBES_WORKER_NOTIFICATION = 10;
    public static final int FLIBUSTA_IS_BACK_NOTIFICATION = 12;
    private static final int RESTART_TOR_CODE = 11;
    private static final int MISC_CODE = 11;
    private static final int BOOK_DOWNLOAD_PROGRESS = 11;
    private static final String DOWNLOADED_BOOKS_GROUP = "downloaded books";
    private static final String ERROR_DOWNLOAD_BOOKS_GROUP = "error download books";
    private static final int DONATE_REQUEST_CODE = 4;
    private static final String BOOK_DOWNLOAD_PROGRESS_CHANNEL_ID = "book download progress";
    private static final int MIRROR_DOWNLOAD_USING_NOTIFICATION = 13;
    private static final int MIRROR_USING_NOTIFICATION = 14;
    private static Notificator instance;
    private final Context mContext;
    public final NotificationManager mNotificationManager;
    public NotificationCompat.Builder mDownloadScheduleBuilder;

    private int BookLoadedId = 100;

    private Notificator(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mNotificationManager != null) {
                // создам канал уведомлений о скачанных книгах
                NotificationChannel nc = new NotificationChannel(BOOKS_CHANNEL_ID, mContext.getString(R.string.books_loaded_channel), NotificationManager.IMPORTANCE_DEFAULT);
                nc.setDescription(mContext.getString(R.string.shifts_reminder));
                nc.enableLights(true);
                nc.setLightColor(Color.RED);
                nc.enableVibration(true);
                mNotificationManager.createNotificationChannel(nc);
                // создам канал уведомлений о скачанных книгах
                NotificationChannel nc1 = new NotificationChannel(SUBSCRIBES_CHANNEL_ID, mContext.getString(R.string.subscribes_channel), NotificationManager.IMPORTANCE_DEFAULT);
                nc1.setDescription(mContext.getString(R.string.subscribe_description));
                nc1.enableLights(true);
                nc1.setLightColor(Color.BLUE);
                nc1.enableVibration(true);
                mNotificationManager.createNotificationChannel(nc1);
                // создам канал различных уведомлений
                nc1 = new NotificationChannel(MISC_CHANNEL_ID, mContext.getString(R.string.misc_channel), NotificationManager.IMPORTANCE_DEFAULT);
                nc1.setDescription(mContext.getString(R.string.misc_description));
                nc1.enableLights(true);
                nc1.setLightColor(Color.BLUE);
                nc1.enableVibration(true);
                mNotificationManager.createNotificationChannel(nc1);
                // создам канал уведомления о проверке подписок
                nc = new NotificationChannel(SUBSCRIBE_CHECK_SERVICE_CHANNEL_ID, mContext.getString(R.string.subscribes_check_channel), NotificationManager.IMPORTANCE_DEFAULT);
                nc.enableVibration(false);
                nc.enableLights(false);
                nc.setSound(null, null);
                nc.setDescription(mContext.getString(R.string.subscribes_check_channel_description));
                mNotificationManager.createNotificationChannel(nc);
                // создам канал уведомления со скачиванием книг
                nc = new NotificationChannel(BOOK_DOWNLOADS_CHANNEL_ID, mContext.getString(R.string.books_download_channel), NotificationManager.IMPORTANCE_LOW);
                nc.enableVibration(false);
                nc.enableLights(false);
                nc.setSound(null, null);
                nc.setDescription(mContext.getString(R.string.books_download_channel_description));
                mNotificationManager.createNotificationChannel(nc);
                // создам канал уведомления о прогрессе скачивания книги
                nc = new NotificationChannel(BOOK_DOWNLOAD_PROGRESS_CHANNEL_ID, mContext.getString(R.string.books_download_progress_channel), NotificationManager.IMPORTANCE_DEFAULT);
                nc.enableVibration(false);
                nc.enableLights(false);
                nc.setSound(null, null);
                nc.setDescription(mContext.getString(R.string.books_download_progress_channel_description));
                mNotificationManager.createNotificationChannel(nc);
            }
        }
    }

    public static Notificator getInstance() {
        if (instance == null) {
            instance = new Notificator(App.getInstance());
        }
        return instance;
    }

    public void sendLoadedBookNotification(String name, String type) {

        // Добавлю группу
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext, BOOK_DOWNLOADS_CHANNEL_ID)
                        .setContentInfo("Загруженные книги")
                        .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                        .setGroup(DOWNLOADED_BOOKS_GROUP)
                        .setGroupSummary(true);

        mNotificationManager.notify(-100, mBuilder.build());

        Log.d("surprise", "Notificator sendLoadedBookNotification 118: type is " + type);

        // создам интент для функции отправки файла
        Intent shareIntent = new Intent(mContext, BookActionReceiver.class);
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_ACTION_TYPE, BookLoadedReceiver.ACTION_TYPE_SHARE);
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, type);
        shareIntent.putExtra(BookActionReceiver.EXTRA_NOTIFICATION_ID, BookLoadedId);
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, name);
        PendingIntent sharePendingIntent = PendingIntent.getBroadcast(mContext, START_SHARING_REQUEST_CODE, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // создам интент для функции открытия файла

        Intent openIntent = new Intent(mContext, BookActionReceiver.class);
        openIntent.putExtra(BookLoadedReceiver.EXTRA_ACTION_TYPE, BookLoadedReceiver.ACTION_TYPE_OPEN);
        openIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, type);
        openIntent.putExtra(BookActionReceiver.EXTRA_NOTIFICATION_ID, BookLoadedId);
        openIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, name);
        PendingIntent openPendingIntent = PendingIntent.getBroadcast(mContext, START_OPEN_REQUEST_CODE, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent openMainIntent = new Intent(mContext, MainActivity.class);
        PendingIntent startMainPending = PendingIntent.getActivity(mContext, START_APP_CODE, openMainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, BOOKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_black_24dp)
                .setContentTitle(name)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(name + " :успешно загружено"))
                .setContentIntent(startMainPending)
                .setDefaults(Notification.DEFAULT_ALL)
                .setGroup(DOWNLOADED_BOOKS_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_share_white_24dp, "Отправить", sharePendingIntent)
                .addAction(R.drawable.ic_open_black_24dp, "Открыть", openPendingIntent);
        mNotificationManager.notify(BookLoadedId, notificationBuilder.build());
        ++BookLoadedId;
    }


    public void sendBookNotFoundInCurrentFormatNotification(BooksDownloadSchedule book) {
        // Добавлю группу
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext, BOOK_DOWNLOADS_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_warning_red_24dp)
                        .setContentInfo("Загруженные книги")
                        .setContentTitle("Не удалось загрузить книги")
                        .setGroup(ERROR_DOWNLOAD_BOOKS_GROUP)
                        .setGroupSummary(true);

        mNotificationManager.notify(-101, mBuilder.build());

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, BOOKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning_red_24dp)
                .setContentTitle(mContext.getString(R.string.book_load_error_message))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(String.format(Locale.ENGLISH, mContext.getString(R.string.book_unreachable_message), book.name, MimeTypes.getDownloadMime(book.format))))
                .setDefaults(Notification.DEFAULT_ALL)
                .setGroup(ERROR_DOWNLOAD_BOOKS_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        mNotificationManager.notify(BookLoadedId, notificationBuilder.build());
        ++BookLoadedId;
    }

    public void sendFoundSubscribesNotification() {
        Intent openSubscriptionsIntent = new Intent(mContext, SubscriptionsActivity.class);
        openSubscriptionsIntent.putExtra(SubscriptionsActivity.START_FRAGMENT, SubscriptionsActivity.START_RESULTS);
        PendingIntent startMainPending = PendingIntent.getActivity(mContext, START_APP_CODE, openSubscriptionsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, SUBSCRIBES_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_black_24dp)
                .setContentTitle("Найдены новые книги")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Есть новые поступления по темам, которые вас интересуют"))
                .setContentIntent(startMainPending)
                .setAutoCancel(true);
        Notification notification = notificationBuilder.build();
        mNotificationManager.notify(SUBSCRIBE_NOTIFICATION, notification);
    }

    public Notification createMassBookLoadNotification() {
        cancelTorErrorMessage();
        // при нажатии на уведомление- открою экран ожидания очереди
        Intent openWindowIntent = new Intent(mContext, ActivityBookDownloadSchedule.class);
        openWindowIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent showWindowPending = PendingIntent.getActivity(mContext, START_APP_CODE, openWindowIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // интент отмены скачивания
        Intent cancelIntent = new Intent(mContext, MiscActionsReceiver.class);
        cancelIntent.putExtra(EXTRA_ACTION_TYPE, MiscActionsReceiver.ACTION_CANCEL_MASS_DOWNLOAD);
        PendingIntent cancelMassDownloadPendingIntent = PendingIntent.getBroadcast(mContext, CANCEL_CODE, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // интент паузы скачивания
        Intent pauseIntent = new Intent(mContext, MiscActionsReceiver.class);
        pauseIntent.putExtra(EXTRA_ACTION_TYPE, MiscActionsReceiver.ACTION_PAUSE_MASS_DOWNLOAD);
        PendingIntent pauseMassDownloadPendingIntent = PendingIntent.getBroadcast(mContext, PAUSE_CODE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mDownloadScheduleBuilder = new NotificationCompat.Builder(mContext, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                .setContentTitle("Скачивание книг")
                .setOngoing(true)
                .setContentText("Загружаю книги")
                .setProgress(0, 0, true)
                .addAction(R.drawable.ic_list_white_24dp, "Очередь", showWindowPending)
                .addAction(R.drawable.fp_ic_action_cancel, "Отмена", cancelMassDownloadPendingIntent)
                .addAction(R.drawable.ic_pause_white_24dp, "Пауза", pauseMassDownloadPendingIntent)
                .setContentIntent(showWindowPending)
                .setAutoCancel(false);
        return mDownloadScheduleBuilder.build();
    }

    public void cancelBookLoadNotification() {
        mNotificationManager.cancel(DOWNLOAD_PROGRESS_NOTIFICATION);
    }

    public void showBooksLoadedNotification(int bookDownloadsWithErrors) {
        NotificationCompat.Builder downloadCompleteBuilder = new NotificationCompat.Builder(mContext, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                .setContentTitle("Скачивание книг");
        if (bookDownloadsWithErrors == 0) {
            downloadCompleteBuilder.setContentText("Все книги успешно скачаны!");
        } else {
            downloadCompleteBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("Книги скачаны, но " + bookDownloadsWithErrors + " скачать не удалось. Они оставлены в очереди скачивания."));
        }
        mNotificationManager.notify(BOOKS_SUCCESS_NOTIFICATION, downloadCompleteBuilder.build());
    }

    public void createMassDownloadStoppedNotification() {
        cancelTorErrorMessage();
        NotificationCompat.Builder downloadStoppedBuilder = new NotificationCompat.Builder(mContext, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                .setContentTitle("Скачивание книг")
                .setContentText("Скачивание книг остановлено!");
        mNotificationManager.notify(DOWNLOAD_PROGRESS_NOTIFICATION, downloadStoppedBuilder.build());
    }

    public void createMassDownloadPausedNotification() {
        cancelTorErrorMessage();
        // интент возобновления скачивания
        Intent pauseIntent = new Intent(mContext, MiscActionsReceiver.class);
        pauseIntent.putExtra(EXTRA_ACTION_TYPE, MiscActionsReceiver.ACTION_RESUME_MASS_DOWNLOAD);
        PendingIntent pauseMassDownloadPendingIntent = PendingIntent.getBroadcast(mContext, START_APP_CODE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder downloadCompleteBuilder = new NotificationCompat.Builder(mContext, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pause_white_24dp)
                .setContentTitle("Скачивание книг")
                .addAction(R.drawable.ic_play_arrow_white_24dp, "Возобновить", pauseMassDownloadPendingIntent)
                .setContentText("Скачивание приостановлено!");
        mNotificationManager.notify(DOWNLOAD_PAUSED_NOTIFICATION, downloadCompleteBuilder.build());
    }

    public void showTorNotLoadedNotification() {
        Log.d("surprise", "Notificator showTorNotLoadedNotification: tor can't");
        // интент возобновления скачивания
        Intent restartTorIntent = new Intent(mContext, MiscActionsReceiver.class);
        restartTorIntent.putExtra(EXTRA_ACTION_TYPE, MiscActionsReceiver.ACTION_RESTART_TOR);
        PendingIntent RestartTorPendingIntent = PendingIntent.getBroadcast(mContext, RESTART_TOR_CODE, restartTorIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder downloadCompleteBuilder = new NotificationCompat.Builder(mContext, MISC_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning_red_24dp)
                .setContentTitle(mContext.getString(R.string.tor_init_error_message))
                .addAction(R.drawable.ic_play_arrow_white_24dp, mContext.getString(R.string.request_restart_tor_message), RestartTorPendingIntent)
                .setContentText(mContext.getString(R.string.download_paused_message));
        mNotificationManager.notify(TOR_LOAD_ERROR_NOTIFICATION, downloadCompleteBuilder.build());
    }

    private void cancelTorErrorMessage() {
        mNotificationManager.cancel(TOR_LOAD_ERROR_NOTIFICATION);
    }

    public void showMassDownloadInQueueMessage() {
        // интент паузы скачивания
        Intent pauseIntent = new Intent(mContext, MiscActionsReceiver.class);
        pauseIntent.putExtra(EXTRA_ACTION_TYPE, MiscActionsReceiver.ACTION_PAUSE_MASS_DOWNLOAD);
        PendingIntent pauseMassDownloadPendingIntent = PendingIntent.getBroadcast(mContext, PAUSE_CODE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // интент отмены скачивания
        Intent cancelIntent = new Intent(mContext, MiscActionsReceiver.class);
        cancelIntent.putExtra(EXTRA_ACTION_TYPE, MiscActionsReceiver.ACTION_CANCEL_MASS_DOWNLOAD);
        PendingIntent cancelMassDownloadPendingIntent = PendingIntent.getBroadcast(mContext, CANCEL_CODE, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // отменю уведомление об ошибке TOR
        cancelTorErrorMessage();
        // интент возобновления скачивания
        Intent restartTorIntent = new Intent(mContext, MiscActionsReceiver.class);
        restartTorIntent.putExtra(EXTRA_ACTION_TYPE, MiscActionsReceiver.ACTION_RESTART_TOR);
        PendingIntent RestartTorPendingIntent = PendingIntent.getBroadcast(mContext, RESTART_TOR_CODE, restartTorIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // добавлю интент для отображения экрана очереди скачивания
        Intent openScheduleIntent = new Intent(mContext, ActivityBookDownloadSchedule.class);
        openScheduleIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openSchedulePendingIntent = PendingIntent.getActivity(mContext, OPEN_SCHEDULE_CODE, openScheduleIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // покажу уведомление об ожидании подключения
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pause_white_24dp)
                .setContentIntent(openSchedulePendingIntent)
                .setContentTitle(mContext.getString(R.string.download_paused_message))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mContext.getString(R.string.waiting_internet_message)))
                .setProgress(0, 0, true)
                .setOngoing(true)
                .addAction(R.drawable.ic_play_arrow_white_24dp, mContext.getString(R.string.restart_tor_from_notification_message), RestartTorPendingIntent)
                .addAction(R.drawable.fp_ic_action_cancel, mContext.getString(R.string.cancel_download_from_notification_message), cancelMassDownloadPendingIntent)
                .addAction(R.drawable.ic_pause_white_24dp, mContext.getString(R.string.later_message), pauseMassDownloadPendingIntent)
                .setAutoCancel(false);
        mNotificationManager.notify(MASS_DOWNLOAD_PAUSED_NOTIFICATION, notificationBuilder.build());
    }

    public void hideMassDownloadInQueueMessage() {
        mNotificationManager.cancel(MASS_DOWNLOAD_PAUSED_NOTIFICATION);
    }

    public Notification getCheckSubscribesNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, SUBSCRIBE_CHECK_SERVICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_black_24dp)
                .setContentTitle(App.getInstance().getString(R.string.check_flibusta_subscriptions_message))
                .setProgress(0, 0, true)
                .setOngoing(true);
        return notificationBuilder.build();
    }

    public void sendNotFoundSubscribesNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, SUBSCRIBES_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_black_24dp)
                .setContentTitle("Новых книг не найдено")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Ничего интересного в этот раз не нашлось"))
                .setAutoCancel(true);
        Notification notification = notificationBuilder.build();
        mNotificationManager.notify(SUBSCRIBE_NOTIFICATION, notification);
    }

    public void updateDownloadProgress(int mBooksCount, int currentDownload) {
        // при нажатии на уведомление- открою экран ожидания очереди
        Intent openWindowIntent = new Intent(mContext, ActivityBookDownloadSchedule.class);
        openWindowIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent showWindowPending = PendingIntent.getActivity(mContext, START_APP_CODE, openWindowIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // интент отмены скачивания
        Intent cancelIntent = new Intent(mContext, MiscActionsReceiver.class);
        cancelIntent.putExtra(EXTRA_ACTION_TYPE, MiscActionsReceiver.ACTION_CANCEL_MASS_DOWNLOAD);
        PendingIntent cancelMassDownloadPendingIntent = PendingIntent.getBroadcast(mContext, CANCEL_CODE, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // интент паузы скачивания
        Intent pauseIntent = new Intent(mContext, MiscActionsReceiver.class);
        pauseIntent.putExtra(EXTRA_ACTION_TYPE, MiscActionsReceiver.ACTION_PAUSE_MASS_DOWNLOAD);
        PendingIntent pauseMassDownloadPendingIntent = PendingIntent.getBroadcast(mContext, PAUSE_CODE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mDownloadScheduleBuilder = new NotificationCompat.Builder(mContext, BOOK_DOWNLOADS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                .setContentTitle("Скачивание книг")
                .setOngoing(true)
                .setContentText("Загружаю книгу " + currentDownload + " из " + mBooksCount)
                .setProgress(mBooksCount, currentDownload, true)
                .addAction(R.drawable.ic_list_white_24dp, "Очередь", showWindowPending)
                .addAction(R.drawable.fp_ic_action_cancel, "Отмена", cancelMassDownloadPendingIntent)
                .addAction(R.drawable.ic_pause_white_24dp, "Пауза", pauseMassDownloadPendingIntent)
                .setContentIntent(showWindowPending)
                .setAutoCancel(false);
        mDownloadScheduleBuilder.build();
        mNotificationManager.notify(DOWNLOAD_PROGRESS_NOTIFICATION, mDownloadScheduleBuilder.build());
    }

    public void sendLoadedBookNotification(BooksDownloadSchedule queuedElement) {

        // Добавлю группу
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext, BOOK_DOWNLOADS_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_book_black_24dp)
                        .setContentInfo("Загруженные книги")
                        .setGroup(DOWNLOADED_BOOKS_GROUP)
                        .setGroupSummary(true);

        mNotificationManager.notify(-100, mBuilder.build());

        Log.d("surprise", "Notificator sendLoadedBookNotification 118: type is " + queuedElement.format);

        // создам интент для функции отправки файла
        Intent shareIntent = new Intent(mContext, BookActionReceiver.class);
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_ACTION_TYPE, BookLoadedReceiver.ACTION_TYPE_SHARE);
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, queuedElement.format);
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_AUTHOR_FOLDER, queuedElement.authorDirName);
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_SEQUENCE_FOLDER, queuedElement.sequenceDirName);
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_RESERVED_SEQUENCE_FOLDER, queuedElement.reservedSequenceName);
        shareIntent.putExtra(BookActionReceiver.EXTRA_NOTIFICATION_ID, BookLoadedId);
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, queuedElement.name);
        PendingIntent sharePendingIntent = PendingIntent.getBroadcast(mContext, START_SHARING_REQUEST_CODE, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // создам интент для функции открытия файла

        Intent openIntent = new Intent(mContext, BookActionReceiver.class);
        openIntent.putExtra(BookLoadedReceiver.EXTRA_ACTION_TYPE, BookLoadedReceiver.ACTION_TYPE_OPEN);
        openIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, queuedElement.format);
        openIntent.putExtra(BookActionReceiver.EXTRA_NOTIFICATION_ID, BookLoadedId);
        openIntent.putExtra(BookLoadedReceiver.EXTRA_AUTHOR_FOLDER, queuedElement.authorDirName);
        openIntent.putExtra(BookLoadedReceiver.EXTRA_SEQUENCE_FOLDER, queuedElement.sequenceDirName);
        openIntent.putExtra(BookLoadedReceiver.EXTRA_RESERVED_SEQUENCE_FOLDER, queuedElement.reservedSequenceName);
        openIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, queuedElement.name);
        PendingIntent openPendingIntent = PendingIntent.getBroadcast(mContext, START_OPEN_REQUEST_CODE, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent openMainIntent = new Intent(mContext, MainActivity.class);
        PendingIntent startMainPending = PendingIntent.getActivity(mContext, START_APP_CODE, openMainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, BOOKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_black_24dp)
                .setContentTitle(queuedElement.name)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(queuedElement.name + " :успешно загружено"))
                .setContentIntent(startMainPending)
                .setDefaults(Notification.DEFAULT_ALL)
                .setGroup(DOWNLOADED_BOOKS_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_share_white_24dp, "Отправить", sharePendingIntent)
                .addAction(R.drawable.ic_open_black_24dp, "Открыть", openPendingIntent);
        mNotificationManager.notify(BookLoadedId, notificationBuilder.build());
        ++BookLoadedId;
    }

    public void begDonation() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://money.yandex.ru/to/41001269882689"));
        PendingIntent openPendingIntent = PendingIntent.getActivity(mContext, DONATE_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, MISC_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_attach_money_24)
                .setContentTitle("Поддержать разработку")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Если вам понравилось приложение- можете кинуть мне денег на кофе. Если у вас есть предложения/пожелания- можете написать их в комментарии к переводу :)"))
                .setContentIntent(openPendingIntent)
                .addAction(R.drawable.ic_baseline_attach_money_24, "Пожертвовать", openPendingIntent)
                .setAutoCancel(true);
        Notification notification = notificationBuilder.build();
        mNotificationManager.notify(MISC_CODE, notification);
    }

    public void createBookLoadingProgressNotification(int contentLength, int loaded, String name) {
        // пересчитаю байты в килобайты
        double total = (double) contentLength / 1024;
        double nowLoaded = (double) loaded / 1024;
        double percentDone = 0.;
        if (loaded > 0) {
            percentDone = loaded * 100 / total / 1000;
        }
        Notification notification = new NotificationCompat.Builder(mContext, BOOK_DOWNLOAD_PROGRESS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                .setContentTitle("Качаю " + name)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(String.format(Locale.ENGLISH, App.getInstance().getString(R.string.loaded_message), nowLoaded, total, percentDone)))
                .setOngoing(true)
                .setProgress(contentLength, loaded, false)
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(false).build();
        mNotificationManager.notify(BOOK_DOWNLOAD_PROGRESS, notification);
    }

    public void cancelBookLoadingProgressNotification() {
        mNotificationManager.cancel(BOOK_DOWNLOAD_PROGRESS);
    }

    public void notifyFlibustaIsBack() {
        Intent contentIntent = new Intent(App.getInstance(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(App.getInstance(), 0,
                contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(App.getInstance(), MISC_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_baseline_bookmark_24)
                        .setContentTitle("Флибуста вернулась!")
                        .setContentText("Нажмите сюда, чтобы начать сёрфить")
                        .setColor(Color.GREEN)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        Notification incomingCallNotification = notificationBuilder.build();
        mNotificationManager.notify(FLIBUSTA_IS_BACK_NOTIFICATION, incomingCallNotification);
    }

    public void notifyDownloadFromMirror() {
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(App.getInstance(), MISC_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_baseline_bookmark_24)
                        .setContentTitle("Используется альтернативное зеркало!")
                        .setStyle(new NotificationCompat.BigTextStyle().bigText("Сейчас используется зеркало, на котором доступ к большинству книг ограничен. Могут возникнуть проблемы со скачиванием!"))
                        .setColor(Color.RED)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification incomingCallNotification = notificationBuilder.build();
        mNotificationManager.notify(MIRROR_DOWNLOAD_USING_NOTIFICATION, incomingCallNotification);
    }

    public void notifyUseMirror() {
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(App.getInstance(), MISC_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_baseline_bookmark_24)
                        .setContentTitle("Используется альтернативное зеркало!")
                        .setStyle(new NotificationCompat.BigTextStyle().bigText("Нет доступа к основному зеркалу. Вы можете просматривать каталог, но со скачиванием книг скорее всего будут проблемы. Лучше зайдите попозже."))
                        .setColor(Color.RED)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification incomingCallNotification = notificationBuilder.build();
        mNotificationManager.notify(MIRROR_USING_NOTIFICATION, incomingCallNotification);
    }
}
