package net.veldor.flibustaloader.utils

import android.util.Log
import net.veldor.flibustaloader.ui.OPDSActivity

object URLHelper {
    @kotlin.jvm.JvmStatic
    fun getBaseUrl(): String {
        return "http://flibustahezeous3.onion"
    }

    @kotlin.jvm.JvmStatic
    fun getBaseOPDSUrl(): String {
        if (PreferencesHandler.instance.isCustomMirror) {
            return PreferencesHandler.instance.customMirror
        }
        if (PreferencesHandler.instance.isExternalVpn) {
            Log.d("surprise", "URLHelper getBaseOPDSUrl 18: use external vpn")
            return "http://flibusta.is"
        }
        return "http://flibustahezeous3.onion"
    }

    @kotlin.jvm.JvmStatic
    fun getFlibustaIsUrl(): String {
        return "http://flibusta.is"
    }

    @kotlin.jvm.JvmStatic
    fun getSearchRequest(searchType: String?, request: String?): String {
        // базовый URL зависит от исползуемого соединения
        val urlConstructor = StringBuilder()
        when {
            PreferencesHandler.instance.isCustomMirror -> {
                urlConstructor.append(PreferencesHandler.instance.customMirror)
                    .append("/opds/")
            }
            PreferencesHandler.instance.isExternalVpn -> {
                urlConstructor.append("http://flibusta.is/opds/")
            }
            else -> {
                urlConstructor.append("http://flibustahezeous3.onion/opds/")
            }
        }
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
            return PreferencesHandler.instance.customMirror
        }
        return PreferencesHandler.BASE_URL
    }
    @kotlin.jvm.JvmStatic
    fun getFlibustaMirrorUrl(): String{
        return PreferencesHandler.MIRROR_URL
    }
}