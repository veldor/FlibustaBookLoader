package net.veldor.flibustaloader.http

import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.impl.client.HttpClients
import cz.msebera.android.httpclient.util.EntityUtils
import cz.msebera.android.httpclient.client.ResponseHandler
import android.util.Log
import cz.msebera.android.httpclient.HttpResponse
import java.io.IOException

class MirrorRequestClient {
    private var responseString: String? = null
    fun request(requestString: String?): String? {
        responseString = null
        val httpclient = HttpClients.createDefault()
        val httpget = HttpGet(requestString)
        try {
            // кастомный обработчик ответов
            val responseHandler = ResponseHandler<String?> { response: HttpResponse ->
                val status = response.statusLine.statusCode
                if (status == 200) {
                    val entity = response.entity
                    try {
                        val body = EntityUtils.toString(entity)
                        if (body != null && body.isNotEmpty()) {
                            responseString = body
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    // неверный ответ с сервера
                    Log.d(
                        "surprise",
                        "CheckUpdateWorker handleResponse: wrong update server answer"
                    )
                }
                null
            }
            // выполню запрос
            httpclient.execute(httpget, responseHandler)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                // по-любому закрою клиент
                httpclient.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return responseString
    }
}