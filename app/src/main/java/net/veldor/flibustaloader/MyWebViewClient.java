package net.veldor.flibustaloader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.receivers.BookLoadedReceiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

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

    static final String BOOK_LOAD_ACTION = "net.veldor.flibustaloader.action.BOOK_LOAD_EVENT";
    static final int START_BOOK_LOADING = 1;
    static final int FINISH_BOOK_LOADING = 2;
    private static final String ENCODING_UTF_8 = "UTF-8";
    private static final String BOOK_FORMAT = "application/octet-stream";
    private static final String FB2_FORMAT = "application/zip";


    // content types
    public static final String FB2_TYPE = "fb2";
    public static final String MOBI_TYPE = "mobi";
    public static final String EPUB_TYPE = "epub";
    public static final String PDF_TYPE = "pdf";
    public static final String DJVU_TYPE = "djvu";

    private static final String JPG_TYPE = "jpg";
    private static final String JPEG_TYPE = "jpeg";
    private static final String GIF_TYPE = "gif";
    private static final String PNG_TYPE = "png";


    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String FILENAME_DELIMITER = "filename=";
    private static final String MY_CSS_STYLE = "myStyle.css";
    static final String BOOK_LOAD_EVENT = "book load event";

    private final AndroidOnionProxyManager onionProxyManager;
    private final WebView mWebView;
    private boolean mViewMode;

    MyWebViewClient(WebView webView) {
        this.onionProxyManager = App.getInstance().mTorManager.getValue();
        this.mWebView = webView;
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        mViewMode = App.getInstance().getViewMode();
        try {
            String requestString = request.getUrl().toString();

            // обрубаю загрузку картинок в упрощённом виде
            if(mViewMode){
                String[] extensionArr = requestString.split("\\.");
                if(extensionArr.length > 0){
                    String extension = extensionArr[extensionArr.length - 1];
                    if (extension.equals(JPG_TYPE) || extension.equals(JPEG_TYPE) || extension.equals(PNG_TYPE) || extension.equals(GIF_TYPE)) {
                        return super.shouldInterceptRequest(view, request);
                    }
                }
            }

            HttpClient httpClient = getNewHttpClient();
            int port = onionProxyManager.getIPv4LocalHostSocksPort();
            InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
            HttpClientContext context = HttpClientContext.create();
            context.setAttribute("socks.address", socksaddr);
            HttpGet httpGet = new HttpGet(requestString);
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
            httpGet.setHeader("X-Compress", "null");
            HttpResponse httpResponse = httpClient.execute(httpGet, context);

            InputStream input = httpResponse.getEntity().getContent();
            String encoding = ENCODING_UTF_8;
            String mime = httpResponse.getEntity().getContentType().getValue();
            if (mime.contains(";")) {
                String[] arr = mime.split(";");
                mime = arr[0];
                arr = arr[1].split("=");
                encoding = arr[1];
            }

            if (mime.equals(BOOK_FORMAT) || mime.equals(FB2_FORMAT)) {
                Context activityContext = view.getContext();
                // получу расширение файла
                String[] types = requestString.split("/");
                String type = types[types.length - 1];
                if (type.equals(FB2_TYPE) || type.equals(MOBI_TYPE) || type.equals(EPUB_TYPE) || type.equals(PDF_TYPE) || type.equals(DJVU_TYPE)) {
                    try {
                        Log.d("surprise", "MyWebViewClient shouldInterceptRequest: start broadcast sended");
                        // начинаю загружать книку, пошлю оповещение о начале загрузки
                        Intent startLoadingIntent = new Intent(BOOK_LOAD_ACTION);
                        startLoadingIntent.putExtra(BOOK_LOAD_EVENT, START_BOOK_LOADING);
                        activityContext.sendBroadcast(startLoadingIntent);

                        Header header = httpResponse.getFirstHeader(HEADER_CONTENT_DISPOSITION);
                        String name = header.getValue().split(FILENAME_DELIMITER)[1];
                        // сохраняю книгу в памяти устройства
                        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(dir, name);
                        InputStream is = httpResponse.getEntity().getContent();
                        FileOutputStream outputStream = new FileOutputStream(file);
                        int read;
                        byte[] buffer = new byte[1024];
                        while ((read = is.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, read);
                        }
                        outputStream.close();
                        is.close();
                        // отправлю сообщение о скачанном файле через broadcastReceiver
                        Intent intent = new Intent(activityContext, BookLoadedReceiver.class);
                        intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, name);
                        intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, type);
                        activityContext.sendBroadcast(intent);
                        /*// вернусь на ранее загруженную страницу
                        activityContext.sendBroadcast(new Intent(BOOK_LOAD_ACTION));*/
                        return super.shouldInterceptRequest(view, request);
                    } catch (IOException e) {
                        Log.d("surprise", "some output error");
                    } finally {
                        // отправлю оповещение об окончании загрузки страницы
                        Intent finishLoadingIntent = new Intent(BOOK_LOAD_ACTION);
                        finishLoadingIntent.putExtra(BOOK_LOAD_EVENT, FINISH_BOOK_LOADING);
                        activityContext.sendBroadcast(finishLoadingIntent);
                        Log.d("surprise", "MyWebViewClient shouldInterceptRequest: finish broadcast sended");
                    }
                }
            }

            return new WebResourceResponse(mime, encoding, input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.shouldInterceptRequest(view, request);
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

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if(url.startsWith(App.BASE_URL)){
            Log.d("surprise", "MyWebView loadUrl: save current url " + url);
            App.getInstance().currentLoadedUrl = url;
        }
        if (mViewMode) {
            injectMyCss();
        }
    }

    private void injectMyCss() {
        App context = App.getInstance();
        try {
            InputStream inputStream = context.getAssets().open(MY_CSS_STYLE);
            byte[] buffer = new byte[inputStream.available()];
            int result = inputStream.read(buffer);
            if(result == 0){
                Log.d("surprise", "MyWebViewClient injectMyCss: readed 0 bytes");
            }
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            mWebView.loadUrl("javascript:( function () { /*подключу свой файл CSS*/ let parent = document.getElementsByTagName('head').item(0); let style = document.createElement('style'); style.type = 'text/css'; style.innerHTML = window.atob('" + encoded + "'); parent.appendChild(style); /*обработаю ссылки в документе- уберу лишние символы, добавлю ссылкам на скачивание отдельный класс*/ let links = document.getElementsByTagName('a'); let i = 0; let elem; while (links[i]) { let linkText = links[i].textContent; links[i].textContent = linkText.replace('[', '').replace(']', '').replace('(', '').replace(')', ''); if (linkText === '(fb2)' || linkText === '(скачать pdf)' || linkText === '(скачать djvu)' || linkText === '(epub)' || linkText === '(mobi)') { links[i].className = 'download-link'; if (linkText === '(fb2)' || linkText === '(скачать pdf)' || linkText === '(скачать djvu)') { elem = document.createElement('br'); links[i].parentNode.insertBefore(elem, links[i]); } } i++; } let hasBooks = false; /*скрою чекбоксы, объявлю ссылку за ними названием книги*/ let checkboxes = document.getElementsByTagName('input'); i = 0; while (checkboxes[i]) { elem = checkboxes[i]; if (elem.getAttribute('type') === 'checkbox') { elem.style.display = \"none\"; let next = elem.nextSibling; while (next && next.nodeName !== 'A') { next = next.nextSibling; } if (next && next.nodeName === 'A') { next.className = 'book-name'; hasBooks = true; } } i++; } /*удалю все изображения*/ let images = document.getElementsByTagName('img'); i = 0; while (images[i]) { if(images[i]){ images[i].style.display = \"none\"; } i++; } let svgImages = document.getElementsByTagName('svg'); i = 0; while (svgImages[i]) { if(svgImages[i]){ svgImages[i].style.display = \"none\"; } i++; } let searchDiv; let searchField; let searchButton; if (hasBooks) { let books = document.getElementsByClassName('book-name'); searchDiv = document.createElement('div'); searchDiv.id = 'searchContainer'; searchField = document.createElement('input'); searchField.type = 'text'; searchField.id = 'booksSearcher'; searchField.setAttribute('placeholder', 'Искать книгу'); searchButton = document.createElement('button'); searchButton.id = 'searchButton'; searchButton.innerText = 'Искать далее'; searchDiv.appendChild(searchField); searchDiv.appendChild(searchButton); document.body.appendChild(searchDiv); let previoslySearched; let searchShift = 0; searchField.oninput = function () { searchButton.innerText = 'искать далее'; if (previoslySearched) previoslySearched.className = 'book-name'; let inputVal = searchField.value.toLowerCase(); if (inputVal) { let i = 0; while (books[i]) { if (books[i].innerText.toLowerCase().indexOf(inputVal) + 1) { books[i].className = 'book-name searched'; let offsetTop = books[i].offsetTop; window.scroll(0, offsetTop - 10); previoslySearched = books[i]; break; } i++; } } }; searchButton.onclick = function () { searchButton.innerText = 'искать далее'; ++searchShift; let thisShift = searchShift; let inputVal = searchField.value.toLowerCase(); if (inputVal.length > 0) { let i = 0; while (books[i]) { if (books[i].innerText.toLowerCase().indexOf(inputVal) + 1) { --thisShift; if (thisShift === 0) { if (previoslySearched) previoslySearched.className = 'book-name'; books[i].className = 'book-name searched'; let offsetTop = books[i].offsetTop; window.scroll(0, offsetTop - 10); previoslySearched = books[i]; break; } } i++; } if (i === books.length) { searchButton.innerText = 'Найден последний элемент'; searchShift = 0; } } } } })();");
        } catch (IOException e) {
            Log.d("surprise", "MyWebViewClient injectMyCss: error when injecting my CSS");
            e.printStackTrace();
        }
    }


    static class FakeDnsResolver implements DnsResolver {
        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            return new InetAddress[]{InetAddress.getByAddress(new byte[]{1, 1, 1, 1})};
        }
    }
}
