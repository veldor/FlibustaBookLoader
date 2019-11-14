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
import net.veldor.flibustaloader.receivers.BookLoadedReceiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
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

public class DownloadBookWorker extends Worker {

    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String FILENAME_DELIMITER = "filename=";

    public DownloadBookWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data data = getInputData();
        String[] properties = data.getStringArray(MyWebClient.DOWNLOAD_ATTRIBUTES);
        if(properties != null && properties.length == 3 && properties[1] != null){
            HttpClient httpClient = getNewHttpClient();
            int port;
            try {
                AndroidOnionProxyManager onionProxyManager = App.getInstance().mTorManager.getValue();
                assert onionProxyManager != null;
                port = onionProxyManager.getIPv4LocalHostSocksPort();
                InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
                HttpClientContext context = HttpClientContext.create();
                context.setAttribute("socks.address", socksaddr);
                HttpGet httpGet = new HttpGet(App.BASE_URL + properties[1]);
                httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
                httpGet.setHeader("X-Compress", "null");
                HttpResponse httpResponse = httpClient.execute(httpGet, context);

                String name = properties[2] + "." + properties[0];
                File file = new File(App.getInstance().getDownloadFolder(), name);
                InputStream is = httpResponse.getEntity().getContent();
                FileOutputStream outputStream = new FileOutputStream(file);
                int read;
                byte[] buffer = new byte[1024];
                while ((read = is.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.close();
                is.close();
                Intent intent = new Intent(App.getInstance(), BookLoadedReceiver.class);
                intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, name);
                intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, properties[0]);
                App.getInstance().sendBroadcast(intent);
                Log.d("surprise", "DownloadBookWorker doWork: book loaded!");

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
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


}
