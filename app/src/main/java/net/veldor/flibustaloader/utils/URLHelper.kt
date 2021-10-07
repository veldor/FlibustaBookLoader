package net.veldor.flibustaloader.utils

import net.veldor.flibustaloader.App

object URLHelper {
    @kotlin.jvm.JvmStatic
    fun getBaseUrl(): String {
        if(PreferencesHandler.instance.isCustomMirror){
            return PreferencesHandler.instance.customMirror!!
        }
        if (App.instance.useMirror) {
            return getFlibustaMirrorUrl()
        }
        return getFlibustaUrl()
    }

    @kotlin.jvm.JvmStatic
    fun getSearchRequest(searchBook: Boolean, request: String?): String {
        // базовый URL зависит от исползуемого соединения
        val urlConstructor = StringBuilder()
        urlConstructor.append("/opds/")
        if (searchBook) {
            urlConstructor.append("search?searchType=books&searchTerm=")
                .append(request)
        } else {
            urlConstructor.append("search?searchType=authors&searchTerm=")
                .append(request)
        }
        return urlConstructor.toString()
    }

    @kotlin.jvm.JvmStatic
    fun getFlibustaUrl(): String {
        if (PreferencesHandler.instance.isCustomMirror) {
            return PreferencesHandler.instance.customMirror.toString()
        }
        return PreferencesHandler.BASE_URL
    }

    @kotlin.jvm.JvmStatic
    fun getFlibustaMirrorUrl(): String {
        return PreferencesHandler.MIRROR_URL
    }
}