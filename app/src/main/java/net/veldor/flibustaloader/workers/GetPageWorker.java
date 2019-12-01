package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyConnectionSocketFactory;
import net.veldor.flibustaloader.MySSLConnectionSocketFactory;
import net.veldor.flibustaloader.MyWebClient;
import net.veldor.flibustaloader.MyWebViewClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.config.Registry;
import cz.msebera.android.httpclient.config.RegistryBuilder;
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager;
import cz.msebera.android.httpclient.ssl.SSLContexts;

import static net.veldor.flibustaloader.MyWebViewClient.TOR_CONNECT_ERROR_ACTION;
import static net.veldor.flibustaloader.MyWebViewClient.TOR_NOT_RUNNING_ERROR;

public class GetPageWorker extends Worker {
    public GetPageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data data = getInputData();
        String text = data.getString(MyWebClient.LOADED_URL);
        HttpClient httpClient = getNewHttpClient();
        int port;
        try {
            AndroidOnionProxyManager onionProxyManager = App.getInstance().mTorManager.getValue();
            assert onionProxyManager != null;
            port = onionProxyManager.getIPv4LocalHostSocksPort();
            InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
            HttpClientContext context = HttpClientContext.create();
            context.setAttribute("socks.address", socksaddr);
            HttpGet httpGet = new HttpGet(text);
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
            httpGet.setHeader("X-Compress", "null");
            HttpResponse httpResponse = httpClient.execute(httpGet, context);
            InputStream is = httpResponse.getEntity().getContent();
            // верну ответ
            App.getInstance().mSearchResult.postValue(inputStreamToString(is));
        } catch (IOException e) {
            e.printStackTrace();
            if (e.getMessage().equals(TOR_NOT_RUNNING_ERROR)) {
                // отправлю оповещение об ошибке загрузки TOR
                Intent finishLoadingIntent = new Intent(TOR_CONNECT_ERROR_ACTION);
                App.getInstance().sendBroadcast(finishLoadingIntent);
            }
        }
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

    private String inputStreamToString(InputStream is) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append(line).append('\n');
            }
            return total.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
