package net.veldor.flibustaloader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.receivers.BookLoadedReceiver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
    static final String BOOK_LOAD_ACTION = "net.veldor.flibustaloader.action.BOOK_LOAD_EVENT";
    public static final String TOR_CONNECT_ERROR_ACTION = "net.veldor.flibustaloader.action.TOR_CONNECT_ERROR";
    static final int START_BOOK_LOADING = 1;
    static final int FINISH_BOOK_LOADING = 2;
    private static final String ENCODING_UTF_8 = "UTF-8";
    private static final String BOOK_FORMAT = "application/octet-stream";
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
    static final String BOOK_LOAD_EVENT = "book load event";
    private static final String MY_COMPAT_CSS_STYLE = "myCompatStyle.css";
    private static final String MY_CSS_NIGHT_STYLE = "myNightMode.css";
    private static final String MY_COMPAT_FAT_CSS_STYLE = "myCompatFatStyle.css";
    private static final String JQUERY = "jquery.js";
    private static final String MY_JS = "myJs.js";
    private static final String HTML_TYPE = "text/html";
    private static final String AJAX_REQUEST = "http://flibustahezeous3.onion/makebooklist?";

    private final AndroidOnionProxyManager onionProxyManager;
    private int mViewMode;
    private boolean mNightMode;

    MyWebViewClient() {
        this.onionProxyManager = App.getInstance().mTorManager.getValue();
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

            HttpClient httpClient = getNewHttpClient();
            int port = onionProxyManager.getIPv4LocalHostSocksPort();
            InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
            HttpClientContext context = HttpClientContext.create();
            context.setAttribute("socks.address", socksaddr);
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
            httpGet.setHeader("X-Compress", "null");
            HttpResponse httpResponse = httpClient.execute(httpGet, context);

            InputStream input = httpResponse.getEntity().getContent();
            String encoding = ENCODING_UTF_8;
            String mime = httpResponse.getEntity().getContentType().getValue();

            // если загружена страница- добавлю её как последнюю загруженную
            if (mime.startsWith(HTML_TYPE)) {
                if (!url.startsWith(AJAX_REQUEST)) {
                    App.getInstance().setLastLoadedUrl(url);
                    Log.d("surprise", "MyWebViewClient handleRequest remember " + url);
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

            if (mime.equals(BOOK_FORMAT) || mime.equals(FB2_FORMAT) || mime.equals(PDF_FORMAT)) {
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
                       /* File file = new File(App.getInstance().getDownloadFolder(), name);
                        InputStream is = httpResponse.getEntity().getContent();
                        FileOutputStream outputStream = new FileOutputStream(file);
                        int read;
                        byte[] buffer = new byte[1024];
                        while ((read = is.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, read);
                        }
                        outputStream.close();
                        is.close();*/

                        DocumentFile downloadsDir = App.getInstance().getNewDownloadDir();
                        if(downloadsDir != null){
                            // проверю, не сохдан ли уже файл, если создан- удалю
                            DocumentFile existentFile = downloadsDir.findFile(name);
                            if(existentFile != null){
                                existentFile.delete();
                            }
                            DocumentFile newFile = downloadsDir.createFile(mime, name);
                            if(newFile != null){
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
                        else{
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
                        }

                        // отправлю сообщение о скачанном файле через broadcastReceiver
                        Intent intent = new Intent(activityContext, BookLoadedReceiver.class);
                        intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, name);
                        intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, type);
                        activityContext.sendBroadcast(intent);
                        /*// вернусь на ранее загруженную страницу
                        activityContext.sendBroadcast(new Intent(BOOK_LOAD_ACTION));*/
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
            if (e.getMessage() != null && e.getMessage().equals(TOR_NOT_RUNNING_ERROR)) {
                // отправлю оповещение об ошибке загрузки TOR
                Intent finishLoadingIntent = new Intent(TOR_CONNECT_ERROR_ACTION);
                App.getInstance().sendBroadcast(finishLoadingIntent);
                // отображу сообщение о невозможности загрузки
                String message = "<H1 style='text-align:center;'>Ошибка подключения к сети</H1>";
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
                return new WebResourceResponse("text/html", ENCODING_UTF_8, inputStream);
            }else{
                Log.d("surprise", "MyWebViewClient handleRequest page loading error");
                Intent finishLoadingIntent = new Intent(TOR_CONNECT_ERROR_ACTION);
                App.getInstance().sendBroadcast(finishLoadingIntent);
            }

        }
        return super.shouldInterceptRequest(view, url);
    }
}
