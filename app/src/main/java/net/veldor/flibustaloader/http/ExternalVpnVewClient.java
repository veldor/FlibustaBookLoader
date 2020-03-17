package net.veldor.flibustaloader.http;

import android.util.Log;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

public class ExternalVpnVewClient {
    public static String request(String text) {
        CloseableHttpClient httpclient = null;
        try {
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
                        try {
                            return EntityUtils.toString(entity);
                        } catch (IOException e) {
                            Log.d("surprise", "TestHttpRequestWorker handleResponse can't get content");
                        }
                    }
                    return null;
                }
            };
            // выполню запрос
            httpclient.execute(httpget, responseHandler);
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
}
