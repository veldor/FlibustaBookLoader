package net.veldor.flibustaloader.http;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.ecxeptions.BookNotFoundException;
import net.veldor.flibustaloader.utils.FilesHandler;
import net.veldor.flibustaloader.utils.MyPreferences;
import net.veldor.flibustaloader.utils.URLHelper;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

public class ExternalVpnVewClient {
    public static String request(String text) {
        Log.d("surprise", "ExternalVpnVewClient request: search " + text);
        CloseableHttpClient httpclient = null;
        try {
            int port = 9050;
            InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
            HttpClientContext context = HttpClientContext.create();
            context.setAttribute("socks.address", socksaddr);
            httpclient = HttpClients.createDefault();
            HttpGet httpget = new HttpGet(text);
            // кастомный обработчик ответов
            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                Log.d("surprise", "TestHttpRequestWorker handleResponse status is " + status);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    Log.d("surprise", "ExternalVpnVewClient handleResponse: have answer");
                    try {
                        Log.d("surprise", "ExternalVpnVewClient handleResponse: returning answer");
                        return EntityUtils.toString(entity);
                    } catch (IOException e) {
                        Log.d("surprise", "ExternalVpnVewClient handleResponse: can't connect " + e.getMessage());
                    }
                }
                Log.d("surprise", "ExternalVpnVewClient handleResponse: can't have answer");
                return null;
            };
            // выполню запрос
            return httpclient.execute(httpget, responseHandler, context);
        } catch (IOException e) {
            Log.d("surprise", "TestHttpRequestWorker doWork have error in request: " + e.getMessage());
        } finally {
            try {
                // по-любому закрою клиент
                if (httpclient != null) {
                    httpclient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Bitmap loadImage(String s) {
        Log.d("surprise", "ExternalVpnVewClient raw request: " + s);
        CloseableHttpClient httpclient = null;
        try {
            httpclient = HttpClients.createSystem();
            HttpGet httpget = new HttpGet(s);
            // выполню запрос
            CloseableHttpResponse response = httpclient.execute(httpget);
            return BitmapFactory.decodeStream(response.getEntity().getContent());
        } catch (IOException e) {
            Log.d("surprise", "TestHttpRequestWorker doWork have error in request: " + e.getMessage());
        } finally {
            try {
                // по-любому закрою клиент
                if (httpclient != null) {
                    httpclient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void downloadBook(BooksDownloadSchedule book) throws BookNotFoundException {
        Log.d("surprise", "ExternalVpnVewClient downloadBook 112: request " + URLHelper.getBaseUrl() + book.link);
        HttpResponse response = rawRequest(URLHelper.getBaseUrl() + book.link);
        if (response == null || response.getStatusLine().getStatusCode() != 200 || response.getEntity().getContentLength() < 1) {
            // попробую загрузку с резервного адреса
            Log.d("surprise", "ExternalVpnVewClient downloadBook 116: request from reserve " + URLHelper.getFlibustaIsUrl() + book.link);
            response = rawRequest(URLHelper.getFlibustaIsUrl() + book.link);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                DocumentFile newFile = FilesHandler.getDownloadFile(book);
                if (newFile != null) {
                    // запрошу данные
                    Log.d("surprise", "TorWebClient downloadBook: request " + book.link + " of book " + book.name);
                    GlobalWebClient.handleBookLoadRequest(response, newFile);
                }
            } catch (Exception e) {
                try {
                    File file = FilesHandler.getCompatDownloadFile(book);
                    GlobalWebClient.handleBookLoadRequest(response, file);
                } catch (Exception e1) {
                    // скачаю файл просто в папку загрузок
                    File file = FilesHandler.getBaseDownloadFile(book);
                    GlobalWebClient.handleBookLoadRequest(response, file);
                }
            }
        } else {
            File file = FilesHandler.getCompatDownloadFile(book);
            GlobalWebClient.handleBookLoadRequest(response, file);
        }
    }

    public static HttpResponse rawRequest(String url) {
        int port = 9050;
        InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
        HttpClientContext context = HttpClientContext.create();
        context.setAttribute("socks.address", socksaddr);
        CloseableHttpClient httpclient = HttpClients.createSystem();
        HttpGet httpget = new HttpGet(url);
        try {
            String authCookie = MyPreferences.getInstance().getAuthCookie();
            if (authCookie != null) {
                httpget.setHeader("Cookie", authCookie);
            }
            return httpclient.execute(httpget, context);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
