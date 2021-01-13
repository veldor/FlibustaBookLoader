package net.veldor.flibustaloader.workers;

import android.content.Context;

import androidx.annotation.NonNull;

import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;

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
        // попробую стартовать TOR
        while (App.sTorStartTry < 4 && !isStopped()) {
            // есть три попытки, если все три неудачны- верну ошибку
            try {
                Log.d("surprise", "StartTorWorker doWork: start tor, try # " + App.sTorStartTry);
                startTor();
                // обнулю счётчик попыток
                App.sTorStartTry = 0;
                return Result.success();
            } catch (TorNotLoadedException | IOException | InterruptedException e) {
                // попытка неудачна, плюсую счётчик попыток
                App.sTorStartTry++;
                Log.d("surprise", "StartTorWorker doWork: tor wrong start try");
            }
        }
        Log.d("surprise", "StartTorWorker doWork: i can't load TOR");
        if(isStopped()){
            Log.d("surprise", "StartTorWorker doWork: i stopped");
            return Result.retry();
        }
        return Result.failure();
    }

    public static void startTor() throws TorNotLoadedException, IOException, InterruptedException {
        AndroidOnionProxyManager tor;
        if (App.getInstance().mTorManager.getValue() != null) {
            tor = App.getInstance().mTorManager.getValue();
        } else {
            tor = new AndroidOnionProxyManager(App.getInstance(), App.TOR_FILES_LOCATION);
        }
        // добавлю полученный TOR для отслеживания его состояния
        App.getInstance().mLoadedTor.postValue(tor);
        // просто создание объекта, не запуск
        // тут- время, которое отводится на попытку запуска
        int totalSecondsPerTorStartup = (int) TimeUnit.MINUTES.toSeconds(3);
        // количество попыток запуска
        int totalTriesPerTorStartup = 1;
            boolean ok = tor.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup);
            if (!ok) {
                // TOR не запущен, оповещу о том, что запуск не удался
                throw new TorNotLoadedException();
            }
            if (tor.isRunning()) {
                //Returns the socks port on the IPv4 localhost address that the Tor OP is listening on
                int port = tor.getIPv4LocalHostSocksPort();
                InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
                HttpClientContext context = HttpClientContext.create();
                context.setAttribute("socks.address", socksaddr);
                App.getInstance().mTorManager.postValue(tor);
            } else {
                // TOR не запущен, оповещу о том, что запуск не удался
                throw new TorNotLoadedException();
            }
    }
}
