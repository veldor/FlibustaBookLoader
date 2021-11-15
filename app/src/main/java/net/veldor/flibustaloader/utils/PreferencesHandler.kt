package net.veldor.flibustaloader.utils

import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.ui.fragments.WebViewFragment
import java.io.File

class PreferencesHandler private constructor() {

    var openedFromOpds = false

    private var preferences: SharedPreferences =
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(App.instance)


    var isTogglePanels: Boolean
        get() = preferences.getBoolean(PREF_TOGGLE_PANELS, true)
        set(state) {
            preferences.edit().putBoolean(PREF_TOGGLE_PANELS, state).apply()
        }


    var isAuthorInBookName: Boolean
        get() = preferences.getBoolean(PREF_AUTHOR_IN_BOOK_NAME, true)
        set(state) {
            preferences.edit().putBoolean(PREF_AUTHOR_IN_BOOK_NAME, state).apply()
        }

    var isSequenceInBookName: Boolean
        get() = preferences.getBoolean(PREF_SEQUENCE_IN_BOOK_NAME, true)
        set(state) {
            preferences.edit().putBoolean(PREF_SEQUENCE_IN_BOOK_NAME, state).apply()
        }

    fun isHideButtons(): Boolean {
        return preferences.getBoolean(PREF_HIDE_BUTTONS, false)
    }
    fun isFilterByLongClick(): Boolean {
        return preferences.getBoolean("add to filter on long click", false)
    }


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
            ""
        )!!
        set(url) {
            if (!url.contains("/favicon.ico") && !url.contains("/sites/default/files/bluebreeze_favicon.ico")) preferences.edit()
                .putString(
                    PREF_LAST_LOADED_URL, url.replace(URLHelper.getBaseUrl(), "")
                ).apply()
        }

    var viewMode: Int
        get() = preferences.getInt(PREF_VIEW_MODE, WebViewFragment.VIEW_MODE_LIGHT)
        set(state) {
            var mode = 1
            when (state) {
                R.id.menuUseLightStyle -> {
                    mode = WebViewFragment.VIEW_MODE_LIGHT
                }
                R.id.menuUseLightFastStyle -> {
                    mode = WebViewFragment.VIEW_MODE_FAST
                }
                R.id.menuUseLightFatStyle -> {
                    mode = WebViewFragment.VIEW_MODE_FAT
                }
                R.id.menuUseFatFastStyle -> {
                    mode = WebViewFragment.VIEW_MODE_FAST_FAT
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

    var isCustomMirror: Boolean
        get() = preferences.getBoolean(
            App.instance.getString(R.string.pref_use_custom_mirror),
            false
        )
        set(state) {
            preferences.edit()
                .putBoolean(App.instance.getString(R.string.pref_use_custom_mirror), state)
                .apply()
        }

    var customMirror: String?
        get() = preferences.getString(
            App.instance.getString(R.string.pref_custom_flibusta_mirror),
            BASE_URL
        )
        set(state) {
            preferences.edit()
                .putString(App.instance.getString(R.string.pref_custom_flibusta_mirror), state)
                .apply()
        }
    var picMirror: String?
        get() = preferences.getString(
            App.instance.getString(R.string.pref_custom_pic_mirror),
            BASE_PIC_URL
        )
        set(state) {
            preferences.edit()
                .putString(App.instance.getString(R.string.pref_custom_pic_mirror), state)
                .apply()
        }

    var isSubscriptionsAutoCheck: Boolean
        get() = preferences.getBoolean(SUBSCRIPTIONS_AUTO_CHECK_PREF, false)
        set(state) {
            preferences.edit().putBoolean(SUBSCRIPTIONS_AUTO_CHECK_PREF, state).apply()
        }
    val isDownloadAutostart: Boolean
        get() = preferences.getBoolean(BOOKS_DOWNLOAD_AUTOSTART, true)

    val showDownloadProgress: Boolean
        get() = preferences.getBoolean(SHOW_DOWNLOAD_PROGRESS_PREF, false)

    var isPreviews: Boolean
        get() = preferences.getBoolean(PREF_PREVIEWS, true)
        set(state) {
            preferences.edit().putBoolean(PREF_PREVIEWS, state).apply()
        }
    var isHideDigests: Boolean
        get() = preferences.getBoolean(HIDE_DIGESTS_PREF, false)
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

    fun bookNameStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_BOOK_NAME_STRICT_FILTER, false)
    }

    fun bookAuthorStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_BOOK_AUTHOR_STRICT_FILTER, false)
    }

    fun bookGenreStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_BOOK_GENRE_STRICT_FILTER, false)
    }

    fun bookSequenceStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_BOOK_SEQUENCE_STRICT_FILTER, false)
    }

    fun sequenceStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_SEQUENCE_STRICT_FILTER, false)
    }

    fun authorStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_AUTHOR_STRICT_FILTER, false)
    }

    fun genreStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_SEQUENCE_STRICT_FILTER, false)
    }

    var isUseFilter: Boolean
        get() = preferences.getBoolean(PREF_USE_FILTER, false)
        set(state) {
            preferences.edit().putBoolean(PREF_USE_FILTER, state).apply()
        }


    var opdsPagedResultsLoad: Boolean
        get() = preferences.getBoolean(PREF_LOAD_ALL, true)
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

    fun getDownloadDir(): DocumentFile? {
        var dl: DocumentFile? = null
        // возвращу папку для закачек
        val downloadLocation =
            preferences.getString(PREF_DOWNLOAD_LOCATION, null)
        if (downloadLocation != null) {
            try {
                dl = DocumentFile.fromTreeUri(App.instance, Uri.parse(downloadLocation))
            } catch (e: Exception) {
            }
        }
        // верну путь к папке загрузок
        return dl
    }

    fun setDownloadDir(file: DocumentFile?) {
        preferences.edit().putString(PREF_DOWNLOAD_LOCATION, file?.uri.toString()).apply()
    }

    fun getCompatDownloadDir(): File? {
        var dd: File? = null
        val downloadLocation = preferences.getString(PREF_DOWNLOAD_LOCATION, null)
        if (downloadLocation != null) {
            dd = File(downloadLocation)
        }
        return dd
    }

    fun setDownloadDir(file: File?) {
        preferences.edit().putString(PREF_DOWNLOAD_LOCATION, file?.toUri().toString()).apply()
    }

    val downloadDirAssigned: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return getDownloadDir() != null
            }
            return getCompatDownloadDir() != null
        }


    fun getDownloadDirLocation(): String? {
        val dir = getDownloadDir()
        if (dir != null && dir.isDirectory) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                UriConverter.getPath(App.instance, dir.uri)
            } else {
                dir.uri.path
            }
        }
        val compatDir = getCompatDownloadDir()
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


    fun isLoadSequencesInAuthorDir(): Boolean {
        return preferences.getBoolean(
            App.instance.getString(R.string.pref_load_series_to_author_dir),
            true
        )
    }

    fun setLoadSequencesInAuthorDir(value: Boolean) {
        preferences.edit()
            .putBoolean(
                App.instance.getString(R.string.pref_load_series_to_author_dir),
                value
            ).apply()
    }

    fun isCreateSequencesDir(): Boolean {
        return preferences.getBoolean(
            App.instance.getString(R.string.pref_create_sequence_folder), false
        )
    }

    fun isDifferentDirForAuthorAndSequence(): Boolean {
        return preferences.getBoolean(
            App.instance.getString(R.string.pref_create_additional_folders),
            false
        )
    }

    fun setDifferentDirForAuthorAndSequence(value: Boolean) {
        preferences.edit()
            .putBoolean(
                App.instance.getString(R.string.pref_create_additional_folders),
                value
            ).apply()
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
        private const val PREF_HIDE_BUTTONS = "no item buttons"
        private const val PREF_DIFFERENT_DIRS = "create additional folders"
        private const val PREF_SEQUENCES_IN_AUTHOR_DIRS = "load series to author dir"
        private const val PREF_TOGGLE_PANELS = "toggle panels"
        private const val PREF_AUTHOR_IN_BOOK_NAME = "pref author in book name"
        private const val PREF_SEQUENCE_IN_BOOK_NAME = "pref sequence in book name"
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
        private const val PREF_FAVORITE_MIME = "favorite format"
        private const val PREF_LAST_CHECKED_BOOK = "last_checked_book"
        private const val PERF_EXTERNAL_VPN = "external vpn"
        private const val SUBSCRIPTIONS_AUTO_CHECK_PREF = "subscriptions auto check"
        const val SHOW_DOWNLOAD_PROGRESS_PREF = "show download progress"
        private const val HW_ACCELERATION_PREF = "hardware acceleration"
        private const val HIDE_DIGESTS_PREF = "hide digests"
        private const val HIDE_DOWNLOADED_PREF = "hide downloaded"
        private const val LAST_CHANGELOG_VERSION_PREF = "last changelog version"
        private const val BOOKS_DOWNLOAD_AUTOSTART = "download auto start"
        private const val PREF_USE_FILTER = "use filter"
        private const val PREF_CHECK_AVAILABILITY = "check availability"
        private const val PREF_ONLY_RUSSIAN = "only russian"
        private const val PREF_BOOK_NAME_STRICT_FILTER = "strict name in books"
        private const val PREF_BOOK_AUTHOR_STRICT_FILTER = "strict author in books"
        private const val PREF_BOOK_GENRE_STRICT_FILTER = "strict genre in books"
        private const val PREF_BOOK_SEQUENCE_STRICT_FILTER = "strict sequence in books"
        private const val PREF_SEQUENCE_STRICT_FILTER = "strict sequence filter"
        private const val PREF_GENRE_STRICT_FILTER = "strict genre filter"
        private const val PREF_AUTHOR_STRICT_FILTER = "strict author filter"
        private const val AUTH_COOKIE_VALUE = "auth cookie value"
        private const val PREF_BEG_DONATION = "beg donation"
        private const val PREF_SKIP_MAIN_SCREEN = "skip load screen"
        private const val PREF_SHOW_LOAD_MORE_BTN = "show more btn"
        private const val PREF_HIDE_PICS = "clear view"

        const val BASE_URL = "http://flibusta.is"
        const val BASE_PIC_URL = "http://flibusta.is"
        const val MIRROR_URL = "http://flibusta.site"


        @JvmStatic
        var instance: PreferencesHandler = PreferencesHandler()
            private set
    }

}