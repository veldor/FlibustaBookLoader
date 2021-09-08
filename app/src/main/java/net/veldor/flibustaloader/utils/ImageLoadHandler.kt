package net.veldor.flibustaloader.utils

import net.veldor.flibustaloader.App
import android.graphics.Bitmap
import net.veldor.flibustaloader.http.ExternalVpnVewClient
import cz.msebera.android.httpclient.client.protocol.HttpClientContext
import cz.msebera.android.httpclient.client.methods.HttpGet
import android.graphics.BitmapFactory
import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory
import cz.msebera.android.httpclient.config.RegistryBuilder
import net.veldor.flibustaloader.MyConnectionSocketFactory
import net.veldor.flibustaloader.MySSLConnectionSocketFactory
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager
import net.veldor.flibustaloader.MyWebViewClient.FakeDnsResolver
import cz.msebera.android.httpclient.impl.client.HttpClients
import cz.msebera.android.httpclient.ssl.SSLContexts
import java.io.IOException
import java.net.InetSocketAddress

internal object ImageLoadHandler {
    fun loadImage(s: String): Bitmap? {
        if (PreferencesHandler.instance.isExternalVpn) {
            return ExternalVpnVewClient.loadImage(s)
        } else {
            try {
                val httpClient = getNewHttpClient()
                val port: Int
                val onionProxyManager = App.instance.mLoadedTor.value!!
                port = onionProxyManager.iPv4LocalHostSocksPort
                val socksaddr = InetSocketAddress("127.0.0.1", port)
                val context = HttpClientContext.create()
                context.setAttribute("socks.address", socksaddr)
                val httpGet = HttpGet(s)
                httpGet.setHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36"
                )
                httpGet.setHeader("X-Compress", "null")
                val httpResponse = httpClient.execute(httpGet, context)
                val `is` = httpResponse.entity.content
                return BitmapFactory.decodeStream(`is`)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun getNewHttpClient(): HttpClient {
        val reg = RegistryBuilder.create<ConnectionSocketFactory>()
            .register("http", MyConnectionSocketFactory())
            .register("https", MySSLConnectionSocketFactory(SSLContexts.createSystemDefault()))
            .build()
        val cm = PoolingHttpClientConnectionManager(reg, FakeDnsResolver())
        return HttpClients.custom()
            .setConnectionManager(cm)
            .build()
    }
}