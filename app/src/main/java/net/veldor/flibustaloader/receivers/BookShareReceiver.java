package net.veldor.flibustaloader.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.veldor.flibustaloader.notificatons.Notificator;
import net.veldor.flibustaloader.utils.BookSharer;

public class BookShareReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        // закрою меню уведомлений
        Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeIntent);

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notificator.BOOK_LOADED_NOTIFICATION);

        String name = intent.getStringExtra(BookLoadedReceiver.EXTRA_BOOK_NAME);
        String type = intent.getStringExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE);
        BookSharer.shareBook(name, type);
    }
}
