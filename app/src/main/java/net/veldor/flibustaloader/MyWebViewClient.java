package net.veldor.flibustaloader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
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
import java.io.UnsupportedEncodingException;
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

    private static final String TOR_NOT_RUNNING_ERROR = "Tor is not running!";
    static final String BOOK_LOAD_ACTION = "net.veldor.flibustaloader.action.BOOK_LOAD_EVENT";
    static final String TOR_CONNECT_ERROR_ACTION = "net.veldor.flibustaloader.action.TOR_CONNECT_ERROR";
    static final int START_BOOK_LOADING = 1;
    static final int FINISH_BOOK_LOADING = 2;
    private static final String ENCODING_UTF_8 = "UTF-8";
    private static final String BOOK_FORMAT = "application/octet-stream";
    private static final String FB2_FORMAT = "application/zip";
    private static final String PDF_FORMAT = "application/pdf";
    private static final String CSS_FORMAT = "text/css";
    private static final String JS_FORMAT = "application/x-javascript";


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
    static final String BOOK_LOAD_EVENT = "book load event";
    private static final String MY_COMPAT_CSS_STYLE = "myCompatStyle.css";
    private static final String MY_CSS_NIGHT_STYLE = "myNightMode.css";
    private static final String MY_COMPAT_FAT_CSS_STYLE = "myCompatFatStyle.css";
    private static final String MY_JS = "myJs.js";

    private final AndroidOnionProxyManager onionProxyManager;
    private int mViewMode;
    private boolean mNightMode;
    private boolean mIsBook;
    private int mScroll;

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
/*
        try {
            mViewMode = App.getInstance().getViewMode();
            mNightMode = App.getInstance().getNightMode();
            String requestString = request.getUrl().toString();

            // обрубаю загрузку картинок в упрощённом виде
            if (mViewMode > 1) {
                String[] extensionArr = requestString.split("\\.");
                if (extensionArr.length > 0) {
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
            if (mime.equals(CSS_FORMAT)) {
                Log.d("surprise", "MyWebViewClient shouldInterceptRequest: load CSS");
                InputStream is = httpResponse.getEntity().getContent();
                // подключу нужные CSS простым объединением строк
                String origin = inputStreamToString(is);
                String injectionText = injectMyCss(origin);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(injectionText.getBytes(encoding));
                return new WebResourceResponse(mime, ENCODING_UTF_8, inputStream);

            } else if (mime.equals(JS_FORMAT)) {
                InputStream is = httpResponse.getEntity().getContent();
                String origin = inputStreamToString(is);
                Log.d("surprise", "MyWebViewClient shouldInterceptRequest: origin is " + origin.length());
                String injectionText = injectMyJs(origin);
                Log.d("surprise", "MyWebViewClient shouldInterceptRequest: origin is " + injectionText.length());
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
                String[] types = requestString.split("/");
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
                        mIsBook = true;
                        return super.shouldInterceptRequest(view, request);
                    } catch (IOException e) {
                        Log.d("surprise", "some output error");
                    } finally {
                        // отправлю оповещение об окончании загрузки страницы
                        Intent finishLoadingIntent = new Intent(BOOK_LOAD_ACTION);
                        finishLoadingIntent.putExtra(BOOK_LOAD_EVENT, FINISH_BOOK_LOADING);
                        activityContext.sendBroadcast(finishLoadingIntent);
                    }
                }
            } else {
                mIsBook = false;
            }

            return new WebResourceResponse(mime, encoding, input);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage().equals(TOR_NOT_RUNNING_ERROR)) {
                try {
                    // отправлю оповещение об ошибке загрузки TOR
                    Intent finishLoadingIntent = new Intent(TOR_CONNECT_ERROR_ACTION);
                    App.getInstance().sendBroadcast(finishLoadingIntent);
                    // отображу сообщение о невозможности загрузки
                    String message = "<H1 style='text-align:center;'>Ошибка подключения к сети</H1>";
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBytes(ENCODING_UTF_8));
                    return new WebResourceResponse("text/html", ENCODING_UTF_8, inputStream);
                } catch (UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return super.shouldInterceptRequest(view, request);*/
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
        mScroll = view.getScrollY();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (!mIsBook) {
            App.getInstance().setLastLoadedUrl(url);
            //view.scrollTo(0, App.getInstance().getLastScroll());
            App.getInstance().setLastScroll(mScroll);
            /*if (mNightMode) {
                injectNightMode();
            }*/
            /*if (mViewMode > 1) {
                injectMyCss();
            }*/
        }
    }

/*    private void injectNightMode() {
        // старые версии Android не понимают переменные цветов и новые объявления JS, подключусь в режиме совместимости
        App context = App.getInstance();
        try {
            InputStream inputStream = context.getAssets().open(MY_CSS_NIGHT_STYLE);
            byte[] buffer = new byte[inputStream.available()];
            int result = inputStream.read(buffer);
            if (result == 0) {
                Log.d("surprise", "MyWebViewClient injectMyCss: read 0 bytes");
            }
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            mWebView.loadUrl("javascript:(function () {" +
                    " var parent = document.getElementsByTagName('head').item(0);" +
                    " var style = document.createElement('style');" +
                    " style.type = 'text/css';" +
                    " style.innerHTML = window.atob('" + encoded + "');" +
                    " parent.appendChild(style);" +
                    " })();");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    private String injectMyJs(String originalJs){
        String output = originalJs;
        try{
            if(mViewMode == App.VIEW_MODE_FAT || mViewMode == App.VIEW_MODE_LIGHT){
                App context = App.getInstance();
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
                    case App.VIEW_MODE_FAST:
                        inputStream = context.getAssets().open(MY_COMPAT_CSS_STYLE);
                        break;
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

            /*byte[] buffer = new byte[inputStream.available()];
            int result = inputStream.read(buffer);
            if (result == 0) {
                Log.d("surprise", "MyWebViewClient injectMyCss: read 0 bytes");
            }
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            mWebView.loadUrl("javascript:(function () {" +
                    " var parent = document.getElementsByTagName('head').item(0);" +
                    " var style = document.createElement('style');" +
                    " style.type = 'text/css';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    " style.innerHTML = window.atob('" + encoded + "');" +
                    " parent.appendChild(style);" +
                    " })();");
            if (mViewMode != App.VIEW_MODE_FAST && mViewMode != App.VIEW_MODE_FAST_FAT) {
                mWebView.loadUrl("javascript:(function () {var href = location.href; if (href === 'http://flibustahezeous3.onion/') { var menu = document.getElementsByClassName('pager'); menu[0].style.display = 'none'; } var alphabetClassName = 'alphabet-link'; var authorClassName = 'author-link'; var bookClassName = 'book-link'; var foundedBookClassName = 'book-link searched'; var selectedBookClassName = 'book-link searched selected'; var bookActionClassName = 'book-action-link'; var bookSeriesClassName = 'book-series-link'; var bookGenreClassName = 'book-genre-link'; var classHidden = 'hidden'; var forumNamesClassName = 'forum-name-link'; handleLinks(document); var target = document.getElementById('books'); if (target) { var observer = new MutationObserver(function () { handleLinks(target); }); var config = {attributes: true, childList: true, characterData: true}; observer.observe(target, config); } var books = document.getElementsByClassName(bookClassName); if (books && books.length > 0) if (books && books.length > 0) { var searchDiv, searchButton, searchField; searchDiv = document.createElement('div'); searchDiv.id = 'searchContainer'; searchField = document.createElement('input'); searchField.type = 'text'; searchField.id = 'booksSearcher'; searchField.setAttribute('placeholder', 'Искать книгу на странице'); searchButton = document.createElement('div'); searchButton.id = 'searchButton'; var innerText = 'Нет условия'; searchButton.innerText = innerText; searchDiv.appendChild(searchField); searchDiv.appendChild(searchButton); document.body.appendChild(searchDiv); var founded; var searchShift = 0; var previouslySelected; var previousSearch; searchField.onkeypress = function (event) { if (event.code === 'Enter') { switchToNext(); } }; searchField.oninput = function () { var inputVal = searchField.value.toLowerCase(); if (inputVal) { founded = []; searchShift = 0; var i = 0; while (books[i]) { if (books[i].innerText.toLowerCase().indexOf(inputVal) + 1 && books[i].offsetHeight) { founded.push(books[i]); } i++; } if (founded && founded.length > 0) { scrollTo(founded[0]); searchButton.innerText = '1 из ' + founded.length; makeBookSearched(founded); makeSearchedBookSelected(founded[0]); } else { clearSelectedItem(); clearSearchedItems(); searchButton.innerText = 'Не найдено'; } } else { clearSelectedItem(); clearSearchedItems(); searchButton.innerText = innerText; founded = []; } }; searchButton.onclick = function () { switchToNext(); }; } function makeBookSearched(foundedElement) { if (previousSearch && previousSearch.length > 0) { previousSearch.forEach(function (elem) { elem.className = bookClassName; }); } if (foundedElement && foundedElement.length > 0) { foundedElement.forEach(function (elem) { elem.className = foundedBookClassName; }); previousSearch = foundedElement; } } function makeSearchedBookSelected(elem) { if (previouslySelected) { previouslySelected.className = foundedBookClassName; } elem.className = selectedBookClassName; previouslySelected = elem; } function clearSelectedItem() { if (previouslySelected) { previouslySelected.className = bookClassName; previouslySelected = null; } } function clearSearchedItems() { if (previousSearch && previousSearch.length > 0) { previousSearch.forEach(function (elem) { elem.className = bookClassName; }); previousSearch = null; } } function switchToNext() { if (founded && founded.length > 0) { ++searchShift; if (founded[searchShift]) { scrollTo(founded[searchShift]); searchButton.innerText = (searchShift + 1) + ' из ' + founded.length; makeSearchedBookSelected(founded[searchShift]); } else { searchShift = 0; scrollTo(founded[0]); searchButton.innerText = '1 из ' + founded.length; makeSearchedBookSelected(founded[0]); } } } function scrollTo(element) { var offset = element.offsetTop - 10; window.scroll(0, offset - 10); } function handleLinks(element) { var links = element.getElementsByTagName('A'); if (links && links.length > 0) { var alphabetListRegex = /^\\/\\w{1,2}$/; var authorStarts = '/a/'; var bookStarts = '/b/'; var seriesStarts = '/s/'; var genreStarts = '/g/'; var forumNamesRegex = /^\\/polka\\/show\\/[\\d]+$/; var counter = 0; var href; var startsWith; var prelast; var current; while (links[counter]) { current = links[counter]; if (current.offsetHeight) { href = current.getAttribute('href'); startsWith = href.substr(0, 3); if (startsWith === bookStarts) { prelast = parseInt(href.substr(href.length - 2, 1) + 1); if (prelast) { current.className = bookClassName; } else { current.className = bookActionClassName; current.innerText = links[counter].innerText.replace(/[()]/g, ''); } } else if (startsWith === authorStarts) { if(href === '/a/all'){ current.className = alphabetClassName; } else{ current.className = authorClassName; } } else if (startsWith === genreStarts) { current.className = bookGenreClassName; } else if (startsWith === seriesStarts) { current.className = bookSeriesClassName; } else if (current.innerText === '(СЛЕДИТЬ)') { current.className = classHidden; } else if (alphabetListRegex.test(href) || href === '/a/all' || href === '/Other') { current.className = alphabetClassName; current.innerText = current.innerText.replace(/[\\[\\]]/g, ''); } else if (forumNamesRegex.test(href)) { current.className = forumNamesClassName; } } counter++; } } } }())");
            }*/
        } catch (IOException e) {
            Log.d("surprise", "MyWebViewClient injectMyCss: error when injecting my Js or CSS");
            e.printStackTrace();
        }
        return null;
    }

    static class FakeDnsResolver implements DnsResolver {
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

    private WebResourceResponse handleRequest(WebView view, String url){
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
            Log.d("surprise", "MyWebViewClient shouldInterceptRequest: " + mime);
            if (mime.equals(CSS_FORMAT)) {
                Log.d("surprise", "MyWebViewClient shouldInterceptRequest: load CSS");
                InputStream is = httpResponse.getEntity().getContent();
                // подключу нужные CSS простым объединением строк
                String origin = inputStreamToString(is);
                String injectionText = injectMyCss(origin);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(injectionText.getBytes(encoding));
                return new WebResourceResponse(mime, ENCODING_UTF_8, inputStream);

            } else if (mime.equals(JS_FORMAT)) {
                InputStream is = httpResponse.getEntity().getContent();
                String origin = inputStreamToString(is);
                Log.d("surprise", "MyWebViewClient shouldInterceptRequest: origin is " + origin.length());
                String injectionText = injectMyJs(origin);
                Log.d("surprise", "MyWebViewClient shouldInterceptRequest: origin is " + injectionText.length());
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
                        mIsBook = true;
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
            } else {
                mIsBook = false;
            }
            return new WebResourceResponse(mime, encoding, input);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage().equals(TOR_NOT_RUNNING_ERROR)) {
                try {
                    // отправлю оповещение об ошибке загрузки TOR
                    Intent finishLoadingIntent = new Intent(TOR_CONNECT_ERROR_ACTION);
                    App.getInstance().sendBroadcast(finishLoadingIntent);
                    // отображу сообщение о невозможности загрузки
                    String message = "<H1 style='text-align:center;'>Ошибка подключения к сети</H1>";
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBytes(ENCODING_UTF_8));
                    return new WebResourceResponse("text/html", ENCODING_UTF_8, inputStream);
                } catch (UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return super.shouldInterceptRequest(view, url);
    }
}
