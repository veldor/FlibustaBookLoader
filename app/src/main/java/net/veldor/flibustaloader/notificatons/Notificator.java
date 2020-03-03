package net.veldor.flibustaloader.notificatons;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MainActivity;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.SubscriptionsActivity;
import net.veldor.flibustaloader.receivers.BookLoadedReceiver;
import net.veldor.flibustaloader.receivers.BookActionReceiver;
import net.veldor.flibustaloader.receivers.MiscActionsReceiver;
import net.veldor.flibustaloader.ui.ActivityBookDownloadSchedule;

import static net.veldor.flibustaloader.receivers.MiscActionsReceiver.EXTRA_ACTION_TYPE;

public class Notificator {
    private static final String BOOKS_CHANNEL_ID = "books";
    private static final String SUBSCRIBES_CHANNEL_ID = "subscribes";
    private static final String MISC_CHANNEL_ID = "misc";
    public static final int BOOK_LOADED_NOTIFICATION = 1;
    private static final int SUBSCRIBE_NOTIFICATION = 2;
    private static final int BACKUP_COMPLETED_NOTIFICATION = 3;
    private static final int START_SHARING_REQUEST_CODE = 2;
    private static final int START_OPEN_REQUEST_CODE = 3;
    private static final int START_APP_CODE = 4;
    public static final int DOWNLOAD_PROGRESS_NOTIFICATION = 5;
    private static final int CANCEL_CODE = 6;
    private static final int PAUSE_CODE = 7;
    private final Context mContext;
    public final NotificationManager mNotificationManager;
    private Notification mMassBookLoadingNotification;
    public NotificationCompat.Builder mDownloadScheduleBuilder;

    public Notificator(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
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
            }
        }
    }

    public void sendLoadedBookNotification(String name, String type) {

        // создам интент для функции отправки файла
        Intent shareIntent = new Intent(mContext, BookActionReceiver.class);
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_ACTION_TYPE, BookLoadedReceiver.ACTION_TYPE_SHARE);
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, type);
        shareIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, name);
        PendingIntent sharePendingIntent = PendingIntent.getBroadcast(mContext, START_SHARING_REQUEST_CODE, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // создам интент для функции открытия файла

        Intent openIntent = new Intent(mContext, BookActionReceiver.class);
        openIntent.putExtra(BookLoadedReceiver.EXTRA_ACTION_TYPE, BookLoadedReceiver.ACTION_TYPE_OPEN);
        openIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, type);
        openIntent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, name);
        PendingIntent openPendingIntent = PendingIntent.getBroadcast(mContext, START_OPEN_REQUEST_CODE, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent openMainIntent = new Intent(mContext, MainActivity.class);
        PendingIntent startMainPending = PendingIntent.getActivity(mContext, START_APP_CODE, openMainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, BOOKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_black_24dp)
                .setContentTitle("Загружена книга")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(name + " :успешно загружено"))
                .setContentIntent(startMainPending)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_share_white_24dp, "Отправить", sharePendingIntent)
                .addAction(R.drawable.ic_open_black_24dp, "Открыть", openPendingIntent);
        Notification notification = notificationBuilder.build();
        mNotificationManager.notify(BOOK_LOADED_NOTIFICATION, notification);
    }

    public void sendFoundSubscribesNotification() {

        Intent openSubscriptionsIntent = new Intent(mContext, SubscriptionsActivity.class);
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

    public void sendBackupSuccessNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, MISC_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_backup_black_24dp)
                .setContentTitle("Настройки сохранены")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Резервная копия настроек сохранена в папку " + App.BACKUP_DIR_NAME))
                .setAutoCancel(true);
        Notification notification = notificationBuilder.build();
        mNotificationManager.notify(BACKUP_COMPLETED_NOTIFICATION, notification);
    }

    public void createMassBookLoadNotification() {
        // при нажатии на уведомление- открою экран ожидания очереди
        Intent openWindowIntent = new Intent(mContext, ActivityBookDownloadSchedule.class);
        PendingIntent showWindowPending = PendingIntent.getActivity(mContext, START_APP_CODE, openWindowIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // интент отмены скачивания
        Intent cancelIntent = new Intent(mContext, MiscActionsReceiver.class);
        cancelIntent.putExtra(EXTRA_ACTION_TYPE, MiscActionsReceiver.ACTION_CANCEL_MASS_DOWNLOAD);
        PendingIntent cancelMassDownloadPendingIntent = PendingIntent.getBroadcast(mContext, CANCEL_CODE, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // интент паузы скачивания
        Intent pauseIntent = new Intent(mContext, MiscActionsReceiver.class);
        pauseIntent.putExtra(EXTRA_ACTION_TYPE, MiscActionsReceiver.ACTION_PAUSE_MASS_DOWNLOAD);
        PendingIntent pauseMassDownloadPendingIntent = PendingIntent.getBroadcast(mContext, PAUSE_CODE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mDownloadScheduleBuilder = new NotificationCompat.Builder(mContext, BOOKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                .setContentTitle("Скачивание книг")
                .setOngoing(true)
                .addAction(R.drawable.ic_list_white_24dp, "Очередь", showWindowPending)
                .addAction(R.drawable.fp_ic_action_cancel, "Отмена", cancelMassDownloadPendingIntent)
                .addAction(R.drawable.ic_pause_white_24dp, "Пауза", pauseMassDownloadPendingIntent)
                .setContentIntent(showWindowPending)
                .setContentText("Подготовка скачивания")
                .setAutoCancel(false);
        mMassBookLoadingNotification = mDownloadScheduleBuilder.build();
        mNotificationManager.notify(DOWNLOAD_PROGRESS_NOTIFICATION, mMassBookLoadingNotification);
    }

    public void cancelBookLoadNotification() {
        mNotificationManager.cancel(DOWNLOAD_PROGRESS_NOTIFICATION);
    }

    public void showBooksLoadedNotification() {
        NotificationCompat.Builder downloadCompleteBuilder = new NotificationCompat.Builder(mContext, BOOKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                .setContentTitle("Скачивание книг")
                .setContentText("Все книги успешно скачаны!");
        mNotificationManager.notify(DOWNLOAD_PROGRESS_NOTIFICATION, downloadCompleteBuilder.build());
    }

    public void createMassDownloadStoppedNotification() {
        NotificationCompat.Builder downloadStoppedBuilder = new NotificationCompat.Builder(mContext, BOOKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                .setContentTitle("Скачивание книг")
                .setContentText("Скачивание книг остановлено!");
        mNotificationManager.notify(DOWNLOAD_PROGRESS_NOTIFICATION, downloadStoppedBuilder.build());
    }

    public void createMassDownloadPausedNotification() {
        // интент возобновления скачивания
        Intent pauseIntent = new Intent(mContext, MiscActionsReceiver.class);
        pauseIntent.putExtra(EXTRA_ACTION_TYPE, MiscActionsReceiver.ACTION_RESUME_MASS_DOWNLOAD);

        PendingIntent pauseMassDownloadPendingIntent = PendingIntent.getBroadcast(mContext, START_APP_CODE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder downloadCompleteBuilder = new NotificationCompat.Builder(mContext, BOOKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pause_white_24dp)
                .setContentTitle("Скачивание книг")
                .addAction(R.drawable.ic_play_arrow_white_24dp, "Возобновить", pauseMassDownloadPendingIntent)
                .setContentText("Скачивание приостановлено!");
        mNotificationManager.notify(DOWNLOAD_PROGRESS_NOTIFICATION, downloadCompleteBuilder.build());
    }
}
