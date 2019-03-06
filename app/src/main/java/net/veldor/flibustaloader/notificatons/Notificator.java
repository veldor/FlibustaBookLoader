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

import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.receivers.BookLoadedReceiver;
import net.veldor.flibustaloader.receivers.BookShareReceiver;

public class Notificator {
    private static final String SHIFTS_CHANNEL_ID = "books";
    public static final int BOOK_LOADED_NOTIFICATION = 1;
    private static final int START_SHARING_REQUEST_CODE = 2;
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
        }
    }

    public void sendLoadedBookNotification(String name, String type){

        // создам интент для функции отправки файла
        Intent intent = new Intent(mContext, BookShareReceiver.class);
        intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, type);
        intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, name);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, START_SHARING_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, SHIFTS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_black_24dp)
                .setContentTitle("Загружена книга")
                .setContentText(name + " :успешно загружено")
                .setAutoCancel(true)
                .addAction(R.drawable.ic_share_white_24dp, "Отправить", pendingIntent);
        Notification notification = notificationBuilder.build();
        mNotificationManager.notify(BOOK_LOADED_NOTIFICATION, notification);
    }
}
