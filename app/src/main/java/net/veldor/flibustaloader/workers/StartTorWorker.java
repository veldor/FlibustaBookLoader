package net.veldor.flibustaloader.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.client.protocol.HttpClientContext;

public class StartTorWorker extends Worker {

    public StartTorWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AndroidOnionProxyManager tor;
        if (App.getInstance().mTorManager.getValue() != null) {
            tor = App.getInstance().mTorManager.getValue();
        } else {
            tor = new AndroidOnionProxyManager(getApplicationContext(), App.TOR_FILES_LOCATION);
        }

        // добавлю полученный TOR для отслеживания его состояния
        App.getInstance().mLoadedTor.postValue(tor);

        // просто создание объекта, не запуск
        // тут- время, которое отводится на попытку запуска
        int totalSecondsPerTorStartup = (int) TimeUnit.MINUTES.toSeconds(4);
        // количество попыток запуска
        int totalTriesPerTorStartup = 5;
        try {
            boolean ok = tor.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup);
            if (!ok) {
                Log.d("surprise", "Tor отказался запускаться");
            }
            if (tor.isRunning()) {
                //Returns the socks port on the IPv4 localhost address that the Tor OP is listening on
                int port = tor.getIPv4LocalHostSocksPort();
                InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
                HttpClientContext context = HttpClientContext.create();
                context.setAttribute("socks.address", socksaddr);
                App.getInstance().mTorManager.postValue(tor);
            } else
                Log.d("surprise", "looks like, we need wait here for tor start works");
        } catch (InterruptedException e) {
            Log.d("surprise", "запуск TOR прерван");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            if(e.getMessage() != null && e.getMessage().contains("Permission denied")){
                return Result.failure();
            }
            e.printStackTrace();
        }
        return Result.success();
    }
}
