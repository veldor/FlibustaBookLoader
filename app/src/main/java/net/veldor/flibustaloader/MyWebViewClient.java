package net.veldor.flibustaloader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.ecxeptions.ConnectionLostException;
import net.veldor.flibustaloader.http.ExternalVpnVewClient;
import net.veldor.flibustaloader.http.GlobalWebClient;
import net.veldor.flibustaloader.http.TorStarter;
import net.veldor.flibustaloader.http.TorWebClient;
import net.veldor.flibustaloader.receivers.BookLoadedReceiver;
import net.veldor.flibustaloader.utils.FilesHandler;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.utils.MyPreferences;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.config.Registry;
import cz.msebera.android.httpclient.config.RegistryBuilder;
import cz.msebera.android.httpclient.conn.DnsResolver;
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager;
import cz.msebera.android.httpclient.ssl.SSLContexts;

public class MyWebViewClient extends WebViewClient {

    public static final String TOR_NOT_RUNNING_ERROR = "Tor is not running!";
    public static final String BOOK_LOAD_ACTION = "net.veldor.flibustaloader.action.BOOK_LOAD_EVENT";
    public static final String TOR_CONNECT_ERROR_ACTION = "net.veldor.flibustaloader.action.TOR_CONNECT_ERROR";
    public static final int START_BOOK_LOADING = 1;
    public static final int FINISH_BOOK_LOADING = 2;
    private static final String ENCODING_UTF_8 = "UTF-8";
    private static final String FB2_FORMAT = "application/zip";
    private static final String PDF_FORMAT = "application/pdf";
    private static final String CSS_FORMAT = "text/css";
    private static final String JS_FORMAT = "application/x-javascript";


    // content types
    private static final String FB2_TYPE = "fb2";
    private static final String MOBI_TYPE = "mobi";
    private static final String EPUB_TYPE = "epub";
    private static final String PDF_TYPE = "pdf";
    private static final String DJVU_TYPE = "djvu";

    private static final String JPG_TYPE = "jpg";
    private static final String JPEG_TYPE = "jpeg";
    private static final String GIF_TYPE = "gif";
    private static final String PNG_TYPE = "png";


    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String FILENAME_DELIMITER = "filename=";
    public static final String BOOK_LOAD_EVENT = "book load event";
    private static final String MY_COMPAT_CSS_STYLE = "myCompatStyle.css";
    private static final String MY_CSS_NIGHT_STYLE = "myNightMode.css";
    private static final String MY_COMPAT_FAT_CSS_STYLE = "myCompatFatStyle.css";
    private static final String MY_JS = "myJs.js";
    private static final String HTML_TYPE = "text/html";
    private static final String AJAX_REQUEST = "http://flibustahezeous3.onion/makebooklist?";

    private AndroidOnionProxyManager onionProxyManager;
    private int mViewMode;
    private boolean mNightMode;

    MyWebViewClient() {
        onionProxyManager = App.getInstance().mLoadedTor.getValue();
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return handleRequest(view, url);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {

        String requestString = request.getUrl().toString();
        return handleRequest(view, requestString);
    }


    private HttpClient getNewHttpClient() {
        while (App.getInstance().torInitInProgress) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        App.getInstance().torInitInProgress = true;
        // попробую стартовать TOR
        TorStarter starter = new TorStarter();
        App.sTorStartTry = 0;
        while (App.sTorStartTry < 4) {
            // есть три попытки, если все три неудачны- верну ошибку
            if (starter.startTor()) {
                GlobalWebClient.mConnectionState.postValue(GlobalWebClient.CONNECTED);
                App.sTorStartTry = 0;
                break;
            } else {
                App.sTorStartTry++;
            }
        }
        App.getInstance().torInitInProgress = false;
        // если счётчик больше 3- не удалось запустить TOR, вызову исключение
        if (App.sTorStartTry > 3) {
//            throw new TorNotLoadedException();
            // сделаю по новому- уведомлю, что не удалось установить соединение
            GlobalWebClient.mConnectionState.postValue(GlobalWebClient.DISCONNECTED);
            return null;
        }
        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new MyConnectionSocketFactory())
                .register("https", new MySSLConnectionSocketFactory(SSLContexts.createSystemDefault()))
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg, new FakeDnsResolver());
        return HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }


    private String injectMyJs(String originalJs) {
        String output = originalJs;
        try {
            if (mViewMode == App.VIEW_MODE_FAT || mViewMode == App.VIEW_MODE_LIGHT) {
                App context = App.getInstance();
                /*inputStream = context.getAssets().open(JQUERY);
                output += inputStreamToString(inputStream);*/
                InputStream inputStream = context.getAssets().open(MY_JS);
                output += inputStreamToString(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    private String injectMyCss(String originalCss) {
        // старые версии Android не понимают переменные цветов и новые объявления JS, подключусь в режиме совместимости
        App context = App.getInstance();
        InputStream inputStream;
        String output = originalCss;
        try {
            if (mViewMode > 1) {
                switch (mViewMode) {
                    case App.VIEW_MODE_FAT:
                    case App.VIEW_MODE_FAST_FAT:
                        inputStream = context.getAssets().open(MY_COMPAT_FAT_CSS_STYLE);
                        break;
                    case App.VIEW_MODE_LIGHT:
                    default:
                        inputStream = context.getAssets().open(MY_COMPAT_CSS_STYLE);
                }

                output += inputStreamToString(inputStream);
            }
            if (mNightMode) {
                inputStream = context.getAssets().open(MY_CSS_NIGHT_STYLE);
                output += inputStreamToString(inputStream);
            }
            return output;
        } catch (IOException e) {
            Log.d("surprise", "MyWebViewClient injectMyCss: error when injecting my Js or CSS");
            e.printStackTrace();
        }
        return null;
    }

    public static class FakeDnsResolver implements DnsResolver {
        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            return new InetAddress[]{InetAddress.getByAddress(new byte[]{1, 1, 1, 1})};
        }
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

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private WebResourceResponse handleRequest(WebView view, String url) {
        Log.d("surprise", "MyWebViewClient handleRequest 235: request " + url);
        if (App.getInstance().useMirror) {
            url = url.replace("http://flibustahezeous3.onion", "https://flibusta.appspot.com");
        }
        try {
            mViewMode = App.getInstance().getViewMode();
            mNightMode = App.getInstance().getNightMode();
            // обрубаю загрузку картинок в упрощённом виде
            if (mViewMode > 1) {
                String[] extensionArr = url.split("\\.");
                if (extensionArr.length > 0) {
                    String extension = extensionArr[extensionArr.length - 1];
                    if (extension.equals(JPG_TYPE) || extension.equals(JPEG_TYPE) || extension.equals(PNG_TYPE) || extension.equals(GIF_TYPE)) {
                        return super.shouldInterceptRequest(view, url);
                    }
                }
            }
            HttpResponse httpResponse;
            if (App.getInstance().isExternalVpn()) {
                httpResponse = ExternalVpnVewClient.rawRequest(url);
            } else {
                HttpClient httpClient = getNewHttpClient();
                // если вернулся null- значит, не удалось получить клиент, скажу об ошибке соединения
                if (httpClient == null) {
                    return getConnectionError();
                }
                if (onionProxyManager == null) {
                    onionProxyManager = App.getInstance().mLoadedTor.getValue();
                }
                if (onionProxyManager == null) {
                    return null;
                }
                int port = onionProxyManager.getIPv4LocalHostSocksPort();
                InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
                HttpClientContext context = HttpClientContext.create();
                context.setAttribute("socks.address", socksaddr);
                HttpGet httpGet = new HttpGet(url);
                httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
                httpGet.setHeader("X-Compress", "null");
                String authCookie = MyPreferences.getInstance().getAuthCookie();
                if (authCookie != null) {
                    httpGet.setHeader("Cookie", authCookie);
                }
                try {
                    httpResponse = httpClient.execute(httpGet, context);
                } catch (Exception e) {
                    return getConnectionError();
                }
            }

            if (httpResponse == null) {
                return getConnectionError();
            }

            InputStream input = httpResponse.getEntity().getContent();
            String encoding = ENCODING_UTF_8;
            String mime = httpResponse.getEntity().getContentType().getValue();
            Log.d("surprise", "MyWebViewClient handleRequest 262: mime is " + mime + " request is " + url);

            // todo разобраться с application/octet-stream
            if (mime.equals("application/octet-stream")) {
                Log.d("surprise", "MyWebViewClient handleRequest 298: HAVE OCTET-STREAM");
                // придётся ориентироваться по имени файла и определять, что это книга
                // костыль, конечно, но что делать

                // покажу хедеры
                Header[] headers = httpResponse.getAllHeaders();
                for (Header h :
                        headers) {
                    if (h.getName().equals("Content-Disposition")) {
                        // похоже на книгу
                        Log.d("surprise", "MyWebViewClient handleRequest 310: LOOKS LIKE ITS BOOK");
                        // Тут пока что грязный хак, скажу, что это epub
                        mime = "application/epub";
                    }
                }
            }
            // Если формат книжный, загружу книгу
            if (MimeTypes.isBookFormat(mime)) {
                Log.d("surprise", "MyWebViewClient handleRequest 277: ADD BOOK TO QUEUE");
                Intent startLoadingIntent = new Intent(BOOK_LOAD_ACTION);
                startLoadingIntent.putExtra(BOOK_LOAD_EVENT, START_BOOK_LOADING);
                App.getInstance().sendBroadcast(startLoadingIntent);
                // пока что- сэмулирую загрузку по типу OPDS
                BooksDownloadSchedule newBook = new BooksDownloadSchedule();
                // покажу хедеры
                Header[] headers = httpResponse.getAllHeaders();
                for (Header h :
                        headers) {
                    Log.d("surprise", "MyWebViewClient handleRequest 271: Header " + h.getName());
                    Log.d("surprise", "MyWebViewClient handleRequest 271: Header VALUE" + h.getValue());
                    if (h.getValue().startsWith("attachment; filename=\"")) {
                        newBook.name = h.getValue().substring(22);
                        Log.d("surprise", "MyWebViewClient handleRequest 276: name is " + newBook.name);
                    } else if (h.getValue().startsWith("attachment;")) {
                        newBook.name = h.getValue().substring(21);
                        Log.d("surprise", "MyWebViewClient handleRequest 276: name is " + newBook.name);
                    }
                }
                TorWebClient client;
                // создам файл
                newBook.link = url.substring(url.indexOf("/b"));
                try {
                    client = new TorWebClient();
                } catch (ConnectionLostException e) {
                    return getConnectionError();
                }
                client.downloadBook(newBook);
                // проверю, что книга загружена. Если да- оповещу об этом, если нет-
                // о том, что не удалось загрузить
                String message;
                if(FilesHandler.isBookDownloaded(newBook)){
                    Intent intent = new Intent(App.getInstance(), BookLoadedReceiver.class);
                    intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, newBook.name);
                    intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, newBook.format);
                    App.getInstance().sendBroadcast(intent);
                    message = "<H1 style='text-align:center;'>Книга загружена</H1><H2 style='text-align:center;'>Возвращаюсь на предыдущую страницу</H2><script>setTimeout(function(){history.back()}, 1000)</script>";
                }
                else{
                    message = "<H1 style='text-align:center;'>Книгу загрузить не удалось, попробуйте позднее</H1><H2 style='text-align:center;'>Возвращаюсь на предыдущую страницу</H2><script>setTimeout(function(){history.back()}, 1000)</script>";
                }
                ByteArrayInputStream inputStream = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    inputStream = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
                } else {
                    try {
                        inputStream = new ByteArrayInputStream(message.getBytes(ENCODING_UTF_8));
                    } catch (UnsupportedEncodingException ex) {
                        ex.printStackTrace();
                    }
                }
                Intent finishLoadingIntent = new Intent(BOOK_LOAD_ACTION);
                finishLoadingIntent.putExtra(BOOK_LOAD_EVENT, FINISH_BOOK_LOADING);
                App.getInstance().sendBroadcast(finishLoadingIntent);
                return new WebResourceResponse("application/zip", ENCODING_UTF_8, inputStream);
            }

            // если загружена страница- добавлю её как последнюю загруженную
            if (mime.startsWith(HTML_TYPE)) {
                if (!url.startsWith(AJAX_REQUEST)) {
                    App.getInstance().setLastLoadedUrl(url);
                    // попробую найти внутри ссылки на книги
                    // скопирую inputStream для разбора ссылок
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    // Fake code simulating the copy
                    // You can generally do better with nio if you need...
                    // And please, unlike me, do something about the Exceptions :D
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = input.read(buffer)) > -1) {
                        baos.write(buffer, 0, len);
                    }
                    baos.flush();
                    // Open new InputStreams using the recorded bytes
                    // Can be repeated as many times as you wish
                    InputStream my = new ByteArrayInputStream(baos.toByteArray());
                    input = new ByteArrayInputStream(baos.toByteArray());
                    // запущу рабочего, который обработает текст страницы и найдёт что-то полезное
                    App.getInstance().handleWebPage(my);
                }
            }
            if (mime.equals(CSS_FORMAT)) {
                InputStream is = httpResponse.getEntity().getContent();
                // подключу нужные CSS простым объединением строк
                String origin = inputStreamToString(is);
                String injectionText = injectMyCss(origin);
                if (injectionText != null) {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(injectionText.getBytes(encoding));
                    return new WebResourceResponse(mime, ENCODING_UTF_8, inputStream);
                }
                if (origin != null) {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(origin.getBytes(encoding));
                    return new WebResourceResponse(mime, ENCODING_UTF_8, inputStream);
                }
                return new WebResourceResponse(mime, ENCODING_UTF_8, null);

            } else if (mime.equals(JS_FORMAT)) {
                InputStream is = httpResponse.getEntity().getContent();
                String origin = inputStreamToString(is);
                String injectionText = injectMyJs(origin);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(injectionText.getBytes(encoding));
                return new WebResourceResponse(mime, ENCODING_UTF_8, inputStream);
            }

            if (mime.contains(";")) {
                String[] arr = mime.split(";");
                mime = arr[0];
                arr = arr[1].split("=");
                encoding = arr[1];
            }

            if (mime.equals(FB2_FORMAT) || mime.equals(PDF_FORMAT)) {
                Context activityContext = view.getContext();
                Header header = httpResponse.getFirstHeader(HEADER_CONTENT_DISPOSITION);
                String name = header.getValue().split(FILENAME_DELIMITER)[1];
                name = name.replace("\"", "");
                String[] extensionSource = name.split("\\.");
                String extension = extensionSource[extensionSource.length - 1];
                String[] types = url.split("/");
                String type = types[types.length - 1];
                if (mime.equals(PDF_FORMAT)) {
                    type = PDF_TYPE;
                }
                if (extension.equals(DJVU_TYPE)) {
                    type = DJVU_TYPE;
                }
                if (type.equals(FB2_TYPE) || type.equals(MOBI_TYPE) || type.equals(EPUB_TYPE) || type.equals(PDF_TYPE) || type.equals(DJVU_TYPE)) {
                    try {
                        // начинаю загружать книку, пошлю оповещение о начале загрузки
                        Intent startLoadingIntent = new Intent(BOOK_LOAD_ACTION);
                        startLoadingIntent.putExtra(BOOK_LOAD_EVENT, START_BOOK_LOADING);
                        activityContext.sendBroadcast(startLoadingIntent);
                        // сохраняю книгу в памяти устройства
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            DocumentFile downloadsDir = App.getInstance().getDownloadDir();
                            if (downloadsDir != null) {
                                // проверю, не сохдан ли уже файл, если создан- удалю
                                DocumentFile existentFile = downloadsDir.findFile(name);
                                if (existentFile != null) {
                                    existentFile.delete();
                                }
                                DocumentFile newFile = downloadsDir.createFile(mime, name);
                                if (newFile != null) {
                                    InputStream is = httpResponse.getEntity().getContent();
                                    OutputStream out = App.getInstance().getContentResolver().openOutputStream(newFile.getUri());
                                    int read;
                                    byte[] buffer = new byte[1024];
                                    while ((read = is.read(buffer)) > 0) {
                                        assert out != null;
                                        out.write(buffer, 0, read);
                                    }
                                    assert out != null;
                                    out.close();
                                }
                            }
                        } else {
                            File file = MyPreferences.getInstance().getDownloadDir();
                            if (file != null) {
                                File newFile = new File(file, name);
                                int status = httpResponse.getStatusLine().getStatusCode();
                                if (status == 200) {
                                    HttpEntity entity = httpResponse.getEntity();
                                    if (entity != null) {
                                        InputStream content = entity.getContent();
                                        if (content != null) {
                                            OutputStream out = new FileOutputStream(newFile);
                                            int read;
                                            byte[] buffer = new byte[1024];
                                            while ((read = content.read(buffer)) > 0) {
                                                out.write(buffer, 0, read);
                                            }
                                            out.close();
                                        }
                                    }
                                }
                            }
                        }
                        // отправлю сообщение о скачанном файле через broadcastReceiver
                        Intent intent = new Intent(activityContext, BookLoadedReceiver.class);
                        intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, name);
                        intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, type);
                        activityContext.sendBroadcast(intent);
                        String message = "<H1 style='text-align:center;'>Книга закачана. Возвращаюсь на предыдущую страницу</H1>";
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBytes(encoding));
                        return new WebResourceResponse(mime, ENCODING_UTF_8, inputStream);
                    } catch (IOException e) {
                        Log.d("surprise", "some output error");
                    } finally {
                        // отправлю оповещение об окончании загрузки страницы
                        Intent finishLoadingIntent = new Intent(BOOK_LOAD_ACTION);
                        finishLoadingIntent.putExtra(BOOK_LOAD_EVENT, FINISH_BOOK_LOADING);
                        activityContext.sendBroadcast(finishLoadingIntent);
                    }
                }
            }
            return new WebResourceResponse(mime, encoding, input);
        } catch (Exception e) {
            e.printStackTrace();
            return getConnectionError();
        }
    }

    /**
     * Сообщу об ошибке соединения и верну заглушку
     */
    private WebResourceResponse getConnectionError() {
        GlobalWebClient.mConnectionState.postValue(GlobalWebClient.DISCONNECTED);
        String message = "<H1 style='text-align:center;'>Ошибка подключения к сети</H1>";

        ByteArrayInputStream inputStream = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            inputStream = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
        } else {
            try {
                //noinspection CharsetObjectCanBeUsed
                inputStream = new ByteArrayInputStream(message.getBytes(ENCODING_UTF_8));
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
        }
        return new WebResourceResponse("text/html", ENCODING_UTF_8, inputStream);

    }
}
