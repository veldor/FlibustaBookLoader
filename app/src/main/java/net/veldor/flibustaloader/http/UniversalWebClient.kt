package net.veldor.flibustaloader.http

import android.net.Uri
import android.os.Build
import android.util.Log
import cz.msebera.android.httpclient.HttpResponse
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper
import java.io.BufferedReader
import java.io.InputStream

class UniversalWebClient {
    fun rawRequest(request: String): WebResponse {
        Log.d("surprise", "UniversalWebClient.kt 14 rawRequest base url is ${URLHelper.getBaseUrl()}")
        return rawRequest(URLHelper.getBaseUrl(), request)
    }

    fun rawRequest(mirror: String, request: String): WebResponse {
        Log.d("surprise", "UniversalWebClient.kt 18 rawRequest mirror is $mirror")
        try {
            val requestString = mirror + request
            Log.d("surprise", "make universal request: $requestString")
            if (PreferencesHandler.instance.isExternalVpn) {
                val response = ExternalVpnVewClient.rawRequest(requestString)
                return if (response != null) {
                    val headers = response.headerFields
                    val resultHeaders = HashMap<String, String>()
                    headers.forEach {
                        if(it.key != null){
                            val value = if (it.value.isNotEmpty()) {
                                it.value[0]
                            } else {
                                ""
                            }
                            resultHeaders[it.key] = value
                        }
                    }
                    WebResponse(
                        response.responseCode,
                        response.inputStream,
                        response.contentType,
                        resultHeaders,
                        response.contentLength
                    )
                } else {
                    WebResponse(999, null, null, null)
                }
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                val response = TorWebClient().rawRequest(requestString)
                return if (response != null) {
                    val resultHeaders = HashMap<String, String>()
                    response.allHeaders.forEach {
                        resultHeaders[it.name] = it.value
                    }
                    WebResponse(
                        response.statusLine.statusCode,
                        response.entity.content,
                        response.entity.contentType.value,
                        resultHeaders,
                        response.entity.contentLength.toInt()
                    )
                } else {
                    WebResponse(999, null, null, null)
                }
            }
            val response = NewTorClient.rawRequest(requestString)
            return if (response != null) {
                Log.d("surprise", "UniversalWebClient.kt 61 rawRequest $response.")
                val headers = response.headerFields
                val resultHeaders = HashMap<String, String>()
                headers.forEach {
                    if (it.key != null) {
                        val value = if (it.value.isNotEmpty()) {
                            it.value[0]
                        } else {
                            ""
                        }
                        resultHeaders[it.key] = value
                    }
                }
                WebResponse(
                    response.responseCode,
                    response.inputStream,
                    response.contentType,
                    resultHeaders,
                    response.contentLength
                )
            } else {
                WebResponse(999, null, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            App.instance.torException = e
            return WebResponse(999, null, null, null)
        }
    }

    fun picRequest(request: String): WebResponse {
        val requestString = PreferencesHandler.instance.picMirror + request
        try {
            Log.d("surprise", "make universal request: $requestString")
            if (PreferencesHandler.instance.isExternalVpn) {
                val response = ExternalVpnVewClient.rawRequest(requestString)
                return if (response != null) {
                    val headers = response.headerFields
                    val resultHeaders = HashMap<String, String>()
                    headers.forEach {
                        if(it.key != null){
                            val value = if (it.value.isNotEmpty()) {
                                it.value[0]
                            } else {
                                ""
                            }
                            resultHeaders[it.key] = value
                        }
                    }
                    WebResponse(
                        response.responseCode,
                        response.inputStream,
                        response.contentType,
                        resultHeaders,
                        response.contentLength
                    )
                } else {
                    WebResponse(999, null, null, null)
                }
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                val response = TorWebClient().rawRequest(requestString)
                return if (response != null) {
                    val resultHeaders = HashMap<String, String>()
                    response.allHeaders.forEach {
                        resultHeaders[it.name] = it.value
                    }
                    WebResponse(
                        response.statusLine.statusCode,
                        response.entity.content,
                        response.entity.contentType.value,
                        resultHeaders,
                        response.entity.contentLength.toInt()
                    )
                } else {
                    WebResponse(999, null, null, null)
                }
            }
            val response = NewTorClient.rawRequest(requestString)
            return if (response != null) {
                val headers = response.headerFields
                val resultHeaders = HashMap<String, String>()
                headers.forEach {
                    if (it.key != null) {
                        val value = if (it.value.isNotEmpty()) {
                            it.value[0]
                        } else {
                            ""
                        }
                        resultHeaders[it.key] = value
                    }
                }
                WebResponse(
                    response.responseCode,
                    response.inputStream,
                    response.contentType,
                    resultHeaders,
                    response.contentLength
                )
            } else {
                WebResponse(999, null, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return WebResponse(999, null, null, null)
        }
    }

    fun responseToString(response: HttpResponse?): String? {
        if (response == null) return null
        if (response.statusLine.statusCode > 399) return null
        val reader = BufferedReader(response.entity.content.reader())
        reader.use { read ->
            return read.readText()
        }
    }

    fun loginRequest(login: String, password: String): Boolean {
        if (PreferencesHandler.instance.isExternalVpn) {
            return ExternalVpnVewClient.loginRequest(
                URLHelper.getBaseUrl() + "/node?destination=node",
                login,
                password
            )
        }
        val request = Uri.parse("/node?destination=node")
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            return TorWebClient().login(request, login, password)
        }
        return NewTorClient.login("/node?destination=node", login, password)
    }

    fun responseToString(response: InputStream?): String? {
        if (response == null) {
            return null
        }
        val reader = BufferedReader(response.reader())
        reader.use { read ->
            return read.readText()
        }
    }
}