package net.veldor.flibustaloader.http;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;

public class RequestAsyncTask extends AsyncTask<String, Integer, Integer> {


    public HttpGet httpGet;
    public HttpClient httpClient;
    public HttpClientContext context;

    @Override
    protected Integer doInBackground(String... strings) {
        try {
            httpClient.execute(httpGet, context);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        Log.d("surprise", "RequestAsyncTask onProgressUpdate 16: progress is " + values);
    }

    @Override
    protected void onPostExecute(Integer integer) {
        Log.d("surprise", "RequestAsyncTask onPostExecute 37: request executed");
        super.onPostExecute(integer);
    }
}
