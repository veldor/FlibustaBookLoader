package net.veldor.flibustaloader

import net.veldor.flibustaloader.http.ExternalVpnVewClient.rawRequest
import net.veldor.flibustaloader.utils.MimeTypes.isBookFormat
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager
import net.veldor.flibustaloader.database.AppDatabase
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.http.GlobalWebClient
import androidx.documentfile.provider.DocumentFile
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory
import kotlin.Throws
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceResponse
import androidx.annotation.RequiresApi
import android.os.Build
import android.webkit.WebResourceRequest
import net.veldor.flibustaloader.http.TorStarter
import cz.msebera.android.httpclient.config.RegistryBuilder
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager
import cz.msebera.android.httpclient.impl.client.HttpClients
import android.graphics.Bitmap
import cz.msebera.android.httpclient.conn.DnsResolver
import cz.msebera.android.httpclient.client.protocol.HttpClientContext
import cz.msebera.android.httpclient.client.methods.HttpGet
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.ui.BaseActivity
import android.content.Intent
import android.util.Log
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.ssl.SSLContexts
import net.veldor.flibustaloader.http.JavaConnectionSocketFactory
import net.veldor.flibustaloader.http.JavaSslConnectionSocketFactory
import net.veldor.flibustaloader.receivers.BookLoadedReceiver
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets

class MyWebViewClient internal constructor() : WebViewClient() {
    private var onionProxyManager: AndroidOnionProxyManager?
    private var mViewMode = 0
    private var mNightMode = false
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse {
        return handleRequest(view, url)!!
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val requestString = request.url.toString()
        return handleRequest(view, requestString)
    }//            throw new TorNotLoadedException();

    // сделаю по новому- уведомлю, что не удалось установить соединение
// есть три попытки, если все три неудачны- верну ошибку
    // если счётчик больше 3- не удалось запустить TOR, вызову исключение
    // попробую стартовать TOR
    private val newHttpClient: HttpClient?
        get() {
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
//            throw new TorNotLoadedException();
                // сделаю по новому- уведомлю, что не удалось установить соединение
                GlobalWebClient.mConnectionState.postValue(GlobalWebClient.DISCONNECTED)
                return null
            }
            val reg = RegistryBuilder.create<ConnectionSocketFactory>()
                .register("http", MyConnectionSocketFactory())
                .register("https", MySSLConnectionSocketFactory(SSLContexts.createSystemDefault()))
                .build()
            val cm = PoolingHttpClientConnectionManager(reg, FakeDnsResolver())
            return HttpClients.custom()
                .setConnectionManager(cm)
                .build()
        }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
        super.onPageStarted(view, url, favicon)
    }

    private fun injectMyJs(originalJs: String?): String? {
        var output = originalJs
        try {
            if (mViewMode == App.VIEW_MODE_FAT || mViewMode == App.VIEW_MODE_LIGHT) {
                val context: App = App.instance
                /*inputStream = context.getAssets().open(JQUERY);
                output += inputStreamToString(inputStream);*/
                val inputStream = context.assets.open(
                    MY_JS
                )
                output += inputStreamToString(inputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return output
    }

    private fun injectMyCss(originalCss: String?): String? {
        // старые версии Android не понимают переменные цветов и новые объявления JS, подключусь в режиме совместимости
        val context: App = App.instance
        var inputStream: InputStream
        var output = originalCss
        try {
            if (mViewMode > 1) {
                inputStream = when (mViewMode) {
                    App.VIEW_MODE_FAT, App.VIEW_MODE_FAST_FAT -> context.assets.open(
                        MY_COMPAT_FAT_CSS_STYLE
                    )
                    App.VIEW_MODE_LIGHT -> context.assets.open(MY_COMPAT_CSS_STYLE)
                    else -> context.assets.open(MY_COMPAT_CSS_STYLE)
                }
                output += inputStreamToString(inputStream)
            }
            if (mNightMode) {
                inputStream = context.assets.open(MY_CSS_NIGHT_STYLE)
                output += inputStreamToString(inputStream)
            }
            return output
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    class FakeDnsResolver : DnsResolver {
        @Throws(UnknownHostException::class)
        override fun resolve(host: String): Array<InetAddress> {
            return arrayOf(InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)))
        }
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

    private fun handleRequest(view: WebView, incomingUrl: String): WebResourceResponse? {
        var url = incomingUrl
        if (App.instance.useMirror) {
            url = url.replace("http://flibustahezeous3.onion", "https://flibusta.appspot.com")
        }
        return try {
            mViewMode = PreferencesHandler.instance.viewMode
            mNightMode = PreferencesHandler.instance.nightMode
            // обрубаю загрузку картинок в упрощённом виде
            if (mViewMode > 1) {
                val extensionArr = url.split("\\.").toTypedArray()
                if (extensionArr.isNotEmpty()) {
                    val extension = extensionArr[extensionArr.size - 1]
                    if (extension == JPG_TYPE || extension == JPEG_TYPE || extension == PNG_TYPE || extension == GIF_TYPE) {
                        return super.shouldInterceptRequest(view, url)
                    }
                }
            }
            val httpResponse: HttpResponse?
            if (PreferencesHandler.instance.isExternalVpn) {
                httpResponse = rawRequest(url)
            } else {
                val httpClient = newHttpClient ?: return connectionError
                // если вернулся null- значит, не удалось получить клиент, скажу об ошибке соединения
                if (onionProxyManager == null) {
                    onionProxyManager = App.instance.mLoadedTor.value
                }
                if (onionProxyManager == null) {
                    return null
                }
                val port = onionProxyManager!!.iPv4LocalHostSocksPort
                val socksaddr = InetSocketAddress("127.0.0.1", port)
                val context = HttpClientContext.create()
                context.setAttribute("socks.address", socksaddr)
                val httpGet = HttpGet(url)
                httpGet.setHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36"
                )
                httpGet.setHeader("X-Compress", "null")
                val authCookie = PreferencesHandler.instance.authCookie
                if (authCookie != null) {
                    httpGet.setHeader("Cookie", authCookie)
                }
                httpResponse = try {
                    httpClient.execute(httpGet, context)
                } catch (e: Exception) {
                    return connectionError
                }
            }
            if (httpResponse == null) {
                return connectionError
            }
            var input = httpResponse.entity.content
            var encoding = ENCODING_UTF_8
            var mime = httpResponse.entity.contentType.value

            if (mime == "application/octet-stream") {
                // придётся ориентироваться по имени файла и определять, что это книга
                // костыль, конечно, но что делать

                // покажу хедеры
                val headers = httpResponse.allHeaders
                for (h in headers) {
                    if (h.name == "Content-Disposition") {
                        // похоже на книгу
                        // Тут пока что грязный хак, скажу, что это epub
                        mime = "application/epub"
                    }
                }
            }
            // Если формат книжный, загружу книгу
            if (isBookFormat(mime)) {
//                Intent startLoadingIntent = new Intent(BOOK_LOAD_ACTION);
//                startLoadingIntent.putExtra(BOOK_LOAD_EVENT, START_BOOK_LOADING);
//                App.getInstance().sendBroadcast(startLoadingIntent);
                // пока что- сэмулирую загрузку по типу OPDS
                val newBook = BooksDownloadSchedule()
                // покажу хедеры
                val headers = httpResponse.allHeaders
                for (h in headers) {
                    if (h.value.startsWith("attachment; filename=\"")) {
                        newBook.name = h.value.substring(22)
                        Log.d(
                            "surprise",
                            "MyWebViewClient handleRequest 276: name is " + newBook.name
                        )
                        break
                    } else if (h.value.startsWith("attachment;")) {
                        newBook.name = h.value.substring(21)
                        Log.d(
                            "surprise",
                            "MyWebViewClient handleRequest 276: name is " + newBook.name
                        )
                        break
                    }
                }
                // создам файл
                val database: AppDatabase = App.instance.mDatabase
                newBook.link = url.substring(url.indexOf("/b"))
                val dao = database.booksDownloadScheduleDao()
                dao.insert(newBook)
                BaseActivity.sLiveDownloadScheduleCount.postValue(true)
                if (PreferencesHandler.instance.isDownloadAutostart) {
                    App.instance.initializeDownload()
                }
                val message = "<H1 style='text-align:center;'>Книга добавлена в очередь загрузок</H1><H2 style='text-align:center;'>Возвращаюсь на предыдущую страницу</H2><script>setTimeout(function(){history.back()}, 1000)</script>"
                var inputStream: ByteArrayInputStream? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    inputStream = ByteArrayInputStream(message.toByteArray(StandardCharsets.UTF_8))
                } else {
                    try {
                        inputStream =
                            ByteArrayInputStream(message.toByteArray(charset(ENCODING_UTF_8)))
                    } catch (ex: UnsupportedEncodingException) {
                        ex.printStackTrace()
                    }
                }
                //                Intent finishLoadingIntent = new Intent(BOOK_LOAD_ACTION);
//                finishLoadingIntent.putExtra(BOOK_LOAD_EVENT, FINISH_BOOK_LOADING);
//                App.getInstance().sendBroadcast(finishLoadingIntent);
                return WebResourceResponse("text/html", ENCODING_UTF_8, inputStream)
            }

            // если загружена страница- добавлю её как последнюю загруженную
            if (mime.startsWith(HTML_TYPE)) {
                if (!url.startsWith(AJAX_REQUEST)) {
                    PreferencesHandler.instance.lastLoadedUrl = url
                    // попробую найти внутри ссылки на книги
                    // скопирую inputStream для разбора ссылок
                    val baos = ByteArrayOutputStream()
                    // Fake code simulating the copy
                    // You can generally do better with nio if you need...
                    // And please, unlike me, do something about the Exceptions :D
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (input.read(buffer).also { len = it } > -1) {
                        baos.write(buffer, 0, len)
                    }
                    baos.flush()
                    // Open new InputStreams using the recorded bytes
                    // Can be repeated as many times as you wish
                    val my: InputStream = ByteArrayInputStream(baos.toByteArray())
                    input = ByteArrayInputStream(baos.toByteArray())
                    // запущу рабочего, который обработает текст страницы и найдёт что-то полезное
                    App.instance.handleWebPage(my)
                }
            }
            if (mime == CSS_FORMAT) {
                val `is` = httpResponse.entity.content
                // подключу нужные CSS простым объединением строк
                val origin = inputStreamToString(`is`)
                val injectionText = injectMyCss(origin)
                if (injectionText != null) {
                    val inputStream = ByteArrayInputStream(
                        injectionText.toByteArray(
                            charset(
                                encoding
                            )
                        )
                    )
                    return WebResourceResponse(mime, ENCODING_UTF_8, inputStream)
                }
                if (origin != null) {
                    val inputStream = ByteArrayInputStream(
                        origin.toByteArray(
                            charset(
                                encoding
                            )
                        )
                    )
                    return WebResourceResponse(mime, ENCODING_UTF_8, inputStream)
                }
                return WebResourceResponse(mime, ENCODING_UTF_8, null)
            } else if (mime == JS_FORMAT) {
                val `is` = httpResponse.entity.content
                val origin = inputStreamToString(`is`)
                val injectionText = injectMyJs(origin)
                val inputStream = ByteArrayInputStream(
                    injectionText!!.toByteArray(charset(encoding))
                )
                return WebResourceResponse(mime, ENCODING_UTF_8, inputStream)
            }
            if (mime.contains(";")) {
                var arr = mime.split(";").toTypedArray()
                mime = arr[0]
                arr = arr[1].split("=").toTypedArray()
                encoding = arr[1]
            }
            if (mime == FB2_FORMAT || mime == PDF_FORMAT) {
                val activityContext = view.context
                val header = httpResponse.getFirstHeader(HEADER_CONTENT_DISPOSITION)
                var name = header.value.split(FILENAME_DELIMITER).toTypedArray()[1]
                name = name.replace("\"", "")
                val extensionSource = name.split("\\.").toTypedArray()
                val extension = extensionSource[extensionSource.size - 1]
                val types = url.split("/").toTypedArray()
                var type = types[types.size - 1]
                if (mime == PDF_FORMAT) {
                    type = PDF_TYPE
                }
                if (extension == DJVU_TYPE) {
                    type = DJVU_TYPE
                }
                if (type == FB2_TYPE || type == MOBI_TYPE || type == EPUB_TYPE || type == PDF_TYPE || type == DJVU_TYPE) {
                    try {
                        // начинаю загружать книку, пошлю оповещение о начале загрузки
                        val startLoadingIntent = Intent(BOOK_LOAD_ACTION)
                        startLoadingIntent.putExtra(BOOK_LOAD_EVENT, START_BOOK_LOADING)
                        activityContext.sendBroadcast(startLoadingIntent)
                        // сохраняю книгу в памяти устройства
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val downloadsDir: DocumentFile =
                                PreferencesHandler.instance.downloadDir!!
                            // проверю, не сохдан ли уже файл, если создан- удалю
                            val existentFile = downloadsDir.findFile(name)
                            existentFile?.delete()
                            val newFile = downloadsDir.createFile(mime, name)
                            if (newFile != null) {
                                val `is` = httpResponse.entity.content
                                val out: OutputStream =
                                    App.instance.contentResolver
                                        .openOutputStream(newFile.uri)!!
                                var read: Int
                                val buffer = ByteArray(1024)
                                while (`is`.read(buffer).also { read = it } > 0) {
                                    out.write(buffer, 0, read)
                                }
                                out.close()
                            }
                        } else {
                            val file = PreferencesHandler.instance.compatDownloadDir
                            if (file != null) {
                                val newFile = File(file, name)
                                val status = httpResponse.statusLine.statusCode
                                if (status == 200) {
                                    val entity = httpResponse.entity
                                    if (entity != null) {
                                        val content = entity.content
                                        if (content != null) {
                                            val out: OutputStream = FileOutputStream(newFile)
                                            var read: Int
                                            val buffer = ByteArray(1024)
                                            while (content.read(buffer).also { read = it } > 0) {
                                                out.write(buffer, 0, read)
                                            }
                                            out.close()
                                        }
                                    }
                                }
                            }
                        }
                        // отправлю сообщение о скачанном файле через broadcastReceiver
                        val intent = Intent(activityContext, BookLoadedReceiver::class.java)
                        intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_NAME, name)
                        intent.putExtra(BookLoadedReceiver.EXTRA_BOOK_TYPE, type)
                        activityContext.sendBroadcast(intent)
                        val message =
                            "<H1 style='text-align:center;'>Книга закачана. Возвращаюсь на предыдущую страницу</H1>"
                        val inputStream = ByteArrayInputStream(
                            message.toByteArray(
                                charset(
                                    encoding
                                )
                            )
                        )
                        return WebResourceResponse(mime, ENCODING_UTF_8, inputStream)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        // отправлю оповещение об окончании загрузки страницы
                        val finishLoadingIntent = Intent(BOOK_LOAD_ACTION)
                        finishLoadingIntent.putExtra(BOOK_LOAD_EVENT, FINISH_BOOK_LOADING)
                        activityContext.sendBroadcast(finishLoadingIntent)
                    }
                }
            }
            WebResourceResponse(mime, encoding, input)
        } catch (e: Exception) {
            e.printStackTrace()
            connectionError
        }
    }

    /**
     * Сообщу об ошибке соединения и верну заглушку
     */
    private val connectionError: WebResourceResponse
        get() {
            GlobalWebClient.mConnectionState.postValue(GlobalWebClient.DISCONNECTED)
            val message = "<H1 style='text-align:center;'>Ошибка подключения к сети</H1>"
            var inputStream: ByteArrayInputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                inputStream = ByteArrayInputStream(message.toByteArray(StandardCharsets.UTF_8))
            } else {
                try {
                    inputStream = ByteArrayInputStream(message.toByteArray(charset(ENCODING_UTF_8)))
                } catch (ex: UnsupportedEncodingException) {
                    ex.printStackTrace()
                }
            }
            return WebResourceResponse("text/html", ENCODING_UTF_8, inputStream)
        }

    companion object {
        const val TOR_NOT_RUNNING_ERROR = "Tor is not running!"
        const val BOOK_LOAD_ACTION = "net.veldor.flibustaloader.action.BOOK_LOAD_EVENT"
        const val TOR_CONNECT_ERROR_ACTION = "net.veldor.flibustaloader.action.TOR_CONNECT_ERROR"
        const val START_BOOK_LOADING = 1
        const val FINISH_BOOK_LOADING = 2
        private const val ENCODING_UTF_8 = "UTF-8"
        private const val FB2_FORMAT = "application/zip"
        private const val PDF_FORMAT = "application/pdf"
        private const val CSS_FORMAT = "text/css"
        private const val JS_FORMAT = "application/x-javascript"

        // content types
        private const val FB2_TYPE = "fb2"
        private const val MOBI_TYPE = "mobi"
        private const val EPUB_TYPE = "epub"
        private const val PDF_TYPE = "pdf"
        private const val DJVU_TYPE = "djvu"
        private const val JPG_TYPE = "jpg"
        private const val JPEG_TYPE = "jpeg"
        private const val GIF_TYPE = "gif"
        private const val PNG_TYPE = "png"
        private const val HEADER_CONTENT_DISPOSITION = "Content-Disposition"
        private const val FILENAME_DELIMITER = "filename="
        const val BOOK_LOAD_EVENT = "book load event"
        private const val MY_COMPAT_CSS_STYLE = "myCompatStyle.css"
        private const val MY_CSS_NIGHT_STYLE = "myNightMode.css"
        private const val MY_COMPAT_FAT_CSS_STYLE = "myCompatFatStyle.css"
        private const val MY_JS = "myJs.js"
        private const val HTML_TYPE = "text/html"
        private const val AJAX_REQUEST = "http://flibustahezeous3.onion/makebooklist?"
    }

    init {
        onionProxyManager = App.instance.mLoadedTor.value
    }
}