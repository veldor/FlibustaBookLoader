package net.veldor.flibustaloader.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.veldor.flibustaloader.utils.BookOpener;
import net.veldor.flibustaloader.utils.BookSharer;

public class BookActionReceiver extends BroadcastReceiver {
    public static final String EXTRA_NOTIFICATION_ID = "notification id";

    @Override
    public void onReceive(Context context, Intent intent) {

        // закрою меню уведомлений
        Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeIntent);

        // закрою уведомление, отправившее интент
        int intentId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
        if(intentId > 0){
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null)
                notificationManager.cancel(intentId);
        }

        String actionType = intent.getStringExtra(BookLoadedReceiver.EXTRA_ACTION_TYPE);
        String name = intent.getStringExtra(BookLoadedReceiver.EXTRA_BOOK_NAME);
        String type = intent.getStringExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE);
        String authorFolder = intent.getStringExtra(BookLoadedReceiver.EXTRA_AUTHOR_FOLDER);
        String sequenceFolder = intent.getStringExtra(BookLoadedReceiver.EXTRA_SEQUENCE_FOLDER);
        String reservedSequenceFolder = intent.getStringExtra(BookLoadedReceiver.EXTRA_RESERVED_SEQUENCE_FOLDER);
        if (actionType != null && actionType.equals(BookLoadedReceiver.ACTION_TYPE_SHARE))
            BookSharer.shareBook(name, type, authorFolder, sequenceFolder, reservedSequenceFolder);
        else if (actionType != null && actionType.equals(BookLoadedReceiver.ACTION_TYPE_OPEN))
            BookOpener.openBook(name, type, authorFolder, sequenceFolder, reservedSequenceFolder);
    }
}
