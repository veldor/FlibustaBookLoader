package net.veldor.flibustaloader.http;

import android.content.Intent;
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
import net.veldor.flibustaloader.ecxeptions.FlibustaUnreachableException;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.workers.StartTorWorker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import cz.msebera.android.httpclient.HttpEntity;
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

    public boolean downloadBook(BooksDownloadSchedule book) throws BookNotFoundException, FlibustaUnreachableException, TorNotLoadedException {
        try {
            // получу имя файла
            DocumentFile downloadsDir = App.getInstance().getDownloadDir();
            DocumentFile newFile = downloadsDir.createFile(book.format, book.name);

            if (newFile != null) {
                // запрошу данные
                Log.d("surprise", "TorWebClient downloadBook: request " + book.link);
                HttpResponse response = simpleGetRequest(App.BASE_URL + book.link);
                if (response != null) {
                    int status = response.getStatusLine().getStatusCode();
                    if (status == 200) {
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            InputStream content = entity.getContent();
                            if (content != null) {
                                OutputStream out = App.getInstance().getContentResolver().openOutputStream(newFile.getUri());
                                if (out != null) {
                                    int read;
                                    byte[] buffer = new byte[1024];
                                    while ((read = content.read(buffer)) > 0) {
                                        out.write(buffer, 0, read);
                                    }
                                    out.close();
                                    if (newFile.isFile() && newFile.length() > 0) {
                                        return true;
                                    }
                                } else {
                                    Log.d("surprise", "TorWebClient downloadBook: файл не найден");
                                }
                            }
                        }
                    } else if (status == 500) {
                        Log.d("surprise", "TorWebClient downloadBook: книга не найдена");
                        throw new BookNotFoundException();
                    } else if (status == 404) {
                        Log.d("surprise", "TorWebClient downloadBook: flibusta not answer");
                        throw new FlibustaUnreachableException();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("surprise", "TorWebClient downloadBook: ошибка при сохранении");
            throw new TorNotLoadedException();
        }

        return false;
    }
}
