package net.veldor.flibustaloader.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import net.veldor.flibustaloader.notificatons.Notificator;

public class BookLoadedReceiver extends BroadcastReceiver {

    public static final String EXTRA_BOOK_NAME = "book name";
    public static final String EXTRA_ACTION_TYPE = "action type";
    public static final String EXTRA_BOOK_TYPE = "book mime";

    public static final String ACTION_TYPE_OPEN = "open";
    public static final String ACTION_TYPE_SHARE = "share";
    public static final String EXTRA_AUTHOR_FOLDER = "author folder";
    public static final String EXTRA_SEQUENCE_FOLDER = "sequence folder";
    public static final String EXTRA_RESERVED_SEQUENCE_FOLDER = "reserved sequence folder";


    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra(EXTRA_BOOK_NAME);
        String type = intent.getStringExtra(EXTRA_BOOK_TYPE);
        Log.d("surprise", "BookLoadedReceiver onReceive " + type);
        Notificator.getInstance().sendLoadedBookNotification(name, type);
        Toast.makeText(context, name + " - загружено!", Toast.LENGTH_LONG).show();
    }
}
