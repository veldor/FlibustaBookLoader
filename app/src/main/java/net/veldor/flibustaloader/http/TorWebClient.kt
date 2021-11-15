package net.veldor.flibustaloader.http

import android.net.Uri
import android.util.Log
import android.widget.Toast
import cz.msebera.android.httpclient.HttpHeaders
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.NameValuePair
import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.client.methods.HttpPost
import cz.msebera.android.httpclient.client.protocol.HttpClientContext
import cz.msebera.android.httpclient.config.RegistryBuilder
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory
import cz.msebera.android.httpclient.impl.client.HttpClients
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager
import cz.msebera.android.httpclient.message.BasicNameValuePair
import cz.msebera.android.httpclient.ssl.SSLContexts
import net.veldor.flibustaloader.*
import net.veldor.flibustaloader.MyWebViewClient.FakeDnsResolver
import net.veldor.flibustaloader.ecxeptions.ConnectionLostException
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper
import net.veldor.flibustaloader.view_models.OPDSViewModel
import java.io.*
import java.net.InetSocketAddress
import java.util.*

class TorWebClient {
    private lateinit var mHttpClient: HttpClient
    private lateinit var mContext: HttpClientContext

    @get:Throws(ConnectionLostException::class)
    private val connectionError: Unit
        get() {
            throw ConnectionLostException()
        }

    fun request(incomingText: String): String? {
        try {
            val httpGet = HttpGet(incomingText)
            httpGet.setHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36"
            )
            httpGet.setHeader("X-Compress", "null")
            val httpResponse = mHttpClient.execute(httpGet, mContext)
            val `is`: InputStream = httpResponse.entity.content
            return inputStreamToString(`is`)
        } catch (e: IOException) {
            //broadcastTorError(e);
            e.printStackTrace()
        }
        return null
    }

    fun requestNoMirror(text: String?): String? {
        try {
            val httpGet = HttpGet(text)
            httpGet.setHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36"
            )
            httpGet.setHeader("X-Compress", "null")
            val httpResponse = mHttpClient.execute(httpGet, mContext)
            val `is`: InputStream = httpResponse.entity.content
            return inputStreamToString(`is`)
        } catch (e: IOException) {
            //broadcastTorError(e);
            e.printStackTrace()
        }
        return null
    }

    @Throws(java.lang.Exception::class)
    fun directRequest(text: String?): String? {
        Log.d("surprise", "directRequest: make direct request $text")
        val httpGet = HttpGet(text)
        httpGet.setHeader(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36"
        )
        httpGet.setHeader("X-Compress", "null")
        val httpResponse = mHttpClient.execute(httpGet, mContext)
        Log.d("surprise", "directRequest:response status is ${httpResponse.statusLine.statusCode}")
        val `is`: InputStream = httpResponse.entity.content
        return inputStreamToString(`is`)
    }

    @Throws(IOException::class)
    fun simpleGetRequest(incomingUrl: String): HttpResponse {
        Log.d("surprise", "simpleGetRequest: load $incomingUrl")
        val httpGet = HttpGet(incomingUrl)
        httpGet.setHeader(
            "Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
        )
        httpGet.setHeader(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36"
        )
        httpGet.setHeader("X-Compress", "null")
        return mHttpClient.execute(httpGet, mContext)
    }

    private val newHttpClient: HttpClient
        get() {
            val reg = RegistryBuilder.create<ConnectionSocketFactory>()
                .register("http", MyConnectionSocketFactory())
                .register("https", MySSLConnectionSocketFactory(SSLContexts.createSystemDefault()))
                .build()
            val cm = PoolingHttpClientConnectionManager(reg, FakeDnsResolver())
            return HttpClients.custom()
                .setConnectionManager(cm)
                .build()
        }

    private fun inputStreamToString(`is`: InputStream): String? {
        try {
            val r = BufferedReader(InputStreamReader(`is`))
            val total = StringBuilder()
            var line: String?
            while (r.readLine().also { line = it } != null) {
                total.append(line).append('\n')
            }
            return total.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    @Throws(Exception::class)
    fun login(uri: Uri, login: String, password: String): Boolean {
        // request main page
        val mainPage = rawRequest(URLHelper.getFlibustaUrl())
        val text = UniversalWebClient().responseToString(mainPage)
        // get form id
        val startIndex = text!!.indexOf("form_build_id") + 19
        val formId = text.subSequence(startIndex, startIndex + 48)
        Log.d("surprise", "loginRequest: form id is $formId")

        val response: HttpResponse?
        val params: UrlEncodedFormEntity? = get2post(uri, login, password, formId.toString())
        try {
            response =
                executeRequest(
                    URLHelper.getFlibustaUrl() + "/node?destination=node",
                    hashMapOf(

                )
                    , params)
            App.instance.requestStatus.postValue(
                App.instance.getString(R.string.response_received_message)
            )
            if (response != null) {
                Log.d("surprise", "login: status is ${response.statusLine.statusCode}")
                val cookies = response.getHeaders("set-cookie")
                if (cookies.size > 1) {
                    val cookieValue = StringBuilder()
                    for (c in cookies) {
                        Log.d("surprise", "login: ${c.name} : ${c.value}")
                        val value = c.value
                        if (value.startsWith("SESS")) {
                            cookieValue.append(value.substring(0, value.indexOf(";")))
                        }
                    }
                    Log.d("surprise", "loginRequest: session cookie is $cookieValue")
                    PreferencesHandler.instance.authCookie = cookieValue.toString()
                    Log.d("surprise", "login: success login")
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
            val tor = App.instance.mLoadedTor.value
            if (tor != null) {
                val httpClient = newHttpClient
                val port = tor.iPv4LocalHostSocksPort
                val socketAddress = InetSocketAddress("127.0.0.1", port)
                val clientContext = HttpClientContext.create()
                clientContext.setAttribute("socks.address", socketAddress)
                val request = HttpPost(url)
                if (params != null) {
                    request.entity = params
                }
                if (headers != null) {
                    for ((key, value) in headers) {
                        request.setHeader(key, value)
                    }
                }
                Log.d("surprise", "executeRequest: post on $url")
                return httpClient.execute(request, clientContext)
            }
        } catch (e: RuntimeException) {
            Toast.makeText(App.instance, "Error request", Toast.LENGTH_LONG).show()
        }
        return null
    }

    fun rawRequest(link: String): HttpResponse? {
        OPDSViewModel.currentRequestState.postValue("Отправляю запрос")
        val httpGet = HttpGet(link)
        httpGet.setHeader(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36"
        )
        val authCookie = PreferencesHandler.instance.authCookie
        if (authCookie != null) {
            httpGet.setHeader("Cookie", authCookie)
        }
        httpGet.setHeader("X-Compress", "null")
        return mHttpClient.execute(httpGet, mContext)
    }

    companion object {
        const val ERROR_DETAILS = "error details"
        fun get2post(url: Uri, login: String, password: String, formId: String): UrlEncodedFormEntity? {
            val paramsArray: MutableList<NameValuePair> = ArrayList()
            paramsArray.add(BasicNameValuePair("openid_identifier", null))
            paramsArray.add(BasicNameValuePair("name", login))
            paramsArray.add(BasicNameValuePair("pass", password))
            paramsArray.add(BasicNameValuePair("persistent_login", "1"))
            paramsArray.add(BasicNameValuePair("op", "Вход в систему"))
            paramsArray.add(BasicNameValuePair("form_build_id", formId))
            paramsArray.add(BasicNameValuePair("form_id", "user_login_block"))
            paramsArray.add(BasicNameValuePair("return_to",URLHelper.getFlibustaUrl() + "/openid/authenticate?destination=node"))
            try {
                return UrlEncodedFormEntity(paramsArray, "utf8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            return null
        }
    }

    init {
        while (TorStarter.liveTorLaunchState.value == TorStarter.TOR_LAUNCH_IN_PROGRESS) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        val starter = TorStarter()
        while (TorStarter.torStartTry < 4) {
            // есть три попытки, если все три неудачны- верну ошибку
            if (starter.startTor()) {
                TorStarter.liveTorLaunchState.postValue(TorStarter.TOR_LAUNCH_SUCCESS)
                // обнулю счётчик попыток
                TorStarter.torStartTry = 0
                break
            }
            Log.d("surprise", "doWork: failed tor start try")
            // попытка неудачна, плюсую счётчик попыток
            TorStarter.torStartTry++
        }
        if (TorStarter.liveTorLaunchState.value != TorStarter.TOR_LAUNCH_SUCCESS) {
            TorStarter.liveTorLaunchState.postValue(TorStarter.TOR_LAUNCH_FAILED)
            connectionError
        } else {
            try {
                mHttpClient = newHttpClient
                val onionProxyManager = starter.tor
                val port = onionProxyManager!!.iPv4LocalHostSocksPort
                val socksaddr = InetSocketAddress("127.0.0.1", port)
                mContext = HttpClientContext.create()
                mContext.setAttribute("socks.address", socksaddr)
            } catch (e: IOException) {
                e.printStackTrace()
                if (e.message != null && e.message == MyWebViewClient.TOR_NOT_RUNNING_ERROR) {
                    connectionError
                }
            } catch (e: RuntimeException) {
                e.printStackTrace()
                if (e.message != null && e.message == MyWebViewClient.TOR_NOT_RUNNING_ERROR) {
                    connectionError
                }
            }
        }
    }
}