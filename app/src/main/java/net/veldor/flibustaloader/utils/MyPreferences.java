package net.veldor.flibustaloader.utils;

import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import net.veldor.flibustaloader.App;

import java.io.File;

import static net.veldor.flibustaloader.App.PREFERENCE_DOWNLOAD_LOCATION;

public class MyPreferences {

    private static final String SUBSCRIPTIONS_AUTO_CHECK_PREF = "subscriptions auto check";
    private static final String LAST_SEARCH_URL_PREF = "last load url";
    private static final String HW_ACCELERATION_PREF = "hardware acceleration";
    private static final String HIDE_DIGESTS_PREF = "hide digests";
    private static final String HIDE_DOWNLOADED_PREF = "hide downloaded";
    private static final String LAST_CHANGELOG_VERSION_PREF = "last changelog version";
    private static MyPreferences instance;
    private final SharedPreferences mSharedPreferences;

    public static MyPreferences getInstance(){
        if(instance == null){
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

    public void switchSubscriptionsAutoCheck(){
        mSharedPreferences.edit().putBoolean(SUBSCRIPTIONS_AUTO_CHECK_PREF, !isSubscriptionsAutoCheck()).apply();
    }

    public void saveLastLoadedPage(String s) {
        mSharedPreferences.edit().putString(LAST_SEARCH_URL_PREF, s).apply();
    }

    public String getLastLoadUrl(){
        return mSharedPreferences.getString(LAST_SEARCH_URL_PREF, null);
    }

    public boolean saveDownloadFolder(String folderLocation) {
        // ещё раз попробую создать файл
        File file = new File(folderLocation);
        if(file.isDirectory()){
            mSharedPreferences.edit().putString(PREFERENCE_DOWNLOAD_LOCATION, folderLocation).apply();
            return true;
        }
        return false;
    }

    public File getDownloadDir(){
        String download_location = mSharedPreferences.getString(PREFERENCE_DOWNLOAD_LOCATION, null);
        Log.d("surprise", "MyPreferences getDownloadDir 62: dir is " + PREFERENCE_DOWNLOAD_LOCATION);
        if(download_location != null){
            File file = new File(download_location);
            if(file.isDirectory()){
                return file;
            }
        }
        return null;
    }

    public boolean isDownloadDir() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return App.getInstance().getDownloadDir() == null;
        }
        else{
            return getDownloadDir() == null;
        }
    }

    public boolean isHardwareAcceleration() {
        return mSharedPreferences.getBoolean(HW_ACCELERATION_PREF, true);
    }

    public void switchHardwareAcceleration(){
        mSharedPreferences.edit().putBoolean(HW_ACCELERATION_PREF, !isHardwareAcceleration()).apply();
    }

    public boolean isDigestsHide() {
        return mSharedPreferences.getBoolean(HIDE_DIGESTS_PREF, false);
    }

    public void switchDigestsHide(){
        mSharedPreferences.edit().putBoolean(HIDE_DIGESTS_PREF, !isDigestsHide()).apply();
    }

    public String getDownloadDirLocation() {
        DocumentFile dir = App.getInstance().getDownloadDir();
        if(dir.isDirectory()){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return UriConverter.getPath(App.getInstance(), dir.getUri());
            }
            else{
                return dir.getUri().getPath();
            }
        }
        File compatDir = getDownloadDir();
        if(compatDir.isDirectory()){
            return compatDir.getAbsolutePath();
        }
        return "Не распознал папку загрузок";
    }

    public boolean isDownloadedHide() {
        return mSharedPreferences.getBoolean(HIDE_DOWNLOADED_PREF, false);
    }
    public void switchDownloadedHide(){
        mSharedPreferences.edit().putBoolean(HIDE_DOWNLOADED_PREF, !isDownloadedHide()).apply();
    }

    public boolean isShowChanges() {
        // получу текущую версию приложения и последнюю версию, в которой отображались изменения
        String currentVersion = Grammar.getAppVersion();
        String savedVersion = mSharedPreferences.getString(LAST_CHANGELOG_VERSION_PREF, "0");
        return  !currentVersion.equals(savedVersion);
    }

    public void setChangesViewed() {
        mSharedPreferences.edit().putString(LAST_CHANGELOG_VERSION_PREF, Grammar.getAppVersion()).apply();
    }
}
