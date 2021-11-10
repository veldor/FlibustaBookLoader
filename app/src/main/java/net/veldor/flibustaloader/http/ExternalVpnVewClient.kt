package net.veldor.flibustaloader.http

import android.net.Uri
import android.util.Log
import android.widget.Toast
import cz.msebera.android.httpclient.HttpHeaders
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.client.methods.HttpPost
import cz.msebera.android.httpclient.client.protocol.HttpClientContext
import cz.msebera.android.httpclient.impl.client.HttpClients
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper
import java.io.IOException
import java.net.InetSocketAddress

object ExternalVpnVewClient {
    @JvmStatic
    fun rawRequest(url: String?): HttpResponse? {
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
        }
        return null
    }

    fun loginRequest(request: String, login: String, password: String): Boolean {

        // request main page
        val mainPage = rawRequest(request)
        val text = UniversalWebClient().responseToString(mainPage)
        // get form id
        val startIndex = text!!.indexOf("form_build_id") + 19
        val formId = text.subSequence(startIndex, startIndex + 48)
        Log.d("surprise", "loginRequest: form id is $formId")

        val response: HttpResponse?
        val r = Uri.parse(request)
        val params: UrlEncodedFormEntity? = TorWebClient.get2post(r, login, password, formId.toString())
        try {
            response =
                executeRequest("http://flibusta.is/node?destination=node",
                    hashMapOf(

                    ),
                    params)
            App.instance.requestStatus.postValue(
                App.instance.getString(R.string.response_received_message)
            )
            if (response != null) {
                val cookies = response.getHeaders("set-cookie")
                if (cookies.size > 1) {
                    val cookieValue = StringBuilder()
                    for (c in cookies) {
                        val value = c.value
                        if (value.startsWith("SESS")) {
                            cookieValue.append(value.substring(0, value.indexOf(";")))
                        }
                    }
                    Log.d("surprise", "loginRequest: session cookie is $cookieValue")
                    PreferencesHandler.instance.authCookie = cookieValue.toString()
                    Log.d("surprise", "loginRequest: success login!")
                    App.instance.requestStatus.postValue(
                        App.instance.getString(R.string.success_login_message)
                    )
                    return true
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    @Throws(Exception::class)
    private fun executeRequest(
        @Suppress("SameParameterValue") url: String,
        @Suppress("SameParameterValue") headers: Map<String, String>?,
        params: UrlEncodedFormEntity?
    ): HttpResponse? {
        try {
            val httpclient = HttpClients.createSystem()
            val request = HttpPost(url)
            if (params != null) {
                request.entity = params
            }
            if (headers != null) {
                for ((key, value) in headers) {
                    request.setHeader(key, value)
                }
            }
            return httpclient.execute(request)
        } catch (e: RuntimeException) {
            Toast.makeText(App.instance, "Error request", Toast.LENGTH_LONG).show()
        }
        return null
    }
}