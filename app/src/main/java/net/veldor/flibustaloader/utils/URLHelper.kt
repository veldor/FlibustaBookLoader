package net.veldor.flibustaloader.utils

import android.util.Log
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.ui.OPDSActivity

object URLHelper {
    @kotlin.jvm.JvmStatic
    fun getBaseUrl(): String {
        if(App.instance.useMirror){
            return getFlibustaMirrorUrl()
        }
        return getFlibustaUrl()
    }

    @kotlin.jvm.JvmStatic
    fun getSearchRequest(searchType: String?, request: String?): String {
        // базовый URL зависит от исползуемого соединения
        val urlConstructor = StringBuilder()
        urlConstructor.append("/opds/")
        when (searchType) {
            OPDSActivity.SEARCH_TYPE_BOOKS -> urlConstructor.append("search?searchType=books&searchTerm=")
                .append(request)
            OPDSActivity.SEARCH_TYPE_AUTHORS -> urlConstructor.append("search?searchType=authors&searchTerm=")
                .append(request)
            OPDSActivity.SEARCH_TYPE_SEQUENCES, OPDSActivity.SEARCH_TYPE_GENRE -> urlConstructor.append(
                request
            )
        }
        return urlConstructor.toString()
    }

    @kotlin.jvm.JvmStatic
    fun getFlibustaUrl(): String{
        if(PreferencesHandler.instance.isCustomMirror){
            return PreferencesHandler.instance.customMirror.toString()
        }
        return PreferencesHandler.BASE_URL
    }
    @kotlin.jvm.JvmStatic
    fun getFlibustaMirrorUrl(): String{
        return PreferencesHandler.MIRROR_URL
    }
}