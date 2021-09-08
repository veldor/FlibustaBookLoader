package net.veldor.flibustaloader.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.veldor.flibustaloader.utils.BookOpener
import net.veldor.flibustaloader.utils.BookSharer

class BookActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        // закрою меню уведомлений
        val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(closeIntent)

        // закрою уведомление, отправившее интент
        val intentId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        if (intentId > 0) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(intentId)
        }
        val actionType = intent.getStringExtra(BookLoadedReceiver.EXTRA_ACTION_TYPE)
        val name = intent.getStringExtra(BookLoadedReceiver.EXTRA_BOOK_NAME)
        val type = intent.getStringExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE)
        val authorFolder = intent.getStringExtra(BookLoadedReceiver.EXTRA_AUTHOR_FOLDER)
        val sequenceFolder =
            intent.getStringExtra(BookLoadedReceiver.EXTRA_SEQUENCE_FOLDER)
        val reservedSequenceFolder =
            intent.getStringExtra(BookLoadedReceiver.EXTRA_RESERVED_SEQUENCE_FOLDER)
        if (actionType != null && actionType == BookLoadedReceiver.ACTION_TYPE_SHARE) BookSharer.shareBook(
            name,
            type,
            authorFolder,
            sequenceFolder,
            reservedSequenceFolder
        ) else if (actionType != null && actionType == BookLoadedReceiver.ACTION_TYPE_OPEN) BookOpener.openBook(
            name,
            type,
            authorFolder,
            sequenceFolder,
            reservedSequenceFolder
        )
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification id"
    }
}