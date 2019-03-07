package net.veldor.flibustaloader.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import net.veldor.flibustaloader.notificatons.Notificator;

public class BookLoadedReceiver extends BroadcastReceiver {

    public static final String EXTRA_BOOK_NAME = "book name";
    public static final String EXTRA_ACTION_TYPE = "action type";
    public static final String EXTRA_BOOK_TYPE = "book mime";

    public static final String ACTION_TYPE_OPEN = "open";
    public static final String ACTION_TYPE_SHARE = "share";


    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra(EXTRA_BOOK_NAME);
        String type = intent.getStringExtra(EXTRA_BOOK_TYPE);
        new Notificator(context).sendLoadedBookNotification(name, type);
        Toast.makeText(context, name + " - загружено!", Toast.LENGTH_LONG).show();
    }
}
