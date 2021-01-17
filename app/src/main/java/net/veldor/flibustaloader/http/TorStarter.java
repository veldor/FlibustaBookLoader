package net.veldor.flibustaloader.http;

import android.util.Log;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TorStarter {
    private final AndroidOnionProxyManager Tor;
    public static final int TOTAL_TRIES_PER_TOR_STARTUP = 3;
    public static final int TOTAL_SECONDS_PER_TOR_STARTUP = (int) TimeUnit.MINUTES.toSeconds(1);

    public TorStarter(){
        if (App.getInstance().mLoadedTor.getValue() == null) {
            Tor = new AndroidOnionProxyManager(App.getInstance(), App.TOR_FILES_LOCATION);
            Log.d("surprise", "StartTorWorker doWork 39: tor manager added");
            App.getInstance().mLoadedTor.postValue(Tor);
        } else {
            Tor = App.getInstance().mLoadedTor.getValue();
        }
    }

    public boolean startTor() {
        if(Tor != null){
            // тут- время, которое отводится на попытку запуска
            // количество попыток запуска
            // пытаемся запустить TOR. Если он запустится- вернёт TRUE, Иначе- false
            try {
                return Tor.startWithRepeat(TOTAL_SECONDS_PER_TOR_STARTUP, TOTAL_TRIES_PER_TOR_STARTUP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public AndroidOnionProxyManager getTor() {
        return Tor;
    }
}
