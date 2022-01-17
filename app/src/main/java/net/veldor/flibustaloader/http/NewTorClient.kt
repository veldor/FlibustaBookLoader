package net.veldor.flibustaloader.http

import android.net.Uri
import android.util.Log
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity
import cz.msebera.android.httpclient.message.BasicNameValuePair
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper
import java.io.*
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

const val REQUEST_METHOD_GET = "GET"
const val REQUEST_METHOD_POST = "POST"
const val READ_TIMEOUT_SEC = 15
const val CONNECT_TIMEOUT_SEC = 50
const val USER_AGENT_PROPERTY = "User-Agent"
const val TOR_BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0"

object NewTorClient {
    @JvmStatic
    fun rawRequest(url: String): HttpURLConnection? {
        val tor = App.instance.mLoadedTor.value!!
        val port = tor.iPv4LocalHostSocksPort
        val proxy = Proxy(
            Proxy.Type.SOCKS,
            InetSocketAddress(
                "127.0.0.1",
                port
            )
        )
        val host = URL(url)
        val connection = host.openConnection(proxy) as HttpURLConnection
        val authCookie = PreferencesHandler.instance.authCookie
        if (authCookie != null) {
            connection.setRequestProperty("Cookie", authCookie)
        }
        connection.apply {
            requestMethod = REQUEST_METHOD_GET
            connectTimeout = CONNECT_TIMEOUT_SEC * 1000
            readTimeout = READ_TIMEOUT_SEC * 1000
            setRequestProperty(USER_AGENT_PROPERTY, TOR_BROWSER_USER_AGENT)
            connect()
        }
        val code = connection.responseCode
        if (code < 400) {
            // success connection, return input stream
            return connection
        }
        return null
    }

    fun login(request: String, login: String, password: String): Boolean {
        // request main page
        val mainPage = UniversalWebClient().rawRequest("")
        if (mainPage.statusCode < 400) {
            val text = UniversalWebClient().responseToString(mainPage.inputStream)
            // get form id
            val startIndex = text!!.indexOf("form_build_id") + 19
            val formId = text.subSequence(startIndex, startIndex + 48)
            val tor = App.instance.mLoadedTor.value!!
            val port = tor.iPv4LocalHostSocksPort
            val proxy = Proxy(
                Proxy.Type.SOCKS,
                InetSocketAddress(
                    "127.0.0.1",
                    port
                )
            )
            val host =
                URL(URLHelper.getBaseUrl() + "/node?destination=node")
            Log.d("surprise", "NewTorClient.kt 74 login request to $host")
            val connection = host.openConnection(proxy) as HttpURLConnection
            val params =
                "openid_identifier=&name=$login&pass=$password&persistent_login=1&op=%D0%92%D1%85%D0%BE%D0%B4+%D0%B2+%D1%81%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D1%83&form_build_id=$formId&form_id=user_login_block&openid.return_to=http%3A%2F%2Fflibusta.is%2Fopenid%2Fauthenticate%3Fdestination%3Dnode"
            connection.doOutput = true
            connection.doInput = true
            connection.useCaches = false
            connection.instanceFollowRedirects = false
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_SEC * 1000
            connection.readTimeout = READ_TIMEOUT_SEC * 1000
            connection.setRequestProperty(USER_AGENT_PROPERTY, TOR_BROWSER_USER_AGENT)
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Content-Length", params.length.toString())
            val os: OutputStream = connection.outputStream
            val osw = OutputStreamWriter(os, "UTF-8")
            osw.write(params)
            osw.flush()
            osw.close()
            os.close()
            connection.connect()
            val code = connection.responseCode
            Log.d("surprise", "NewTorClient.kt 88 login $code")
            if (code < 400) {
                var headers = connection.headerFields
                var cookieHeader = headers.get("Set-Cookie")
                if (!cookieHeader.isNullOrEmpty()) {
                    cookieHeader.forEach {
                        val cookieValue = StringBuilder()
                        if (it.startsWith("SESS")) {
                            cookieValue.append(it.substring(0, it.indexOf(";")))
                            Log.d("surprise", "loginRequest: session cookie is $cookieValue")
                            PreferencesHandler.instance.authCookie = cookieValue.toString()
                            Log.d("surprise", "login: success login")
                            App.instance.requestStatus.postValue(
                                App.instance.getString(R.string.success_login_message)
                            )
                            return true
                        }
                    }
                }
                // success connection, return input stream
                return false
            }
        } else {
            Log.d("surprise", "NewTorClient.kt 98 login ${mainPage.statusCode}")
        }
        return false
    }

}