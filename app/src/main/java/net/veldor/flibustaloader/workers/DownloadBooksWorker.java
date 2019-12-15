package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyConnectionSocketFactory;
import net.veldor.flibustaloader.MySSLConnectionSocketFactory;
import net.veldor.flibustaloader.MyWebViewClient;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.utils.Grammar;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.utils.TorWebClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;

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

public class DownloadBooksWorker extends Worker {

    private HttpClient mHttpClient;
    private HttpClientContext mContext;
    private boolean mIsStopped;
    private String mName;

    public DownloadBooksWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // отмечу, что процесс уже идёт
        Log.d("surprise", "DownloadBooksWorker doWork i start downloads");
        // получу предпочтительный тип загрузки
        Data data = getInputData();
        int bookType = data.getInt(MimeTypes.MIME_TYPE, 0);
        String bookMime = MimeTypes.MIMES_LIST[bookType];
        // получу список книг
        if (App.getInstance().mBooksForDownload != null && App.getInstance().mBooksForDownload.size() > 0) {
                // переберу все книги
                FoundedBook book;
                while (App.getInstance().mBooksForDownload != null && App.getInstance().mBooksForDownload.size() > 0 && !mIsStopped) {
                    // возьму первый элемент из списка недогруженных книг
                    book = (FoundedBook) App.getInstance().mBooksForDownload.get(0);
                    downloadBook(book, bookMime);
                    // книга загружена, удалю из списка
                    App.getInstance().mBooksForDownload.remove(0);

                    App.getInstance().mMultiplyDownloadStatus.postValue("Скачано " + (App.getInstance().mParsedResult.getValue().size() - App.getInstance().mBooksForDownload.size()) + " из " + App.getInstance().mParsedResult.getValue().size() + " книг.");
                }

        }
        Log.d("surprise", "DownloadBooksWorker doWork закончил загрузку");
        App.getInstance().mDownloadInProgress = false;
        return Result.success();
    }

    private void downloadBook(FoundedBook book, String bookMime) {
        try {
            // настрою клиент
            mHttpClient = getNewHttpClient();
            int port;
            AndroidOnionProxyManager onionProxyManager = App.getInstance().mTorManager.getValue();
            assert onionProxyManager != null;
            port = onionProxyManager.getIPv4LocalHostSocksPort();
            InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
            mContext = HttpClientContext.create();
            mContext.setAttribute("socks.address", socksaddr);
        } catch (IOException e) {
            TorWebClient.broadcastTorError();
            e.printStackTrace();
        }
        try{
        // получу все ссылки
        ArrayList<DownloadLink> links = book.downloadLinks;
        DownloadLink link;
        // если в списке всего одна ссылка- скачаю файл, как он есть, если несколько- попробую выбрать предпочтительную, если её нет- скачаю первую
        if (links.size() == 1) {
            link = links.get(0);
        } else {
            // получу полный предпочтительный mime
            String mime = MimeTypes.getFullMime(bookMime);
            int counter = 0;
            while ((link = links.get(counter)) != null) {
                if (link.mime.equals(mime)) {
                    break;
                }
                counter++;
            }
            // если не нашли предпочтительный тип- попробую поискать fb2, как самый распространённый тип
            if (link == null) {
                counter = 0;
                while ((link = links.get(counter)) != null) {
                    if (link.mime.equals(MimeTypes.getFullMime("fb2"))) {
                        break;
                    }
                    counter++;
                }
            }
            // если вообще ничего похожего- загружу первый попавшийся тип :)
            if (link == null) {
                link = links.get(0);
            }
        }
        // получу ссылку на скачивание
        HttpGet httpGet = new HttpGet(App.BASE_URL + link.url);
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
        httpGet.setHeader("X-Compress", "null");
        HttpResponse httpResponse = mHttpClient.execute(httpGet, mContext);
        String author_last_name = link.author.substring(0, link.author.indexOf(" "));
        String book_name = link.name.replaceAll(" ", "_").replaceAll("[^\\d\\w-_]", "");
        String book_mime = MimeTypes.getDownloadMime(link.mime);
        //Log.d("surprise", "DownloadBooksWorker downloadBook " + book_mime);
        // если сумма символов меньше 255- создаю полное имя
        if (author_last_name.length() + book_name.length() + book_mime.length() + 2 < 255 / 2 - 6) {
            mName = author_last_name + "_" + book_name + "_" + Grammar.getRandom() + "." + book_mime;
        } else {
            // сохраняю книгу по имени автора и тому, что влезет от имени книги
            mName = author_last_name + "_" + book_name.substring(0, 127 - (author_last_name.length() + book_mime.length() + 2 + 6)) + "_" + Grammar.getRandom() + "." + book_mime;
        }
        //Log.d("surprise", "DownloadBooksWorker downloadBook " + name);
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
        } catch (ClientProtocolException e) {
            Log.d("surprise", "DownloadBooksWorker downloadBook clientProtocolException");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("surprise", "DownloadBooksWorker doWork have error here");
            // добавлю книгу в список книг, которые не удалсь скачать
            App.getInstance().mBooksDownloadFailed.add(book);
            App.getInstance().mDownloadInProgress = false;
            App.getInstance().mUnloadedBook.postValue(mName);
            //WorkManager.getInstance().cancelAllWork();
            // отправлю оповещение об ошибке загрузки TOR
            //TorWebClient.broadcastTorError();
            e.printStackTrace();
        }
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

    @Override
    public void onStopped() {
        super.onStopped();
        Log.d("surprise", "GetAllPagesWorker onStopped i stopped");
        mIsStopped = true;
        // остановлю процесс
    }
}
