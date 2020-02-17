package net.veldor.flibustaloader.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyConnectionSocketFactory;
import net.veldor.flibustaloader.MySSLConnectionSocketFactory;
import net.veldor.flibustaloader.MyWebViewClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.config.Registry;
import cz.msebera.android.httpclient.config.RegistryBuilder;
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager;
import cz.msebera.android.httpclient.ssl.SSLContexts;

public class ImageLoadHandler {
    public static Bitmap loadImage(String s) {
        try {
        HttpClient httpClient = getNewHttpClient();
        int port;
        AndroidOnionProxyManager onionProxyManager = App.getInstance().mTorManager.getValue();
        assert onionProxyManager != null;
            port = onionProxyManager.getIPv4LocalHostSocksPort();
        InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
        HttpClientContext context = HttpClientContext.create();
        context.setAttribute("socks.address", socksaddr);
        HttpGet httpGet = new HttpGet(s);
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
        httpGet.setHeader("X-Compress", "null");
        HttpResponse httpResponse = httpClient.execute(httpGet, context);
            InputStream is = httpResponse.getEntity().getContent();
            return BitmapFactory.decodeStream(is);
    } catch (IOException e) {
        e.printStackTrace();
    }
        return null;
    }

    private static HttpClient getNewHttpClient() {
        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new MyConnectionSocketFactory())
                .register("https", new MySSLConnectionSocketFactory(SSLContexts.createSystemDefault()))
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg, new MyWebViewClient.FakeDnsResolver());
        return HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }
}
