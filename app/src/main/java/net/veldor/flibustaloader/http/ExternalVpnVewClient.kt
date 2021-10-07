package net.veldor.flibustaloader.http

import android.util.Log
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.client.methods.HttpPost
import cz.msebera.android.httpclient.client.protocol.HttpClientContext
import cz.msebera.android.httpclient.impl.client.HttpClients
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.io.IOException
import java.net.InetSocketAddress

object ExternalVpnVewClient {
    @JvmStatic
    fun rawRequest(url: String?): HttpResponse? {
        Log.d("surprise", "rawRequest: make raw request $url")
        val port = 9050
        val socksaddr = InetSocketAddress("127.0.0.1", port)
        val context = HttpClientContext.create()
        context.setAttribute("socks.address", socksaddr)
        val httpclient = HttpClients.createSystem()
        val httpget = HttpGet(url)
        try {
            val authCookie = PreferencesHandler.instance.authCookie
            if (authCookie != null) {
                httpget.setHeader("Cookie", authCookie)
            }
            return httpclient.execute(httpget, context)
        } catch (e: IOException) {
            e.printStackTrace()
            GlobalWebClient.mConnectionState.postValue(GlobalWebClient.DISCONNECTED)
        }
        return null
    }

    fun loginRequest(request: String, login: String, password: String): Boolean {
        val port = 9050
        val socksaddr = InetSocketAddress("127.0.0.1", port)
        val context = HttpClientContext.create()
        context.setAttribute("socks.address", socksaddr)
        val httpclient = HttpClients.createSystem()
        val httpPost = HttpPost(request)
        return false
    }
}