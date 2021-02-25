package net.veldor.flibustaloader.http;

import android.util.Log;

import net.veldor.flibustaloader.App;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

public class MirrorRequestClient {
    private String ResponseString;

    public String request(String requestString) {
        ResponseString = null;
        //requestString = requestString.replace(App.BASE_URL, App.MIRROR_URL);
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(requestString);
        try {
            // кастомный обработчик ответов
            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    HttpEntity entity = response.getEntity();
                    try {
                        String body = EntityUtils.toString(entity);
                        if (body != null && !body.isEmpty()) {
                            ResponseString = body;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // неверный ответ с сервера
                    Log.d("surprise", "CheckUpdateWorker handleResponse: wrong update server answer");
                }
                return null;
            };
            // выполню запрос
            httpclient.execute(httpget, responseHandler);
        } catch (
                IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // по-любому закрою клиент
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ResponseString;
    }
}
