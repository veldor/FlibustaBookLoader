package net.veldor.flibustaloader.http;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.ecxeptions.BookNotFoundException;
import net.veldor.flibustaloader.ecxeptions.FlibustaUnreachableException;
import net.veldor.flibustaloader.utils.URLHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                @Override
                public String handleResponse(HttpResponse response) {
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
                }
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

    public static boolean downloadBook(BooksDownloadSchedule book) {
        try {
            // получу имя файла
            DocumentFile downloadsDir = App.getInstance().getDownloadDir();
            DocumentFile newFile;
            newFile = downloadsDir.createFile(book.format, book.name);
            if (newFile != null) {
                // запрошу данные
                HttpResponse response = rawRequest(URLHandler.getBaseUrl() + book.link);
                if (response != null) {
                    int status = response.getStatusLine().getStatusCode();
                    Log.d("surprise", "ExternalVpnVewClient downloadBook status is " + status);
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
        } catch (FlibustaUnreachableException e) {
            e.printStackTrace();
        } catch (BookNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static HttpResponse rawRequest(String url) {
        int port = 9050;
        InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
        HttpClientContext context = HttpClientContext.create();
        context.setAttribute("socks.address", socksaddr);
        CloseableHttpClient httpclient = HttpClients.createSystem();
        HttpGet httpget = new HttpGet(url);
        try {
            return httpclient.execute(httpget, context);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
