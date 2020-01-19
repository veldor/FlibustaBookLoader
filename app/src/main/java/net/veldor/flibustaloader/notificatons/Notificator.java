package net.veldor.flibustaloader.notificatons;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import net.veldor.flibustaloader.MainActivity;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.SubscriptionsActivity;
import net.veldor.flibustaloader.receivers.BookLoadedReceiver;
import net.veldor.flibustaloader.receivers.BookActionReceiver;

public class Notificator {
    private static final String SHIFTS_CHANNEL_ID = "books";
    public static final int BOOK_LOADED_NOTIFICATION = 1;
    public static final int SUBSCRIBE_NOTIFICATION = 2;
    private static final int START_SHARING_REQUEST_CODE = 2;
    private static final int START_OPEN_REQUEST_CODE = 3;
    private static final int START_APP_CODE = 4;
    private static final String SUBSCRIBES_CHANNEL_ID = "subscribes";
    private final Context mContext;
    private final NotificationManager mNotificationManager;

    public Notificator(Context context){
        mContext = context;
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // создам канал уведомлений о скачанных книгах
            NotificationChannel nc = new NotificationChannel(SHIFTS_CHANNEL_ID, mContext.getString(R.string.books_loaded_channel), NotificationManager.IMPORTANCE_DEFAULT);
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
        }
    }

    public void sendLoadedBookNotification(String name, String type){

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

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, SHIFTS_CHANNEL_ID)
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
}
