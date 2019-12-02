package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyConnectionSocketFactory;
import net.veldor.flibustaloader.MySSLConnectionSocketFactory;
import net.veldor.flibustaloader.MyWebClient;
import net.veldor.flibustaloader.MyWebViewClient;

import java.io.IOException;
import java.net.InetSocketAddress;

import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.config.Registry;
import cz.msebera.android.httpclient.config.RegistryBuilder;
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager;
import cz.msebera.android.httpclient.ssl.SSLContexts;

import static net.veldor.flibustaloader.MyWebViewClient.TOR_CONNECT_ERROR_ACTION;
import static net.veldor.flibustaloader.MyWebViewClient.TOR_NOT_RUNNING_ERROR;

public class SetupWebclientWorker extends Worker {

    public SetupWebclientWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            MyWebClient webClient = App.getInstance().getWebClient();
            webClient.mHttpClient = getNewHttpClient();

            AndroidOnionProxyManager onionProxyManager = App.getInstance().mTorManager.getValue();
            assert onionProxyManager != null;
            int port = onionProxyManager.getIPv4LocalHostSocksPort();
            InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
            webClient.mContext = HttpClientContext.create();
            webClient.mContext.setAttribute("socks.address", socksaddr);
        } catch (IOException e) {
            e.printStackTrace();
            if (e.getMessage().equals(TOR_NOT_RUNNING_ERROR)) {
                // отправлю оповещение об ошибке загрузки TOR
                Intent finishLoadingIntent = new Intent(TOR_CONNECT_ERROR_ACTION);
                App.getInstance().sendBroadcast(finishLoadingIntent);
            }
        }
        Log.d("surprise", "SetupWebclientWorker doWork: webClient setted");
        return Result.success();
    }

    private HttpClient getNewHttpClient() {
        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new MyConnectionSocketFactory())
                .register("https", new MySSLConnectionSocketFactory(SSLContexts.createSystemDefault()))
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg, new MyWebViewClient.FakeDnsResolver());
        return HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }
}
