package net.veldor.flibustaloader.http;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;
import androidx.work.WorkManager;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyConnectionSocketFactory;
import net.veldor.flibustaloader.MySSLConnectionSocketFactory;
import net.veldor.flibustaloader.MyWebViewClient;
import net.veldor.flibustaloader.R;
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
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpHeaders;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.config.Registry;
import cz.msebera.android.httpclient.config.RegistryBuilder;
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.ssl.SSLContexts;

import static net.veldor.flibustaloader.MyWebViewClient.TOR_CONNECT_ERROR_ACTION;
import static net.veldor.flibustaloader.MyWebViewClient.TOR_NOT_RUNNING_ERROR;

public class TorWebClient {

    public static final String ERROR_DETAILS = "error details";
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
                broadcastTorError(e);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            if (e.getMessage() != null && e.getMessage().equals(TOR_NOT_RUNNING_ERROR)) {
                // отправлю оповещение об ошибке загрузки TOR
                broadcastTorError(e);
            }
        }
    }

    private static void broadcastTorError(Exception e) {
        // остановлю все задачи
        WorkManager.getInstance(App.getInstance()).cancelAllWork();
        // отправлю оповещение об ошибке загрузки TOR
        Intent finishLoadingIntent = new Intent(TOR_CONNECT_ERROR_ACTION);
        finishLoadingIntent.putExtra(ERROR_DETAILS, e.getMessage());
        App.getInstance().sendBroadcast(finishLoadingIntent);
    }


    public String request(String text) {
        Log.d("surprise", "TorWebClient request 109: request " + text);
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
            broadcastTorError(e);
            e.printStackTrace();
        }
        return null;
    }

    private HttpResponse simpleGetRequest(String url) throws IOException {
        Log.d("surprise", "TorWebClient simpleGetRequest 128: request " + url);
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
            HttpResponse response = simpleGetRequest(URLHelper.getBaseOPDSUrl() + book.link);
            // проверю, что запрос выполнен и файл не пуст. Если это не так- попорбую загрузить книгу с основного домена
            if (response == null || response.getStatusLine().getStatusCode() != 200 || response.getEntity().getContentLength() < 1) {
                Log.d("surprise", "ExternalVpnVewClient downloadBook 116: request from reserve " + URLHelper.getFlibustaIsUrl() + book.link);
                // попробую загрузку с резервного адреса
                response = simpleGetRequest(URLHelper.getFlibustaIsUrl() + book.link);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                DocumentFile newFile = FilesHandler.getDownloadFile(book);
                if (newFile != null) {
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


    public boolean login(Uri uri, String login, String password) throws Exception {
        Log.d("surprise", "TorWebClient login start logging in");
        HttpResponse response;
        UrlEncodedFormEntity params;
        params = get2post(uri, login, password);
        try {
            response = executeRequest("http://flibustahezeous3.onion/node?destination=node", null, params);
            App.getInstance().RequestStatus.postValue(App.getInstance().getString(R.string.response_received_message));
            if (response != null) {
                int status = response.getStatusLine().getStatusCode();
                Log.d("surprise", "TorWebClient login status is " + status);
                // получен ответ, попробую извлечь куку
                Header[] cookies = response.getHeaders("set-cookie");
                if (cookies.length > 1) {
                    StringBuilder cookieValue = new StringBuilder();
                    for (Header c :
                            cookies) {
                        String value = c.getValue();
                        /*if(value.startsWith("PERSISTENT_LOGIN")){
                            cookieValue.append(value.substring(0, value.indexOf(";")));
                        }
                        else */
                        if (value.startsWith("SESS")) {
                            cookieValue.append(value.substring(0, value.indexOf(";")));
                        }
                    }
                    MyPreferences.getInstance().saveLoginCookie(cookieValue.toString());
                    App.getInstance().RequestStatus.postValue(App.getInstance().getString(R.string.success_login_message));
                    return true;
                } else {
                    Log.d("surprise", "TorWebClient login no cookie :(");
                    Log.d("surprise", "TorWebClient.java 230 login: " + response.getEntity().getContent());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("surprise", "TorWebClient login error logging in");
        }
        return false;
    }

    private static UrlEncodedFormEntity get2post(Uri url, String login, String password) {
        Set<String> params = url.getQueryParameterNames();
        if (params.isEmpty()) {
            return null;
        }
        List<NameValuePair> paramsArray = new ArrayList<>();
        paramsArray.add(new BasicNameValuePair("openid_identifier", null));
        paramsArray.add(new BasicNameValuePair("name", login));
        paramsArray.add(new BasicNameValuePair("pass", password + "/"));
        paramsArray.add(new BasicNameValuePair("persistent_login", "1"));
        paramsArray.add(new BasicNameValuePair("op", "Вход в систему"));
        paramsArray.add(new BasicNameValuePair("form_build_id", "form-sIt20MHWRjpMKIvxdtHOGqLAa4D2GiBnFIXke7LXv7Y"));
        paramsArray.add(new BasicNameValuePair("form_id", "user_login_block"));
        paramsArray.add(new BasicNameValuePair("return_to", "http://flibustahezeous3.onion/openid/authenticate?destination=node"));
        try {
            return new UrlEncodedFormEntity(paramsArray, "utf8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }

    private HttpResponse executeRequest(String url, Map<String, String> headers, UrlEncodedFormEntity params) throws Exception {
        try {
            AndroidOnionProxyManager tor = App.getInstance().mTorManager.getValue();
            if (tor != null) {
                HttpClient httpClient = getNewHttpClient();
                int port = tor.getIPv4LocalHostSocksPort();
                InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", port);
                HttpClientContext clientContext = HttpClientContext.create();
                clientContext.setAttribute("socks.address", socketAddress);

                HttpPost request = new HttpPost(url);

                if (params != null) {
                    request.setEntity(params);
                }
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        request.setHeader(entry.getKey(), entry.getValue());
                    }
                }
                request.setHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
                request.setHeader(HttpHeaders.ACCEPT_ENCODING, "ggzip, deflate");
                request.setHeader(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
                request.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
                request.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
                request.setHeader("DNT", "1");
                request.setHeader(HttpHeaders.HOST, "flibustahezeous3.onion");
                request.setHeader("Origin", "http://flibustahezeous3.onion");
                request.setHeader(HttpHeaders.PRAGMA, "no-cache");
                request.setHeader("Proxy-Connection", "keep-ali e");
                request.setHeader("Upgrade-Insecure-Requests", "1");
                request.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.116 Safari/537.36");
                return httpClient.execute(request, clientContext);
            }
        } catch (RuntimeException e) {
            Toast.makeText(App.getInstance(), "Error request", Toast.LENGTH_LONG).show();
        }
        return null;
    }
}
