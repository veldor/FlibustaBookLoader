package net.veldor.flibustaloader.http

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.util.EntityUtils
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.ecxeptions.BookNotFoundException
import net.veldor.flibustaloader.ecxeptions.ConnectionLostException
import net.veldor.flibustaloader.ecxeptions.ZeroBookSizeException
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

object GlobalWebClient {
    @JvmField
    val mConnectionState = MutableLiveData<Int?>()
    const val CONNECTED = 1
    const val DISCONNECTED = 2
    @Throws(IOException::class)
    fun request(requestString: String): String? {
        // если используется внешний VPN- выполню поиск в нём, иначае- в TOR
        if (PreferencesHandler.instance.isExternalVpn) {
            val response = ExternalVpnVewClient.rawRequest(requestString)
            if (response != null) {
                return EntityUtils.toString(response.entity)
            }
        } else {
            val response = MirrorRequestClient().request(requestString)
            if (response == null) {
                // сначала попробую запросить инфу с зеркала
                val webClient: TorWebClient = try {
                    TorWebClient()
                } catch (e: ConnectionLostException) {
                    return null
                }
                return webClient.request(requestString)
            }
            return response
        }
        return null
    }

    @Throws(BookNotFoundException::class)
    fun handleBookLoadRequest(response: HttpResponse?, newFile: DocumentFile) {
        try {
            if (response != null) {
                val status = response.statusLine.statusCode
                if (status == 200) {
                    val entity = response.entity
                    if (entity != null) {
                        val showDownloadProgress = PreferencesHandler.instance.showDownloadProgress
                        val contentLength = entity.contentLength
                        val startTime = System.currentTimeMillis()
                        var lastNotificationTime = System.currentTimeMillis()
                        val fileName = newFile.name!!
                        val notifier = NotificationHandler.instance
                        if (contentLength > 0) {
                            // создам уведомление, в котором буду показывать прогресс скачивания
                            if (showDownloadProgress) {
                                notifier.createBookLoadingProgressNotification(
                                    contentLength.toInt(),
                                    0,
                                    fileName,
                                    startTime
                                )
                            }
                        }
                        val content = entity.content
                        if (content != null && entity.contentLength > 0) {
                            val out =
                                App.instance.contentResolver.openOutputStream(newFile.uri)
                            if (out != null) {
                                var read: Int
                                val buffer = ByteArray(4092)
                                while (content.read(buffer).also { read = it } > 0) {
                                    out.write(buffer, 0, read)
                                    if (contentLength > 0 && showDownloadProgress && lastNotificationTime + 1000 < System.currentTimeMillis()) {
                                        lastNotificationTime = System.currentTimeMillis()
                                        notifier.createBookLoadingProgressNotification(
                                            contentLength.toInt(),
                                            newFile.length().toInt(),
                                            fileName,
                                            startTime
                                        )
                                    }
                                }
                                out.close()
                                content.close()
                                notifier.cancelBookLoadingProgressNotification()
                                if (newFile.length() > 0) {
                                    return
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("surprise", "GlobalWebClient handleBookLoadRequest 103: have error " + e.message)
        }
        // если что-то пошло не так- удалю файл и покажу ошибку скачивания
        newFile.delete()
        throw BookNotFoundException()
    }

    @Throws(BookNotFoundException::class)
    fun handleBookLoadRequest(response: HttpResponse?, newFile: File) {
        try {
            if (response != null) {
                val status = response.statusLine.statusCode
                if (status == 200) {
                    val entity = response.entity
                    if (entity != null) {
                        val contentLength = entity.contentLength
                        val startTime = System.currentTimeMillis()
                        val fileName = newFile.name
                        val notifier = NotificationHandler.instance
                        if (contentLength > 0) {
                            // создам уведомление, в котором буду показывать прогресс скачивания
                            notifier.createBookLoadingProgressNotification(
                                contentLength.toInt(),
                                0,
                                fileName,
                                startTime
                            )
                        }
                        val content = entity.content
                        if (content != null && entity.contentLength > 0) {
                            val out: OutputStream = FileOutputStream(newFile)
                            var read: Int
                            val buffer = ByteArray(1024)
                            while (content.read(buffer).also { read = it } > 0) {
                                out.write(buffer, 0, read)
                                notifier.createBookLoadingProgressNotification(
                                    contentLength.toInt(),
                                    newFile.length().toInt(),
                                    fileName,
                                    startTime
                                )
                            }
                            out.close()
                            content.close()
                            notifier.cancelBookLoadingProgressNotification()
                            if (newFile.length() > 0) {
                                return
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("surprise", "GlobalWebClient handleBookLoadRequest 103: have error " + e.message)
            e.printStackTrace()
        }
        // если что-то пошло не так- удалю файл и покажу ошибку скачивания
        val deleteResult = newFile.delete()
        Log.d(
            "surprise",
            "TorWebClient downloadBook: книга не найдена, статус удаления файла: $deleteResult"
        )
        throw BookNotFoundException()
    }

    fun handleBookLoadRequestNoContentLength(
        response: HttpResponse?,
        newFile: DocumentFile
    ): Boolean {
        try {
            if (response != null) {
                val status = response.statusLine.statusCode
                if (status == 200) {
                    val entity = response.entity
                    if (entity != null) {
                        val content = entity.content
                        if (content != null) {
                            val out =
                                App.instance.contentResolver.openOutputStream(newFile.uri)
                            if (out != null) {
                                var read: Int
                                val buffer = ByteArray(1024)
                                while (content.read(buffer).also { read = it } > 0) {
                                    out.write(buffer, 0, read)
                                }
                                out.close()
                                content.close()
                                return if (newFile.length() > 0) {
                                    Log.d(
                                        "surprise",
                                        "TorWebClient downloadBook 190: file founded and saved to " + newFile.uri
                                    )
                                    true
                                } else {
                                    throw ZeroBookSizeException()
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("surprise", "GlobalWebClient handleBookLoadRequest 103: have error " + e.message)
        }
        // если что-то пошло не так- удалю файл и покажу ошибку скачивания
        newFile.delete()
        Log.d("surprise", "TorWebClient downloadBook: книга не найдена")
        return false
    }

    fun handleBookLoadRequestNoContentLength(response: HttpResponse?, newFile: File): Boolean {
        try {
            if (response != null) {
                val status = response.statusLine.statusCode
                if (status == 200) {
                    val entity = response.entity
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
                            Log.d(
                                "surprise",
                                "TorWebClient downloadBook 188: created file length is " + newFile.length()
                            )
                            if (newFile.length() > 0) {
                                Log.d(
                                    "surprise",
                                    "TorWebClient downloadBook 190: file founded and saved to " + newFile.absolutePath
                                )
                                return true
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("surprise", "GlobalWebClient handleBookLoadRequest 103: have error " + e.message)
        }
        // если что-то пошло не так- удалю файл и покажу ошибку скачивания
        val deleteResult = newFile.delete()
        Log.d(
            "surprise",
            "TorWebClient downloadBook: книга не найдена, статус удаления файла: $deleteResult"
        )
        return false
    }

    @Throws(IOException::class)
    fun requestNoMirror(requestString: String?): String? {
        // если используется внешний VPN- выполню поиск в нём, иначае- в TOR
        if (PreferencesHandler.instance.isExternalVpn) {
            val response = ExternalVpnVewClient.rawRequest(requestString)
            if (response != null) {
                return EntityUtils.toString(response.entity)
            }
        } else {
            // сначала попробую запросить инфу с зеркала
            val webClient: TorWebClient = try {
                TorWebClient()
            } catch (e: ConnectionLostException) {
                return null
            }
            return webClient.requestNoMirror(requestString)
        }
        return null
    }
}