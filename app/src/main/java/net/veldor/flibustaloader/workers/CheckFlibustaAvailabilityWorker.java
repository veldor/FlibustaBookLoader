package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.http.GlobalWebClient;
import net.veldor.flibustaloader.notificatons.Notificator;
import net.veldor.flibustaloader.utils.MyPreferences;

import java.io.IOException;

public class CheckFlibustaAvailabilityWorker extends Worker {


    public static final String ACTION = "check availability";
    public static final String AVAILABILITY_STATE = "availability state";

    public CheckFlibustaAvailabilityWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data.Builder outputDataBuilder = new Data.Builder();

        // проверю доступность интернета в принципе
        String url;
        // теперь проверю подключение к основному серверу
        if (MyPreferences.getInstance().isCustomMirror()) {
            url = MyPreferences.getInstance().getCustomMirror();
            if (inspect(url)) {
                outputDataBuilder.putBoolean(AVAILABILITY_STATE, true);
            }
        } else if (App.getInstance().isExternalVpn()) {
            url = "http://flibusta.is/";
            if (inspect(url)) {
                outputDataBuilder.putBoolean(AVAILABILITY_STATE, true);
            }
        } else {
            url = "http://flibustahezeous3.onion/";
            if (inspect(url)) {
                outputDataBuilder.putBoolean(AVAILABILITY_STATE, true);
            } else {
                // запущу периодическую проверку доступности зеркала
                App.getInstance().startCheckWorker();
                // попробую использовать резервное подключение
                url = "https://flibusta.appspot.com/";
                if (inspect(url)) {
                    App.getInstance().useMirror = true;
                    Notificator.getInstance().notifyUseMirror();
                    outputDataBuilder.putBoolean(AVAILABILITY_STATE, true);
                }
            }
        }

        return Result.success(outputDataBuilder.build());
    }

    private boolean inspect(String url) {
        try {
            String answer = GlobalWebClient.request(url);
            if (answer != null && answer.length() > 0) {
                Log.d("surprise", "CheckFlibustaAvailabilityWorker inspect 49: check success");
                return true;
            }
        } catch (TorNotLoadedException | IOException e) {
            Log.d("surprise", "CheckFlibustaAvailabilityWorker inspect 57: check failed");
            //e.printStackTrace();
        }
        return false;
    }
}
