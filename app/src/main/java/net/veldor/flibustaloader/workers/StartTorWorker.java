package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;

public class StartTorWorker extends Worker {

    public StartTorWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("surprise", "StartTorWorker doWork: start opening TOR");
        // просто создание объекта, не запуск
        AndroidOnionProxyManager tor = new AndroidOnionProxyManager(getApplicationContext(), App.TOR_FILES_LOCATION);

        // тут- время, которое отводится на попытку запуска
        int totalSecondsPerTorStartup = (int) TimeUnit.MINUTES.toSeconds(4);
        // количество попыток запуска
        int totalTriesPerTorStartup = 5;

        try {
            boolean ok = tor.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup);
            if(!ok){
                Log.d("surprise", "Tor отказался запускаться");
            }
            if(tor.isRunning()){
                //Returns the socks port on the IPv4 localhost address that the Tor OP is listening on
                int port = tor.getIPv4LocalHostSocksPort();
                InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
                HttpClientContext context = HttpClientContext.create();
                context.setAttribute("socks.address", socksaddr);
                App.getInstance().mTorManager.postValue(tor);
                Toast.makeText(App.getInstance(), "Tor запустился", Toast.LENGTH_LONG).show();
            }
            else
                Log.d("surprise", "looks like, we need wait here for tor start works");
        } catch (InterruptedException e) {
            Log.d("surprise", "запуск TOR прерван");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("surprise", "ошибка запуска TOR");
            e.printStackTrace();
        }
        return Result.success();
    }
}
