package net.veldor.flibustaloader.http;

import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;
import androidx.work.WorkManager;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyConnectionSocketFactory;
import net.veldor.flibustaloader.MySSLConnectionSocketFactory;
import net.veldor.flibustaloader.MyWebViewClient;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.ecxeptions.BookNotFoundException;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.utils.FilesHandler;
import net.veldor.flibustaloader.utils.MyPreferences;
import net.veldor.flibustaloader.utils.URLHelper;
import net.veldor.flibustaloader.workers.StartTorWorker;

import java.io.BufferedReader;
import java.io.File;
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

public class TorWebClient {

    private HttpClient mHttpClient;
    private HttpClientContext mContext;

    public TorWebClient() throws TorNotLoadedException {

        while (App.sTorStartTry < 4) {
            // есть три попытки, если все три неудачны- верну ошибку
            try {
                //Log.d("surprise", "StartTorWorker doWork: start tor, try # " + App.sTorStartTry);
                StartTorWorker.startTor();
                Log.d("surprise", "StartTorWorker doWork: tor success start");
                // обнулю счётчик попыток
                App.sTorStartTry = 0;
                break;
            } catch (TorNotLoadedException | IOException | InterruptedException e) {
                // попытка неудачна, плюсую счётчик попыток
                App.sTorStartTry++;
                Log.d("surprise", "StartTorWorker doWork: tor wrong start try");
            }
        }
        // если счётчик больше 3- не удалось запустить TOR, вызову исключение
        if (App.sTorStartTry > 3) {
            throw new TorNotLoadedException();
        }

        try {
            mHttpClient = getNewHttpClient();
            AndroidOnionProxyManager onionProxyManager = App.getInstance().mTorManager.getValue();
            if (onionProxyManager == null) {
                // верну ошибочный результат
                throw new TorNotLoadedException();
            }
            int port = onionProxyManager.getIPv4LocalHostSocksPort();
            Log.d("surprise", "TorWebClient TorWebClient 82: port is " + port);
            InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
            mContext = HttpClientContext.create();
            mContext.setAttribute("socks.address", socksaddr);
        } catch (IOException e) {
            e.printStackTrace();
            if (e.getMessage() != null && e.getMessage().equals(TOR_NOT_RUNNING_ERROR)) {
                // отправлю оповещение об ошибке загрузки TOR
                broadcastTorError();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            if (e.getMessage() != null && e.getMessage().equals(TOR_NOT_RUNNING_ERROR)) {
                // отправлю оповещение об ошибке загрузки TOR
                broadcastTorError();
            }
        }
    }

    private static void broadcastTorError() {
        // остановлю все задачи
        WorkManager.getInstance(App.getInstance()).cancelAllWork();
        // отправлю оповещение об ошибке загрузки TOR
        Intent finishLoadingIntent = new Intent(TOR_CONNECT_ERROR_ACTION);
        App.getInstance().sendBroadcast(finishLoadingIntent);
    }


    public String request(String text) {
        try {
            HttpGet httpGet = new HttpGet(text);
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
            httpGet.setHeader("X-Compress", "null");
            HttpResponse httpResponse = mHttpClient.execute(httpGet, mContext);
            App.getInstance().mLoadAllStatus.postValue("Данные получены");
            InputStream is;
            is = httpResponse.getEntity().getContent();
            return inputStreamToString(is);
        } catch (IOException e) {
            App.getInstance().mLoadAllStatus.postValue("Ошибка загрузки страницы");
            broadcastTorError();
            e.printStackTrace();
        }
        return null;
    }

    private HttpResponse simpleGetRequest(String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
        httpGet.setHeader("X-Compress", "null");
        return mHttpClient.execute(httpGet, mContext);
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

    public void downloadBook(BooksDownloadSchedule book) throws BookNotFoundException, TorNotLoadedException {
        try {
            HttpResponse response = simpleGetRequest(App.BASE_URL + book.link);
            // проверю, что запрос выполнен и файл не пуст. Если это не так- попорбую загрузить книгу с основного домена
            if(response == null || response.getStatusLine().getStatusCode() != 200 || response.getEntity().getContentLength()  < 1){
                Log.d("surprise", "ExternalVpnVewClient downloadBook 116: request from reserve " + URLHelper.getFlibustaIsUrl() + book.link);
                // попробую загрузку с резервного адреса
                response = simpleGetRequest(URLHelper.getFlibustaIsUrl() + book.link);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                DocumentFile newFile = FilesHandler.getDownloadFile(book);
                if (newFile != null) {
                    // запрошу данные
                    Log.d("surprise", "TorWebClient downloadBook: request " + book.link + " of book " + book.name);
                    GlobalWebClient.handleBookLoadRequest(response, newFile);
                }
            } else {
                File file = FilesHandler.getCompatDownloadFile(book);
                GlobalWebClient.handleBookLoadRequest(response, file);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("surprise", "TorWebClient downloadBook: ошибка при сохранении");
            throw new TorNotLoadedException();
        }
    }
}
