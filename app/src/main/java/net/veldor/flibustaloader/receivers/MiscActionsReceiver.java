package net.veldor.flibustaloader.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.work.WorkManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.notificatons.Notificator;
import net.veldor.flibustaloader.ui.MainActivity;
import net.veldor.flibustaloader.utils.LogHandler;
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
    public static final String ACTION_SEND_LOGS = "send logs";
    public static final String ACTION_ENABLE_VPN_MODE = "enable vpn";

    @Override
    public void onReceive(Context context, Intent intent) {
        String mainAction = intent.getAction();
        String action = intent.getStringExtra(EXTRA_ACTION_TYPE);
        Log.d("surprise", "MiscActionsReceiver onReceive 33: receiver action is " + mainAction);
        Log.d("surprise", "MiscActionsReceiver onReceive 43: receiver action is " + action);
        if(action != null){
            switch (action) {
                case ACTION_CANCEL_MASS_DOWNLOAD:
                    App.getInstance().getNotificator(). hideMassDownloadInQueueMessage();
                    DownloadBooksWorker.dropDownloadsQueue();
                    WorkManager.getInstance(App.getInstance()).cancelAllWorkByTag(MULTIPLY_DOWNLOAD);
                    // отменяю работу и очищу очередь скачивания
                    App.getInstance().getNotificator().cancelBookLoadNotification();
                    Toast.makeText(App.getInstance(), "Скачивание книг отменено и очередь скачивания очищена!", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_PAUSE_MASS_DOWNLOAD:
                    App.getInstance().getNotificator(). hideMassDownloadInQueueMessage();
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
                case ACTION_RESTART_TOR:
                    App.getInstance().getNotificator(). hideMassDownloadInQueueMessage();
                    App.sTorStartTry = 0;
                    App.getInstance().startTor();
                    break;
                case ACTION_SEND_LOGS:
                    App.getInstance().getNotificator().mNotificationManager.cancel(Notificator.IS_TEST_VERSION_NOTIFICATION);
                    Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    context.sendBroadcast(it);
                    LogHandler.getInstance().sendLogs();

                case ACTION_ENABLE_VPN_MODE:
                    App.getInstance().switchExternalVpnUse();
                    Intent mStartActivity = new Intent(context, MainActivity.class);
                    int mPendingIntentId = 123456;
                    PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                    System.exit(0);

            }
        }
    }
}
