package net.veldor.flibustaloader.utils

import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import java.io.File

class PreferencesHandler private constructor() {
    private var preferences: SharedPreferences =
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(App.instance)


    var isLinearLayout: Boolean
        get() = preferences.getBoolean(PREF_LINEAR_LAYOUT, true)
        set(state) {
            preferences.edit().putBoolean(PREF_LINEAR_LAYOUT, state).apply()
        }


    var hardwareAcceleration: Boolean
        get() = preferences.getBoolean(HW_ACCELERATION_PREF, true)
        set(state) {
            preferences.edit().putBoolean(HW_ACCELERATION_PREF, state)
                    .apply()
        }


    var view: Int
        get() = preferences.getInt(PREF_VIEW, 0)
        set(viewType) {
            preferences.edit().putInt(PREF_VIEW, viewType).apply()
        }

    val isCheckUpdate: Boolean
        get() = preferences.getBoolean(PREF_CHECK_UPDATES, true)

    var isReDownload: Boolean
        get() = preferences.getBoolean(PREF_RE_DOWNLOAD, true)
        set(checked) {
            preferences.edit().putBoolean(PREF_RE_DOWNLOAD, checked).apply()
        }

    var lastLoadedUrl: String
        get() = preferences.getString(
                PREF_LAST_LOADED_URL,
                URLHelper.getBaseUrl()
        )!!
        set(url) {
            if (url != "http://flibustahezeous3.onion/favicon.ico" && url != "http://flibustahezeous3.onion/sites/default/files/bluebreeze_favicon.ico") preferences.edit()
                    .putString(
                            PREF_LAST_LOADED_URL, url
                    ).apply()
        }


    val picMirror: String
        get() = preferences.getString(
                App.instance.getString(R.string.pref_custom_pic_mirror), App.PIC_MIRROR_URL
        )!!

    var viewMode: Int
        get() = preferences.getInt(PREF_VIEW_MODE, App.VIEW_MODE_LIGHT)
        set(state) {
            var mode = 1
            when (state) {
                R.id.menuUseLightStyle -> {
                    mode = App.VIEW_MODE_LIGHT
                }
                R.id.menuUseLightFastStyle -> {
                    mode = App.VIEW_MODE_FAST
                }
                R.id.menuUseLightFatStyle -> {
                    mode = App.VIEW_MODE_FAT
                }
                R.id.menuUseFatFastStyle -> {
                    mode = App.VIEW_MODE_FAST_FAT
                }
            }
            preferences.edit().putInt(PREF_VIEW_MODE, mode).apply()
        }


    var nightMode: Boolean
        get() = preferences.getBoolean(PREF_NIGHT_MODE_ENABLED, false)
        set(state) {
            preferences.edit().putBoolean(PREF_NIGHT_MODE_ENABLED, state).apply()
        }

    val isEInk: Boolean
        get() = preferences.getBoolean(
                App.instance.getString(R.string.pref_is_eink),
                false
        )

    fun isShowChanges(): Boolean {
        // получу текущую версию приложения и последнюю версию, в которой отображались изменения
        val currentVersion = Grammar.appVersion
        val savedVersion = preferences.getString(LAST_CHANGELOG_VERSION_PREF, "0")
        return currentVersion != savedVersion
    }

    val isCustomMirror: Boolean
        get() = preferences.getBoolean(
                App.instance.getString(R.string.pref_use_custom_mirror),
                false
        )

    val customMirror: String
        get() = preferences.getString(
                App.instance.getString(R.string.pref_custom_flibusta_mirror),
                URLHelper.getBaseUrl()
        )!!

    var isSubscriptionsAutoCheck: Boolean
        get() = preferences.getBoolean(SUBSCRIPTIONS_AUTO_CHECK_PREF, false)
        set(state) {
            preferences.edit().putBoolean(SUBSCRIPTIONS_AUTO_CHECK_PREF, state).apply()
        }
    val isDownloadAutostart: Boolean
        get() = preferences.getBoolean(BOOKS_DOWNLOAD_AUTOSTART, false)

    val showDownloadProgress: Boolean
        get() = preferences.getBoolean(SHOW_DOWNLOAD_PROGRESS_PREF, false)

    var isPreviews: Boolean
        get() = preferences.getBoolean(PREF_PREVIEWS, true)
        set(state) {
            preferences.edit().putBoolean(PREF_PREVIEWS, state).apply()
        }
    var isHideDigests: Boolean
        get() = preferences.getBoolean(HIDE_DIGESTS_PREF, true)
        set(state) {
            preferences.edit().putBoolean(HIDE_DIGESTS_PREF, state).apply()
        }

    var isHideRead: Boolean
        get() = preferences.getBoolean(PREF_HIDE_READ, false)
        set(state) {
            preferences.edit().putBoolean(PREF_HIDE_READ, state).apply()
        }

    var isHideDownloaded: Boolean
        get() = preferences.getBoolean(HIDE_DOWNLOADED_PREF, false)
        set(state) {
            preferences.edit().putBoolean(HIDE_DOWNLOADED_PREF, state).apply()
        }

    var isOnlyRussian: Boolean
        get() = preferences.getBoolean(PREF_ONLY_RUSSIAN, false)
        set(state) {
            preferences.edit().putBoolean(PREF_ONLY_RUSSIAN, state).apply()
        }

    var isUseFilter: Boolean
        get() = preferences.getBoolean(PREF_USE_FILTER, false)
        set(state) {
            preferences.edit().putBoolean(PREF_USE_FILTER, state).apply()
        }


    var isDownloadAll: Boolean
        get() = preferences.getBoolean(PREF_LOAD_ALL, false)
        set(state) {
            preferences.edit().putBoolean(PREF_LOAD_ALL, state).apply()
        }

    var isExternalVpn: Boolean
        get() = preferences.getBoolean(PERF_EXTERNAL_VPN, false)
        set(state) {
            preferences.edit().putBoolean(PERF_EXTERNAL_VPN, state).apply()
        }


    var lastCheckedBookId: String
        get() = preferences.getString(PREF_LAST_CHECKED_BOOK, "tag:book:0")!!
        set(value) = preferences.edit().putString(PREF_LAST_CHECKED_BOOK, value).apply()

    var favoriteMime: String?
        get() {
            val favoriteFormat = preferences.getString(PREF_FAVORITE_MIME, null)
            return if (favoriteFormat == null || favoriteFormat.isEmpty()) {
                null
            } else favoriteFormat
        }
        set(value) {
            if (value == null) {
                preferences.edit().remove(PREF_FAVORITE_MIME).apply()
            } else {
                preferences.edit().putString(PREF_FAVORITE_MIME, value).apply()
            }
        }


    var authCookie: String?
        get() = preferences.getString(AUTH_COOKIE_VALUE, null)
        set(value) {
            if (value == null) {
                preferences.edit().remove(AUTH_COOKIE_VALUE).apply()
            } else {
                preferences.edit().putString(AUTH_COOKIE_VALUE, value).apply()
            }
        }


    var saveOnlySelected: Boolean
        get() = preferences.getBoolean(PREF_SAVE_ONLY_SELECTED, false)
        set(checked) {
            preferences.edit().putBoolean(PREF_SAVE_ONLY_SELECTED, checked).apply()
        }

    fun saveLastLoadedPage(s: String?) {
        preferences.edit().putString(LAST_SEARCH_URL_PREF, s).apply()
    }

    fun saveDownloadFolder(folderLocation: String?): Boolean {
        // ещё раз попробую создать файл
        val file = File(folderLocation)
        if (file.isDirectory) {
            preferences.edit().putString(PREF_DOWNLOAD_LOCATION, folderLocation)
                    .apply()
            return true
        }
        return false
    }

    // возвращу папку для закачек
    var downloadDir: DocumentFile?
        get() {
            // возвращу папку для закачек
            val downloadLocation =
                    preferences.getString(PREF_DOWNLOAD_LOCATION, null)
            if (downloadLocation != null) {
                try {
                    val dl = DocumentFile.fromTreeUri(App.instance, Uri.parse(downloadLocation))
                    if (dl != null && dl.isDirectory) {
                        return dl
                    }
                } catch (e: Exception) {
                    return null
                }
            }
            // верну путь к папке загрузок
            return null
        }
        set(file) {
            preferences.edit().putString(PREF_DOWNLOAD_LOCATION, file?.uri.toString()).apply()
        }


    var compatDownloadDir: File?
        get() {
            val downloadLocation = preferences.getString(PREF_DOWNLOAD_LOCATION, null)
            if (downloadLocation != null) {
                val file = File(downloadLocation)
                if (file.isDirectory) {
                    return file
                }
            }
            return null
        }
        set(file) {
            preferences.edit().putString(PREF_DOWNLOAD_LOCATION, file?.toUri().toString()).apply()
        }

    val downloadDirAssigned: Boolean
        get()
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return downloadDir != null
            }
            return compatDownloadDir != null
        }


fun getDownloadDirLocation(): String? {
    val dir = downloadDir
    if (dir != null && dir.isDirectory) {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            UriConverter.getPath(App.instance, dir.uri)
        } else {
            dir.uri.path
        }
    }
    val compatDir = compatDownloadDir
    return if (compatDir != null && compatDir.isDirectory) {
        compatDir.absolutePath
    } else "Не распознал папку загрузок"
}


fun setChangesViewed() {
    preferences.edit().putString(LAST_CHANGELOG_VERSION_PREF, Grammar.appVersion)
            .apply()
}


fun isCreateAuthorsDir(): Boolean {
    return preferences.getBoolean(
            App.instance.getString(R.string.pref_create_author_folder), false
    )
}

fun isCreateSequencesDir(): Boolean {
    return preferences.getBoolean(
            App.instance.getString(R.string.pref_create_sequence_folder), false
    )
}

fun isCreateAdditionalDir(): Boolean {
    return preferences.getBoolean(
            App.instance.getString(R.string.pref_create_additional_folders), false
    )
}


fun isAutofocusSearch(): Boolean {
    return preferences.getBoolean(
            App.instance.getString(R.string.pref_autostart_search), true
    )
}


fun setCreateAuthorsDir(checked: Boolean) {
    preferences.edit()
            .putBoolean(App.instance.getString(R.string.pref_create_author_folder), checked)
            .apply()
}

fun setCreateSequencesDir(checked: Boolean) {
    preferences.edit()
            .putBoolean(App.instance.getString(R.string.pref_create_sequence_folder), checked)
            .apply()
}


fun askedForDonation(): Boolean {
    return preferences.getBoolean(PREF_BEG_DONATION, false)
}


fun setDonationBegged() {
    preferences.edit().putBoolean(PREF_BEG_DONATION, true).apply()
}

fun setEInk(isChecked: Boolean) {
    preferences.edit()
            .putBoolean(App.instance.getString(R.string.pref_is_eink), isChecked).apply()
}

fun isCheckAvailability(): Boolean {
    return preferences.getBoolean(PREF_CHECK_AVAILABILITY, true)
}

fun setInspectionEnabled(isEnabled: Boolean) {
    preferences.edit().putBoolean(PREF_CHECK_AVAILABILITY, isEnabled).apply()
}

fun isSkipMainScreen(): Boolean {
    return preferences.getBoolean(PREF_SKIP_MAIN_SCREEN, false)
}

fun isShowLoadMoreBtn(): Boolean {
    return preferences.getBoolean(PREF_SHOW_LOAD_MORE_BTN, false)
}

fun isPicHide(): Boolean {
    return preferences.getBoolean(PREF_HIDE_PICS, false)
}


companion object {
    private const val PREF_LINEAR_LAYOUT = "linear layout"
    const val PREF_DOWNLOAD_LOCATION = "download_location"
    private const val PREF_VIEW = "view"
    private const val PREF_CHECK_UPDATES = "check_updates"
    private const val PREF_RE_DOWNLOAD = "re download"
    private const val PREF_LAST_LOADED_URL = "last_loaded_url"
    private const val PREF_VIEW_MODE = "view mode"
    private const val PREF_NIGHT_MODE_ENABLED = "night mode"
    private const val PREF_PREVIEWS = "cover_previews_show"
    private const val PREF_HIDE_READ = "hide read"
    private const val PREF_LOAD_ALL = "load all"
    private const val PREF_SAVE_ONLY_SELECTED = "save only selected"
    private const val PREF_FAVORITE_MIME = "favorite format"
    private const val PREF_LAST_CHECKED_BOOK = "last_checked_book"
    private const val PERF_EXTERNAL_VPN = "external vpn"
    private const val SUBSCRIPTIONS_AUTO_CHECK_PREF = "subscriptions auto check"
    private const val LAST_SEARCH_URL_PREF = "last load url"
    const val SHOW_DOWNLOAD_PROGRESS_PREF = "show download progress"
    private const val HW_ACCELERATION_PREF = "hardware acceleration"
    private const val HIDE_DIGESTS_PREF = "hide digests"
    private const val HIDE_DOWNLOADED_PREF = "hide downloaded"
    private const val LAST_CHANGELOG_VERSION_PREF = "last changelog version"
    private const val BOOKS_DOWNLOAD_AUTOSTART = "download auto start"
    private const val PREF_USE_FILTER = "use filter"
    private const val PREF_CHECK_AVAILABILITY = "check availability"
    private const val PREF_ONLY_RUSSIAN = "only russian"
    private const val AUTH_COOKIE_VALUE = "auth cookie value"
    private const val PREF_BEG_DONATION = "beg donation"
    private const val PREF_SKIP_MAIN_SCREEN = "skip load screen"
    private const val PREF_SHOW_LOAD_MORE_BTN = "show more btn"
    private const val PREF_HIDE_PICS = "clear view"

    const val BASE_URL = "http://flibustahezeous3.onion"
    const val MIRROR_URL = "http://flibusta.site"


    @JvmStatic
    var instance: PreferencesHandler = PreferencesHandler()
        private set
}

}