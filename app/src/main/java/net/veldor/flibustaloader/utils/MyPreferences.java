package net.veldor.flibustaloader.utils;

import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;

import java.io.File;

import static net.veldor.flibustaloader.App.PREFERENCE_DOWNLOAD_LOCATION;

public class MyPreferences {

    private static final String SUBSCRIPTIONS_AUTO_CHECK_PREF = "subscriptions auto check";
    private static final String LAST_SEARCH_URL_PREF = "last load url";
    public static final String SHOW_DOWNLOAD_PROGRESS_PREF = "show download progress";
    private static final String HW_ACCELERATION_PREF = "hardware acceleration";
    private static final String HIDE_DIGESTS_PREF = "hide digests";
    private static final String HIDE_DOWNLOADED_PREF = "hide downloaded";
    private static final String LAST_CHANGELOG_VERSION_PREF = "last changelog version";
    private static final String BOOKS_DOWNLOAD_AUTOSTART = "download auto start";
    private static final String PREF_USE_FILTER = "use filter";
    private static final String PREF_CHECK_AVAILABILITY = "check availability";
    private static final String PREF_ONLY_RUSSIAN = "only russian";
    private static final String AUTH_COOKIE_VALUE = "auth cookie value";
    private static final String PREF_BEG_DONATION = "beg donation";
    private static MyPreferences instance;
    private final SharedPreferences mSharedPreferences;

    public static MyPreferences getInstance() {
        if (instance == null) {
            instance = new MyPreferences();
        }
        return instance;
    }

    private MyPreferences() {
        // читаю настройки sharedPreferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
    }

    public boolean isSubscriptionsAutoCheck() {
        return mSharedPreferences.getBoolean(SUBSCRIPTIONS_AUTO_CHECK_PREF, false);
    }

    public void switchSubscriptionsAutoCheck() {
        mSharedPreferences.edit().putBoolean(SUBSCRIPTIONS_AUTO_CHECK_PREF, !isSubscriptionsAutoCheck()).apply();
    }

    public void saveLastLoadedPage(String s) {
        mSharedPreferences.edit().putString(LAST_SEARCH_URL_PREF, s).apply();
    }

    public boolean saveDownloadFolder(String folderLocation) {
        // ещё раз попробую создать файл
        File file = new File(folderLocation);
        if (file.isDirectory()) {
            mSharedPreferences.edit().putString(PREFERENCE_DOWNLOAD_LOCATION, folderLocation).apply();
            return true;
        }
        return false;
    }

    public File getDownloadDir() {
        String download_location = mSharedPreferences.getString(PREFERENCE_DOWNLOAD_LOCATION, null);
        Log.d("surprise", "MyPreferences getDownloadDir 62: dir is " + PREFERENCE_DOWNLOAD_LOCATION);
        if (download_location != null) {
            File file = new File(download_location);
            if (file.isDirectory()) {
                return file;
            }
        }
        return null;
    }

    public boolean isDownloadDir() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return App.getInstance().getDownloadDir() == null;
        } else {
            return getDownloadDir() == null;
        }
    }

    public boolean isHardwareAcceleration() {
        return mSharedPreferences.getBoolean(HW_ACCELERATION_PREF, true);
    }

    public void switchHardwareAcceleration() {
        mSharedPreferences.edit().putBoolean(HW_ACCELERATION_PREF, !isHardwareAcceleration()).apply();
    }

    public boolean isDigestsHide() {
        return mSharedPreferences.getBoolean(HIDE_DIGESTS_PREF, false);
    }

    public void switchDigestsHide() {
        mSharedPreferences.edit().putBoolean(HIDE_DIGESTS_PREF, !isDigestsHide()).apply();
    }

    public String getDownloadDirLocation() {
        DocumentFile dir = App.getInstance().getDownloadDir();
        if (dir != null && dir.isDirectory()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return UriConverter.getPath(App.getInstance(), dir.getUri());
            } else {
                return dir.getUri().getPath();
            }
        }
        File compatDir = getDownloadDir();
        if (compatDir != null && compatDir.isDirectory()) {
            return compatDir.getAbsolutePath();
        }
        return "Не распознал папку загрузок";
    }

    public boolean isDownloadedHide() {
        return mSharedPreferences.getBoolean(HIDE_DOWNLOADED_PREF, false);
    }

    public void switchDownloadedHide() {
        mSharedPreferences.edit().putBoolean(HIDE_DOWNLOADED_PREF, !isDownloadedHide()).apply();
    }

    public boolean isShowChanges() {
        // получу текущую версию приложения и последнюю версию, в которой отображались изменения
        String currentVersion = Grammar.getAppVersion();
        String savedVersion = mSharedPreferences.getString(LAST_CHANGELOG_VERSION_PREF, "0");
        return !currentVersion.equals(savedVersion);
    }

    public void setChangesViewed() {
        mSharedPreferences.edit().putString(LAST_CHANGELOG_VERSION_PREF, Grammar.getAppVersion()).apply();
    }


    public boolean isEInk() {
        return mSharedPreferences.getBoolean(App.getInstance().getString(R.string.pref_is_eink), false);
    }

    public boolean isCreateAuthorsDir() {
        return mSharedPreferences.getBoolean(App.getInstance().getString(R.string.pref_create_author_folder), false);
    }

    public boolean isCreateSequencesDir() {
        return mSharedPreferences.getBoolean(App.getInstance().getString(R.string.pref_create_sequence_folder), false);
    }

    public boolean isCreateAdditionalDir() {
        return mSharedPreferences.getBoolean(App.getInstance().getString(R.string.pref_create_additional_folders), false);
    }

    public boolean isDownloadAutostart() {
        return mSharedPreferences.getBoolean(BOOKS_DOWNLOAD_AUTOSTART, true);
    }

    public boolean isAutofocusSearch() {
        return mSharedPreferences.getBoolean(App.getInstance().getString(R.string.pref_autostart_search), true);
    }

    public boolean isCustomMirror() {
        return mSharedPreferences.getBoolean(App.getInstance().getString(R.string.pref_use_custom_mirror), false);
    }

    public String getCustomMirror() {
        return mSharedPreferences.getString(App.getInstance().getString(R.string.pref_custom_flibusta_mirror), URLHelper.getBaseUrl());
    }

    public void setCreateAuthorsDir(boolean checked) {
        mSharedPreferences.edit().putBoolean(App.getInstance().getString(R.string.pref_create_author_folder), checked).apply();
    }

    public void setCreateSequencesDir(boolean checked) {
        mSharedPreferences.edit().putBoolean(App.getInstance().getString(R.string.pref_create_sequence_folder), checked).apply();
    }

    public boolean isUseFilter() {
        return mSharedPreferences.getBoolean(PREF_USE_FILTER, true);
    }

    public void setUseFilter(boolean check) {
        mSharedPreferences.edit().putBoolean(PREF_USE_FILTER, check).apply();
    }

    public boolean isOnlyRussian() {
        return mSharedPreferences.getBoolean(PREF_ONLY_RUSSIAN, false);
    }

    public void setOnlyRussian(boolean check) {
        mSharedPreferences.edit().putBoolean(PREF_ONLY_RUSSIAN, check).apply();
    }

    public void saveLoginCookie(String cookie) {
        Log.d("surprise", "MyPreferences.java 196 saveLoginCookie: saving cookie " + cookie);
        mSharedPreferences.edit().putString(AUTH_COOKIE_VALUE, cookie).apply();
    }

    public String getAuthCookie() {
        return mSharedPreferences.getString(AUTH_COOKIE_VALUE, null);
    }

    public void removeAuthCookie() {
        mSharedPreferences.edit().remove(AUTH_COOKIE_VALUE).apply();
    }

    public boolean askedForDonation() {
        return mSharedPreferences.getBoolean(PREF_BEG_DONATION, false);
    }
    public boolean isShowDownloadProgress() {
        return mSharedPreferences.getBoolean(SHOW_DOWNLOAD_PROGRESS_PREF, false);
    }

    public void setDonationBegged() {
        mSharedPreferences.edit().putBoolean(PREF_BEG_DONATION, true).apply();
    }

    public void setEInk(boolean isChecked) {
        mSharedPreferences.edit().putBoolean(App.getInstance().getString(R.string.pref_is_eink), isChecked).apply();
    }

    public boolean isCheckAvailability() {
        return mSharedPreferences.getBoolean(PREF_CHECK_AVAILABILITY, true);
    }

    public void setInspectionEnabled(boolean isEnabled) {
        mSharedPreferences.edit().putBoolean(PREF_CHECK_AVAILABILITY, isEnabled).apply();
    }
}