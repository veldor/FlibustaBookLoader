package net.veldor.flibustaloader.http

import net.veldor.flibustaloader.utils.URLHelper.getBaseOPDSUrl
import net.veldor.flibustaloader.utils.FilesHandler.getDownloadFile
import net.veldor.flibustaloader.utils.FilesHandler.getCompatDownloadFile
import net.veldor.flibustaloader.utils.FilesHandler.getBaseDownloadFile
import net.veldor.flibustaloader.utils.URLHelper.getFlibustaIsUrl
import net.veldor.flibustaloader.App
import cz.msebera.android.httpclient.client.protocol.HttpClientContext
import kotlin.Throws
import net.veldor.flibustaloader.ecxeptions.ConnectionLostException
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory
import cz.msebera.android.httpclient.config.RegistryBuilder
import net.veldor.flibustaloader.MyConnectionSocketFactory
import net.veldor.flibustaloader.MySSLConnectionSocketFactory
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager
import net.veldor.flibustaloader.MyWebViewClient.FakeDnsResolver
import cz.msebera.android.httpclient.impl.client.HttpClients
import net.veldor.flibustaloader.ecxeptions.BookNotFoundException
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import android.os.Build
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity
import net.veldor.flibustaloader.R
import cz.msebera.android.httpclient.client.methods.HttpPost
import android.widget.Toast
import cz.msebera.android.httpclient.message.BasicNameValuePair
import net.veldor.flibustaloader.MyWebViewClient
import android.net.Uri
import cz.msebera.android.httpclient.*
import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.ssl.SSLContexts
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.io.*
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.net.InetSocketAddress
import java.util.ArrayList

class TorWebClient {
    private var mHttpClient: HttpClient? = null
    private lateinit var mContext: HttpClientContext

    @get:Throws(ConnectionLostException::class)
    private val connectionError: Unit
        get() {
            GlobalWebClient.mConnectionState.postValue(GlobalWebClient.DISCONNECTED)
            throw ConnectionLostException()
        }

    fun request(incomingText: String): String? {
        var text = incomingText
        if (App.instance.useMirror) {
            // TODO заменить зеркало
            text = text.replace("http://flibustahezeous3.onion", "https://flibusta.appspot.com")
        }
        try {
            val httpGet = HttpGet(text)
            httpGet.setHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36"
            )
            httpGet.setHeader("X-Compress", "null")
            val httpResponse = mHttpClient!!.execute(httpGet, mContext)
            App.instance.mLoadAllStatus.postValue("Данные получены")
            val `is`: InputStream = httpResponse.entity.content
            return inputStreamToString(`is`)
        } catch (e: IOException) {
            App.instance.mLoadAllStatus.postValue("Ошибка загрузки страницы")
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
            val httpResponse = mHttpClient!!.execute(httpGet, mContext)
            App.instance.mLoadAllStatus.postValue("Данные получены")
            val `is`: InputStream = httpResponse.entity.content
            return inputStreamToString(`is`)
        } catch (e: IOException) {
            App.instance.mLoadAllStatus.postValue("Ошибка загрузки страницы")
            //broadcastTorError(e);
            e.printStackTrace()
        }
        return null
    }

    @Throws(IOException::class)
    private fun simpleGetRequest(incomingUrl: String): HttpResponse {
        var url = incomingUrl
        if (App.instance.useMirror) {
            // TODO заменить зеркало
            url = url.replace("http://flibustahezeous3.onion", "https://flibusta.appspot.com")
        }
        val httpGet = HttpGet(url)
        httpGet.setHeader(
            "Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
        )
        httpGet.setHeader(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36"
        )
        httpGet.setHeader("X-Compress", "null")
        return mHttpClient!!.execute(httpGet, mContext)
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

    @Throws(BookNotFoundException::class)
    fun downloadBook(book: BooksDownloadSchedule) {
        try {
            var response = simpleGetRequest(getBaseOPDSUrl() + book.link)
            // проверю, что запрос выполнен и файл не пуст. Если это не так- попорбую загрузить книгу с основного домена
            if (response.statusLine.statusCode == 200 && response.entity.contentLength < 1) {
                var result = false
                // тут может быть загрузка книги без указания длины контента, попробую загрузить
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        val newFile = getDownloadFile(book, response)
                        if (newFile != null) {
                            result = GlobalWebClient.handleBookLoadRequestNoContentLength(
                                response,
                                newFile
                            )
                            if (newFile.isFile && newFile.length() > 0) {
                                if (book.format.isEmpty()) {
                                    val receivedContentType = response.getLastHeader("Content-Type")
                                    book.format = receivedContentType.value
                                }
                                book.loaded = true
                            }
                        }
                    } catch (e: Exception) {
                        try {
                            val file = getCompatDownloadFile(book)
                            result =
                                GlobalWebClient.handleBookLoadRequestNoContentLength(response, file)
                            if (file.isFile && file.length() > 0) {
                                book.loaded = true
                            }
                        } catch (e1: Exception) {
                            // скачаю файл просто в папку загрузок
                            val file = getBaseDownloadFile(book)
                            result =
                                GlobalWebClient.handleBookLoadRequestNoContentLength(response, file)
                            if (file.isFile && file.length() > 0) {
                                if (book.format.isEmpty()) {
                                    val receivedContentType = response.getLastHeader("Content-Type")
                                    book.format = receivedContentType.value
                                }
                                book.loaded = true
                            }
                        }
                    }
                } else {
                    val file = getCompatDownloadFile(book)
                    result = GlobalWebClient.handleBookLoadRequestNoContentLength(response, file)
                    if (file.isFile && file.length() > 0) {
                        if (book.format.isEmpty()) {
                            val receivedContentType = response.getLastHeader("Content-Type")
                            book.format = receivedContentType.value
                        }
                        book.loaded = true
                    }
                }
                if (result) {
                    return
                }
            }
            if (response.statusLine.statusCode != 200 || response.entity.contentLength < 1) {
                // попробую загрузку с резервного адреса
                response = simpleGetRequest(getFlibustaIsUrl() + book.link)
                if (response.statusLine.statusCode == 200 && response.entity.contentLength < 1) {
                    var result = false
                    // тут может быть загрузка книги без указания длины контента, попробую загрузить
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        try {
                            val newFile = getDownloadFile(book, response)
                            if (newFile != null) {
                                result = GlobalWebClient.handleBookLoadRequestNoContentLength(
                                    response,
                                    newFile
                                )
                                if (newFile.isFile && newFile.length() > 0) {
                                    if (book.format.isEmpty()) {
                                        val receivedContentType =
                                            response.getLastHeader("Content-Type")
                                        book.format = receivedContentType.value
                                    }
                                    book.loaded = true
                                }
                            }
                        } catch (e: Exception) {
                            try {
                                val file = getCompatDownloadFile(book)
                                result = GlobalWebClient.handleBookLoadRequestNoContentLength(
                                    response,
                                    file
                                )
                                if (file.isFile && file.length() > 0) {
                                    if (book.format.isEmpty()) {
                                        val receivedContentType =
                                            response.getLastHeader("Content-Type")
                                        book.format = receivedContentType.value
                                    }
                                    book.loaded = true
                                }
                            } catch (e1: Exception) {
                                // скачаю файл просто в папку загрузок
                                val file = getBaseDownloadFile(book)
                                result = GlobalWebClient.handleBookLoadRequestNoContentLength(
                                    response,
                                    file
                                )
                                if (file.isFile && file.length() > 0) {
                                    if (book.format.isEmpty()) {
                                        val receivedContentType =
                                            response.getLastHeader("Content-Type")
                                        book.format = receivedContentType.value
                                    }
                                    book.loaded = true
                                }
                            }
                        }
                    } else {
                        val file = getCompatDownloadFile(book)
                        result =
                            GlobalWebClient.handleBookLoadRequestNoContentLength(response, file)
                        if (file.isFile && file.length() > 0) {
                            if (book.format.isEmpty()) {
                                val receivedContentType = response.getLastHeader("Content-Type")
                                book.format = receivedContentType.value
                            }
                            book.loaded = true
                        }
                    }
                    if (result) {
                        return
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val newFile = getDownloadFile(book, response)
                    if (newFile != null) {
                        GlobalWebClient.handleBookLoadRequest(response, newFile)
                        if (newFile.isFile && newFile.length() > 0) {
                            if (book.format.isEmpty()) {
                                val receivedContentType = response.getLastHeader("Content-Type")
                                book.format = receivedContentType.value
                            }
                            book.loaded = true
                        }
                    }
                } catch (e: Exception) {
                    try {
                        val file = getCompatDownloadFile(book)
                        GlobalWebClient.handleBookLoadRequest(response, file)
                        if (file.isFile && file.length() > 0) {
                            if (book.format.isEmpty()) {
                                val receivedContentType = response.getLastHeader("Content-Type")
                                book.format = receivedContentType.value
                            }
                            book.loaded = true
                        }
                    } catch (e1: Exception) {
                        // скачаю файл просто в папку загрузок
                        val file = getBaseDownloadFile(book)
                        GlobalWebClient.handleBookLoadRequest(response, file)
                        if (file.isFile && file.length() > 0) {
                            if (book.format.isEmpty()) {
                                val receivedContentType = response.getLastHeader("Content-Type")
                                book.format = receivedContentType.value
                            }
                            book.loaded = true
                        }
                    }
                }
            } else {
                val file = getCompatDownloadFile(book)
                GlobalWebClient.handleBookLoadRequest(response, file)
                if (file.isFile && file.length() > 0) {
                    if (book.format.isEmpty()) {
                        val receivedContentType = response.getLastHeader("Content-Type")
                        book.format = receivedContentType.value
                    }
                    book.loaded = true
                }
            }
        } catch (e: NoHttpResponseException) {
            // книга недоступна для скачивания
            throw BookNotFoundException()
        } catch (e: IOException) {
            e.printStackTrace()
            //throw new TorNotLoadedException();
        }
    }

    @Throws(Exception::class)
    fun login(uri: Uri, login: String, password: String): Boolean {
        val response: HttpResponse?
        val params: UrlEncodedFormEntity? = get2post(uri, login, password)
        try {
            response =
                executeRequest("http://flibustahezeous3.onion/node?destination=node", null, params)
            App.instance.requestStatus.postValue(
                App.instance.getString(R.string.response_received_message)
            )
            if (response != null) {
                response.statusLine.statusCode
                // получен ответ, попробую извлечь куку
                val cookies = response.getHeaders("set-cookie")
                if (cookies.size > 1) {
                    val cookieValue = StringBuilder()
                    for (c in cookies) {
                        val value = c.value
                        /*if(value.startsWith("PERSISTENT_LOGIN")){
                            cookieValue.append(value.substring(0, value.indexOf(";")));
                        }
                        else */if (value.startsWith("SESS")) {
                            cookieValue.append(value.substring(0, value.indexOf(";")))
                        }
                    }
                    PreferencesHandler.instance.authCookie = cookieValue.toString()
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
                request.setHeader(
                    HttpHeaders.ACCEPT,
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
                )
                request.setHeader(HttpHeaders.ACCEPT_ENCODING, "ggzip, deflate")
                request.setHeader(
                    HttpHeaders.ACCEPT_LANGUAGE,
                    "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7"
                )
                request.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                request.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
                request.setHeader("DNT", "1")
                request.setHeader(HttpHeaders.HOST, "flibustahezeous3.onion")
                request.setHeader("Origin", "http://flibustahezeous3.onion")
                request.setHeader(HttpHeaders.PRAGMA, "no-cache")
                request.setHeader("Proxy-Connection", "keep-ali e")
                request.setHeader("Upgrade-Insecure-Requests", "1")
                request.setHeader(
                    HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.116 Safari/537.36"
                )
                return httpClient.execute(request, clientContext)
            }
        } catch (e: RuntimeException) {
            Toast.makeText(App.instance, "Error request", Toast.LENGTH_LONG).show()
        }
        return null
    }

    companion object {
        const val ERROR_DETAILS = "error details"
        private fun get2post(url: Uri, login: String, password: String): UrlEncodedFormEntity? {
            val params = url.queryParameterNames
            if (params.isEmpty()) {
                return null
            }
            val paramsArray: MutableList<NameValuePair> = ArrayList()
            paramsArray.add(BasicNameValuePair("openid_identifier", null))
            paramsArray.add(BasicNameValuePair("name", login))
            paramsArray.add(BasicNameValuePair("pass", password))
            paramsArray.add(BasicNameValuePair("persistent_login", "1"))
            paramsArray.add(BasicNameValuePair("op", "Вход в систему"))
            paramsArray.add(
                BasicNameValuePair(
                    "form_build_id",
                    "form-sIt20MHWRjpMKIvxdtHOGqLAa4D2GiBnFIXke7LXv7Y"
                )
            )
            paramsArray.add(BasicNameValuePair("form_id", "user_login_block"))
            paramsArray.add(
                BasicNameValuePair(
                    "return_to",
                    "http://flibustahezeous3.onion/openid/authenticate?destination=node"
                )
            )
            try {
                return UrlEncodedFormEntity(paramsArray, "utf8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            return null
        }
    }

    init {
        while (App.instance.torInitInProgress) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        App.instance.torInitInProgress = true
        // попробую стартовать TOR
        val starter = TorStarter()
        App.sTorStartTry = 0
        while (App.sTorStartTry < 4) {
            // есть три попытки, если все три неудачны- верну ошибку
            if (starter.startTor()) {
                GlobalWebClient.mConnectionState.postValue(GlobalWebClient.CONNECTED)
                App.sTorStartTry = 0
                break
            } else {
                App.sTorStartTry++
            }
        }
        App.instance.torInitInProgress = false
        // если счётчик больше 3- не удалось запустить TOR, вызову исключение
        if (App.sTorStartTry > 3) {
            connectionError
        }
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