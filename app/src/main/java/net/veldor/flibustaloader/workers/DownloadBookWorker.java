package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
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
import net.veldor.flibustaloader.utils.TorWebClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

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

public class DownloadBookWorker extends Worker {


    private String mName;

    public DownloadBookWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        App.getInstance().mDownloadProgress.postValue(true);
        Data data = getInputData();
        String[] properties = data.getStringArray(MyWebClient.DOWNLOAD_ATTRIBUTES);
        if (properties != null && properties.length == 5 && properties[1] != null) {
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

                String author_last_name = properties[3].substring(0, properties[3].indexOf(" "));
                String book_name = properties[2].replaceAll(" ", "_").replaceAll("[^\\d\\w-_]", "");
                String book_mime = properties[0];
                // если сумма символов меньше 255- создаю полное имя
                if (author_last_name.length() + book_name.length() + book_mime.length() + 2 < 255 / 2) {
                    mName = author_last_name + "_" + book_name + "." + book_mime;
                } else {
                    // сохраняю книгу по имени автора и тому, что влезет от имени книги
                    //name = author_last_name + "_" + book_name.substring(0, 127 - author_last_name.length() + book_mime.length() + 2) + "." + book_mime;
                    mName = author_last_name + "_" + book_name.substring(0, 127 - (author_last_name.length() + book_mime.length() + 2)) + book_mime;

                }
                DocumentFile downloadsDir = App.getInstance().getNewDownloadDir();
                if(downloadsDir != null){
                    // проверю, не сохдан ли уже файл, если создан- удалю
                    DocumentFile existentFile = downloadsDir.findFile(mName);
                    if(existentFile != null){
                        existentFile.delete();
                    }
                    DocumentFile newFile = downloadsDir.createFile(book_mime, mName);
                    if(newFile != null){
                        InputStream is = httpResponse.getEntity().getContent();
                        OutputStream out = App.getInstance().getContentResolver().openOutputStream(newFile.getUri());
                        if(out != null){
                            int read;
                            byte[] buffer = new byte[1024];
                            while ((read = is.read(buffer)) > 0) {
                                out.write(buffer, 0, read);
                            }
                            out.close();
                        }
                        else{
                            throw new IOException("Не удалось создать файл");
                        }
                    }
                    // проверю, загрузилась ли книга
                    if(newFile == null || !newFile.exists() || newFile.length() == 0){
                        // файл не создан
                        throw new IOException("Не удалось создать файл");
                    }
                }
                else{
                    File file = new File(App.getInstance().getDownloadFolder(), mName);
                    InputStream is = httpResponse.getEntity().getContent();
                    FileOutputStream outputStream = new FileOutputStream(file);
                    int read;
                    byte[] buffer = new byte[1024];
                    while ((read = is.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.close();
                    is.close();

                    // проверю, загрузилась ли книга
                    if(!file.exists() || file.length() == 0){
                        // файл не создан
                        throw new IOException("Не удалось создать файл");
                    }
                }
                Intent intent = new Intent(App.getInstance(), BookLoadedReceiver.class);
                intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, mName);
                intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, book_mime);
                App.getInstance().sendBroadcast(intent);
                // помещу книгу в список загруженных
                DatabaseWorker.makeBookDownloaded(properties[4]);

            } catch (ClientProtocolException e) {
                // отправлю оповещение об ошибке загрузки TOR
                TorWebClient.broadcastTorError();
                e.printStackTrace();
            } catch (IOException e) {
                Log.d("surprise", "DownloadBookWorker doWork  i have error");
                App.getInstance().mUnloadedBook.postValue(mName);
                // отправлю оповещение об ошибке загрузки TOR
                //TorWebClient.broadcastTorError();
                e.printStackTrace();
            }
        }
        App.getInstance().mDownloadProgress.postValue(false);
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
