package net.veldor.flibustaloader.http

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.ResponseHandler
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.client.protocol.HttpClientContext
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient
import cz.msebera.android.httpclient.impl.client.HttpClients
import cz.msebera.android.httpclient.util.EntityUtils
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.ecxeptions.BookNotFoundException
import net.veldor.flibustaloader.utils.FilesHandler.getBaseDownloadFile
import net.veldor.flibustaloader.utils.FilesHandler.getCompatDownloadFile
import net.veldor.flibustaloader.utils.FilesHandler.getDownloadFile
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper
import java.io.IOException
import java.net.InetSocketAddress

object ExternalVpnVewClient {
    fun request(text: String): String? {
        Log.d("surprise", "ExternalVpnVewClient request: search $text")
        var httpclient: CloseableHttpClient? = null
        try {
            val port = 9050
            val socksaddr = InetSocketAddress("127.0.0.1", port)
            val context = HttpClientContext.create()
            context.setAttribute("socks.address", socksaddr)
            httpclient = HttpClients.createDefault()
            val httpget = HttpGet(text)
            // кастомный обработчик ответов
            val responseHandler = ResponseHandler<String?> { response: HttpResponse ->
                val status = response.statusLine.statusCode
                Log.d("surprise", "TestHttpRequestWorker handleResponse status is $status")
                val entity = response.entity
                if (entity != null) {
                    Log.d("surprise", "ExternalVpnVewClient handleResponse: have answer")
                    try {
                        Log.d("surprise", "ExternalVpnVewClient handleResponse: returning answer")
                        return@ResponseHandler EntityUtils.toString(entity)
                    } catch (e: IOException) {
                        Log.d(
                            "surprise",
                            "ExternalVpnVewClient handleResponse: can't connect " + e.message
                        )
                    }
                }
                Log.d("surprise", "ExternalVpnVewClient handleResponse: can't have answer")
                null
            }
            // выполню запрос
            return httpclient.execute(httpget, responseHandler, context)
        } catch (e: IOException) {
            Log.d("surprise", "TestHttpRequestWorker doWork have error in request: " + e.message)
        } finally {
            try {
                // по-любому закрою клиент
                httpclient?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun loadImage(s: String): Bitmap? {
        Log.d("surprise", "ExternalVpnVewClient raw request: $s")
        var httpclient: CloseableHttpClient? = null
        try {
            httpclient = HttpClients.createSystem()
            val httpget = HttpGet(s)
            // выполню запрос
            val response = httpclient.execute(httpget)
            return BitmapFactory.decodeStream(response.entity.content)
        } catch (e: IOException) {
            Log.d("surprise", "TestHttpRequestWorker doWork have error in request: " + e.message)
        } finally {
            try {
                // по-любому закрою клиент
                httpclient?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    @Throws(BookNotFoundException::class)
    fun downloadBook(book: BooksDownloadSchedule) {
        Log.d(
            "surprise",
            "ExternalVpnVewClient downloadBook 112: request " + URLHelper.getBaseUrl() + book.link
        )
        var response = rawRequest(URLHelper.getBaseUrl() + book.link)
        if (response == null || response.statusLine.statusCode != 200 || response.entity.contentLength < 1) {
            response = rawRequest(URLHelper.getBaseUrl() + book.link)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val newFile = getDownloadFile(book, response!!)
                // запрошу данные
                Log.d(
                    "surprise",
                    "TorWebClient downloadBook: request " + book.link + " of book " + book.name
                )
                GlobalWebClient.handleBookLoadRequest(response, newFile)
                if (newFile.isFile && newFile.length() > 0) {
                    book.loaded = true
                }
            } catch (e: Exception) {
                try {
                    val file = getCompatDownloadFile(book)
                    GlobalWebClient.handleBookLoadRequest(response, file)
                    if (file.isFile && file.length() > 0) {
                        book.loaded = true
                    }
                } catch (e1: Exception) {
                    // скачаю файл просто в папку загрузок
                    val file = getBaseDownloadFile(book)
                    GlobalWebClient.handleBookLoadRequest(response, file)
                }
            }
        } else {
            val file = getCompatDownloadFile(book)
            GlobalWebClient.handleBookLoadRequest(response, file)
            if (file.isFile && file.length() > 0) {
                book.loaded = true
            }
        }
    }

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
            GlobalWebClient.mConnectionState.postValue(GlobalWebClient.DISCONNECTED)
        }
        return null
    }
}