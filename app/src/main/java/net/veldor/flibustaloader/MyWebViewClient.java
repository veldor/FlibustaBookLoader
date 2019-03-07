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

import java.io.ByteArrayInputStream;
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

    public static final File DOWNLOAD_FOLDER_LOCATION = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    static final String BOOK_LOAD_ACTION = "net.veldor.flibustaloader.action.BOOK_LOAD_EVENT";
    static final int START_BOOK_LOADING = 1;
    static final int FINISH_BOOK_LOADING = 2;
    private static final String ENCODING_UTF_8 = "UTF-8";
    private static final String BOOK_FORMAT = "application/octet-stream";
    private static final String FB2_FORMAT = "application/zip";
    private static final String PDF_FORMAT = "application/pdf";


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

            if (mime.equals(BOOK_FORMAT) || mime.equals(FB2_FORMAT) || mime.equals(PDF_FORMAT)) {
                Context activityContext = view.getContext();
                Header header = httpResponse.getFirstHeader(HEADER_CONTENT_DISPOSITION);
                String name = header.getValue().split(FILENAME_DELIMITER)[1];
                name = name.replace("\"", "");
                String[] extensionSource = name.split("\\.");
                String extension = extensionSource[extensionSource.length - 1];
                String[] types = requestString.split("/");
                String type = types[types.length - 1];
                if(mime.equals(PDF_FORMAT)){
                    type = PDF_TYPE;
                }
                if(extension.equals(DJVU_TYPE)){
                    type = DJVU_TYPE;
                }
                if (type.equals(FB2_TYPE) || type.equals(MOBI_TYPE) || type.equals(EPUB_TYPE) || type.equals(PDF_TYPE) || type.equals(DJVU_TYPE)) {
                    try {
                        // начинаю загружать книку, пошлю оповещение о начале загрузки
                        Intent startLoadingIntent = new Intent(BOOK_LOAD_ACTION);
                        startLoadingIntent.putExtra(BOOK_LOAD_EVENT, START_BOOK_LOADING);
                        activityContext.sendBroadcast(startLoadingIntent);
                        // сохраняю книгу в памяти устройства
                        File file = new File(DOWNLOAD_FOLDER_LOCATION, name);
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
                        String message = "<H1 style='text-align:center;'>Книга закачана. Возвращаюсь на предыдущую страницу</H1>";
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBytes(encoding));
                        return new WebResourceResponse("text/html", ENCODING_UTF_8, inputStream);
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
            App.getInstance().currentLoadedUrl = url;
        }
        if (mViewMode) {
            injectMyCss();
        }
    }

    private void injectMyCss() {
        App context = App.getInstance();
        try {
            // попробую добавить отдельно css и отдельно js
            InputStream inputStream = context.getAssets().open(MY_CSS_STYLE);
            byte[] buffer = new byte[inputStream.available()];
            int result = inputStream.read(buffer);
            if(result == 0){
                Log.d("surprise", "MyWebViewClient injectMyCss: readed 0 bytes");
            }
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            mWebView.loadUrl("javascript:(function () {" +
                    " /*подключу свой файл CSS*/" +
                    " let parent = document.getElementsByTagName('head').item(0);" +
                    " let style = document.createElement('style');" +
                    " style.type = 'text/css';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    " style.innerHTML = window.atob('" + encoded + "');" +
                    " parent.appendChild(style);" +
                    " })();");

            mWebView.loadUrl("javascript:(function () { let alphabetClassName = 'alphabet-link'; let authorClassName = 'author-link'; let bookClassName = 'book-link'; let foundedBookClassName = 'book-link searched'; let selectedBookClassName = 'book-link searched selected'; let bookActionClassName = 'book-action-link'; let bookSeriesClassName = 'book-series-link'; let bookGenreClassName = 'book-genre-link'; let classHidden = 'hidden'; let forumNamesClassName = 'forum-name-link'; function handleLinks(element) {let links = element.getElementsByTagName('A'); if (links && links.length > 0) {let alphabetListRegex = /^\\/\\w{1,2}$/; let authorRegex = /^\\/a\\/\\d+$/; let bookRegex = /^\\/b\\/\\d+$/; let bookSeriesRegex = /^\\/s\\/\\d+$/; let bookGenreRegex = /^\\/g\\/[\\d_\\w]+$/; let bookActonRegex = /^\\/b\\/\\d+\\/\\w+$/; let forumNamesRegex = /^\\/polka\\/show\\/[\\d]+$/; let counter = 0; let href; while (links[counter]) { if(links[counter].offsetHeight){ href = links[counter].getAttribute('href'); if (bookRegex.test(href)) { links[counter].className = bookClassName; } else if (bookActonRegex.test(href)) {links[counter].className = bookActionClassName; links[counter].innerText = links[counter].innerText.replace(/[()]/g, ''); }else if (authorRegex.test(href)) { links[counter].className = authorClassName; } else if (bookSeriesRegex.test(href)) { links[counter].className = bookSeriesClassName; } else if (bookGenreRegex.test(href)) { links[counter].className = bookGenreClassName; } else if (links[counter].innerText === '(СЛЕДИТЬ)') { links[counter].className = classHidden; }else if (alphabetListRegex.test(href) || href === '/a/all' || href === '/Other') { links[counter].className = alphabetClassName; links[counter].innerText = links[counter].innerText.replace(/[\\[\\]]/g, ''); } else if (forumNamesRegex.test(href)) { links[counter].className = forumNamesClassName; } } counter++; } } } handleLinks(document);let target = document.getElementById('books'); if (target) {let observer = new MutationObserver(function () { handleLinks(target); });let config = {attributes: true, childList: true, characterData: true};observer.observe(target, config);} let books = document.getElementsByClassName(bookClassName); if(books && books.length > 0)if (books && books.length > 0) { let searchDiv, searchButton, searchField; searchDiv = document.createElement('div'); searchDiv.id = 'searchContainer'; searchField = document.createElement('input'); searchField.type = 'text'; searchField.id = 'booksSearcher'; searchField.setAttribute('placeholder', 'Искать книгу на странице'); searchButton = document.createElement('div'); searchButton.id = 'searchButton'; let innerText = 'Нет условия'; searchButton.innerText = innerText; searchDiv.appendChild(searchField); searchDiv.appendChild(searchButton); document.body.appendChild(searchDiv); let founded; let searchShift = 0; let previouslySelected; let previousSearch; function makeBookSearched(foundedElement) {if (previousSearch && previousSearch.length > 0) { previousSearch.forEach(function (elem) { elem.className = bookClassName; }); } if (foundedElement && foundedElement.length > 0) { foundedElement.forEach(function (elem) { elem.className = foundedBookClassName; }); previousSearch = foundedElement; } } function makeSearchedBookSelected(elem) { if (previouslySelected) { previouslySelected.className = foundedBookClassName; } elem.className = selectedBookClassName; previouslySelected = elem; } function clearSelectedItem() { if(previouslySelected){ previouslySelected.className = bookClassName; previouslySelected = null; } } function clearSearchedItems() { if (previousSearch && previousSearch.length > 0) { previousSearch.forEach(function (elem) { elem.className = bookClassName; }); previousSearch = null; } } searchField.onkeypress = function (event) { if (event.code === 'Enter') { switchToNext(); } }; searchField.oninput = function () { let inputVal = searchField.value.toLowerCase(); if (inputVal) { founded = []; searchShift = 0; let i = 0; while (books[i]) { if (books[i].innerText.toLowerCase().indexOf(inputVal) + 1 && books[i].offsetHeight) { founded.push(books[i]); } i++; } if (founded && founded.length > 0) {scrollTo(founded[0]); searchButton.innerText = '1 из ' + founded.length; makeBookSearched(founded); makeSearchedBookSelected(founded[0]); } else { clearSelectedItem(); clearSearchedItems(); searchButton.innerText = 'Не найдено'; } } else { clearSelectedItem(); clearSearchedItems(); searchButton.innerText = innerText; founded = []; } }; searchButton.onclick = function () { switchToNext(); }; function switchToNext() { if (founded && founded.length > 0) { ++searchShift; if (founded[searchShift]) { scrollTo(founded[searchShift]); searchButton.innerText = (searchShift + 1) + ' из ' + founded.length; makeSearchedBookSelected(founded[searchShift]); } else { searchShift = 0; scrollTo(founded[0]); searchButton.innerText = '1 из ' + founded.length; makeSearchedBookSelected(founded[0]); } } } function scrollTo(element) { let offset = element.offsetTop - 10; window.scroll(0, offset - 10); } } }())");
        } catch (IOException e) {
            Log.d("surprise", "MyWebViewClient injectMyCss: error when injecting my Js or CSS");
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
