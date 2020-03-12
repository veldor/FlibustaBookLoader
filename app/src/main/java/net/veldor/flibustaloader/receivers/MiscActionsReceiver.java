package net.veldor.flibustaloader.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.work.WorkManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.workers.DownloadBooksWorker;

import static net.veldor.flibustaloader.notificatons.Notificator.DOWNLOAD_PAUSED_NOTIFICATION;
import static net.veldor.flibustaloader.view_models.MainViewModel.MULTIPLY_DOWNLOAD;

public class MiscActionsReceiver extends BroadcastReceiver {
    public static final String EXTRA_ACTION_TYPE = "action type";
    public static final String ACTION_CANCEL_MASS_DOWNLOAD = "cancel download";
    public static final String ACTION_PAUSE_MASS_DOWNLOAD = "pause download";
    public static final String  ACTION_RESUME_MASS_DOWNLOAD = "resume download";
    public static final String ACTION_REPEAT_DOWNLOAD = "repeat download";
    public static final String ACTION_SKIP_BOOK = "skip book";
    public static final String ACTION_RESTART_TOR = "restart tor";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra(EXTRA_ACTION_TYPE);
        switch (action) {
            case ACTION_CANCEL_MASS_DOWNLOAD:
                DownloadBooksWorker.dropDownloadsQueue();
                WorkManager.getInstance(App.getInstance()).cancelAllWorkByTag(MULTIPLY_DOWNLOAD);
                // отменяю работу и очищу очередь скачивания
                App.getInstance().getNotificator().cancelBookLoadNotification();
                Toast.makeText(App.getInstance(), "Скачивание книг отменено и очередь скачивания очищена!", Toast.LENGTH_LONG).show();
                break;
            case ACTION_PAUSE_MASS_DOWNLOAD:
                Log.d("surprise", "MiscActionsReceiver onReceive: pause");
                WorkManager.getInstance(App.getInstance()).cancelAllWorkByTag(MULTIPLY_DOWNLOAD);
                Toast.makeText(App.getInstance(), "Скачивание книг приостановлено!", Toast.LENGTH_LONG).show();
                // покажу уведомление о приостановленной загрузке
                App.getInstance().getNotificator().createMassDownloadPausedNotification();
                break;
            case ACTION_SKIP_BOOK:
                Log.d("surprise", "MiscActionsReceiver onReceive: skip first book");
                // пропущу первую книгу в очереди и продолжу скачивание
                DownloadBooksWorker.skipFirstBook();
            case ACTION_RESUME_MASS_DOWNLOAD:
                Log.d("surprise", "MiscActionsReceiver onReceive: resume mass download");
                App.getInstance().getNotificator().mNotificationManager.cancel(DOWNLOAD_PAUSED_NOTIFICATION);
            case ACTION_REPEAT_DOWNLOAD:
                // возобновлю скачивание
                App.getInstance().initializeDownload();
                break;
        }
    }
}
