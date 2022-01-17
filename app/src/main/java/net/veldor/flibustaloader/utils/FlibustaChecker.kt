package net.veldor.flibustaloader.utils

import android.util.Log
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient
import cz.msebera.android.httpclient.impl.client.HttpClients
import cz.msebera.android.httpclient.util.EntityUtils
import org.json.JSONObject

class FlibustaChecker {

    companion object {
        private val apiRequest =
            "https://check-host.net/check-http?host=http://flibusta.is&max_nodes=3"
        private const val apiResultRequest =
            "https://check-host.net/check-result/"
        const val STATE_WAITING = -1
        const val STATE_RUNNING = 1
        const val STATE_AVAILABLE = 2
        const val STATE_UNAVAILABLE = 3
        const val STATE_PASSED = 4
    }

    fun isAlive(): Int {
        val httpclient: CloseableHttpClient = HttpClients.createDefault()
        var httpget = HttpGet(apiRequest)
        Log.d("surprise", "FlibustaChecker.kt 27 isAlive ping flibusta on $httpget")
        httpget.addHeader("Accept", "application/json")
        var response = httpclient.execute(httpget)
        if (response.statusLine.statusCode == 200) {
            // parse json response
            var body: String = EntityUtils.toString(response.entity)
            var info = JSONObject(body)
            if (info.has("ok")) {
                val result = info.getInt("ok")
                if (result == 1) {
                    if (info.has("request_id")) {
                        val requestId = info.getString("request_id")
                        if (requestId.isNotEmpty()) {
                            while (true) {
                                Thread.sleep(500)
                                // запрошу результаты
                                httpget = HttpGet(apiResultRequest.plus(requestId))
                                httpget.addHeader("Accept", "application/json")
                                response = httpclient.execute(httpget)
                                if (response.statusLine.statusCode == 200) {
                                    body = EntityUtils.toString(response.entity)
                                    info = JSONObject(body)
                                    val keys = info.keys()
                                    var failed = 0
                                    keys.forEach {
                                        if (!info.isNull(it)) {
                                            val answerItem = info.getJSONArray(it)
                                            if (answerItem != null) {
                                                val availability = answerItem.getJSONArray(0)
                                                if (availability != null) {
                                                    val state = availability.getInt(0)
                                                    if (state == 1) {
                                                        return STATE_AVAILABLE
                                                    } else if (state == 0) {
                                                        Log.d(
                                                            "surprise",
                                                            "FlibustaChecker.kt 55 isAlive looks like it's dead"
                                                        )
                                                        failed++
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (failed > 0) {
                                        return STATE_UNAVAILABLE
                                    }
                                } else {
                                    break
                                }
                            }
                        }
                    }
                } else {
                    Log.d("surprise", "FlibustaChecker.kt 31 isAlive wrong request")
                }
            }
        }
        return STATE_PASSED
    }
}