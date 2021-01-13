package net.veldor.flibustaloader.workers;

import android.app.Notification;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.http.TorWebClient;
import net.veldor.flibustaloader.notificatons.Notificator;
import net.veldor.flibustaloader.utils.MyPreferences;

public class PeriodicCheckFlibustaAvailabilityWorker extends Worker {


    public static final String ACTION = "periodic check availability";

    public PeriodicCheckFlibustaAvailabilityWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if(MyPreferences.getInstance().isCheckAvailability() && !App.getInstance().isExternalVpn()){
            Log.d("surprise", "PeriodicCheckFlibustaAvailabilityWorker doWork 32: START CHECK AVAIL");
            // помечу рабочего важным
            // Mark the Worker as important
            setForegroundAsync(createForegroundInfo());

            AndroidOnionProxyManager tor = App.getInstance().mLoadedTor.getValue();
            while (tor == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tor = App.getInstance().mLoadedTor.getValue();
            }
            // теперь подожду, пока TOR дозагрузится
            while (!tor.isBootstrapped()) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // готово, проверю доступность
            TorWebClient webClient = null;
            try {
                webClient = new TorWebClient();
            } catch (TorNotLoadedException e) {
                e.printStackTrace();
            }
            if (webClient == null) {
                return Result.success();
            }

            String url = "http://flibustahezeous3.onion";

            String answer = webClient.requestNoMirror(url);

            if (answer != null && answer.length() > 0) {
                App.getInstance().getNotificator().notifyFlibustaIsBack();
                WorkManager.getInstance(App.getInstance()).cancelAllWorkByTag(ACTION);
                WorkManager.getInstance(App.getInstance()).cancelUniqueWork(ACTION);
            }
        }
        else{
            WorkManager.getInstance(App.getInstance()).cancelAllWorkByTag(ACTION);
            WorkManager.getInstance(App.getInstance()).cancelUniqueWork(ACTION);
        }
        return Result.success();
    }

    @NonNull
    private ForegroundInfo createForegroundInfo() {
        // Build a notification using bytesRead and contentLength

        Notification notification = Notificator.getInstance().getCheckAvailabilityNotification();

        return new ForegroundInfo(Notificator.CHECK_AVAILABILITY_NOTIFICATION, notification);
    }
}
