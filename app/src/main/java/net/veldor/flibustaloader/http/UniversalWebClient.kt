package net.veldor.flibustaloader.http

import android.net.Uri
import android.util.Log
import cz.msebera.android.httpclient.HttpResponse
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper
import java.io.BufferedReader

class UniversalWebClient {
    fun rawRequest(request: String) : HttpResponse?{
        val requestString = URLHelper.getBaseUrl() + request
        //Log.d("surprise", "make universal request: $requestString")
        if(PreferencesHandler.instance.isExternalVpn){
            return ExternalVpnVewClient.rawRequest(requestString)
        }
        return TorWebClient().rawRequest(requestString)
    }
    fun picRequest(request: String) : HttpResponse?{
        val requestString = PreferencesHandler.instance.picMirror + request
        //Log.d("surprise", "make universal request: $requestString")
        if(PreferencesHandler.instance.isExternalVpn){
            return ExternalVpnVewClient.rawRequest(requestString)
        }
        return TorWebClient().rawRequest(requestString)
    }

    fun responseToString(response: HttpResponse?): String? {
        if(response == null) return null
        if (response.statusLine.statusCode > 399) return null
        val reader = BufferedReader(response.entity.content.reader())
        reader.use { read ->
            return read.readText()
        }
    }
    fun responseFullToString(response: HttpResponse?): String? {
        if(response == null) return null
        val reader = BufferedReader(response.entity.content.reader())
        reader.use { read ->
            return read.readText()
        }
    }


    fun loginRequest(login: String, password: String): Boolean {
        if (PreferencesHandler.instance.isExternalVpn) {
            return ExternalVpnVewClient.loginRequest(URLHelper.getBaseUrl() + "/node?destination=node", login, password)
        }
        val request = Uri.parse(URLHelper.getBaseUrl() + "/node?destination=node")
        return TorWebClient().login(request, login, password)
    }
}