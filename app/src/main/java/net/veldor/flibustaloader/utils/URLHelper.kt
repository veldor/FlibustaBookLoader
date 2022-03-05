package net.veldor.flibustaloader.utils

import android.os.Build
import android.util.Log
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R

object URLHelper {
    @kotlin.jvm.JvmStatic
    fun getBaseUrl(): String {
        if (PreferencesHandler.instance.isCustomMirror) {
            val customMirror = PreferencesHandler.instance.customMirror!!
            if(customMirror.isNotEmpty()){
                return customMirror
            }
        }
        if (App.instance.useMirror) {
            return getFlibustaMirrorUrl()
        }
        return getFlibustaUrl()
    }

    @kotlin.jvm.JvmStatic
    fun getSearchRequest(searchType: Int, request: String?): String {
        // базовый URL зависит от исползуемого соединения
        val urlConstructor = StringBuilder()
        urlConstructor.append("/opds/")
        when (searchType) {
            R.id.searchBook -> urlConstructor.append("search?searchType=books&searchTerm=")
                .append(request)
            R.id.searchAuthor -> urlConstructor.append("search?searchType=authors&searchTerm=")
                .append(request)
            R.id.searchSequence -> urlConstructor.append("sequences/")
                .append(request)
            R.id.searchGenre -> urlConstructor.append("genres/")
                .append(request)
        }
        return urlConstructor.toString()
    }

    @kotlin.jvm.JvmStatic
    fun getFlibustaUrl(): String {
        if (PreferencesHandler.instance.isCustomMirror) {
            return PreferencesHandler.instance.customMirror.toString()
        }
        if(!PreferencesHandler.instance.isExternalVpn){
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                return PreferencesHandler.TOR_COMPAT_URL
            }
            return PreferencesHandler.TOR_URL
        }
        return PreferencesHandler.BASE_URL
    }

    @kotlin.jvm.JvmStatic
    fun getFlibustaMirrorUrl(): String {
        return PreferencesHandler.MIRROR_URL
    }
}