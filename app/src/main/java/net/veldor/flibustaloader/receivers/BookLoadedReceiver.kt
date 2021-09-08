package net.veldor.flibustaloader.receivers

import android.content.Intent
import android.widget.Toast
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import net.veldor.flibustaloader.notificatons.NotificationHandler

class BookLoadedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_BOOK_NAME)
        val type = intent.getStringExtra(EXTRA_BOOK_TYPE)
        Log.d("surprise", "BookLoadedReceiver onReceive $type")
        NotificationHandler.instance.sendLoadedBookNotification(name, type)
        Toast.makeText(context, "$name - загружено!", Toast.LENGTH_LONG).show()
    }

    companion object {
        const val EXTRA_BOOK_NAME = "book name"
        const val EXTRA_ACTION_TYPE = "action type"
        const val EXTRA_BOOK_TYPE = "book mime"
        const val ACTION_TYPE_OPEN = "open"
        const val ACTION_TYPE_SHARE = "share"
        const val EXTRA_AUTHOR_FOLDER = "author folder"
        const val EXTRA_SEQUENCE_FOLDER = "sequence folder"
        const val EXTRA_RESERVED_SEQUENCE_FOLDER = "reserved sequence folder"
    }
}