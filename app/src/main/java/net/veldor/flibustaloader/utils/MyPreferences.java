package net.veldor.flibustaloader.utils;

import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import net.veldor.flibustaloader.App;

import java.io.File;

import static net.veldor.flibustaloader.App.PREFERENCE_DOWNLOAD_LOCATION;

public class MyPreferences {

    private static final String SUBSCRIPTIONS_AUTO_CHECK_PREF = "subscriptions auto check";
    private static final String LAST_SEARCH_URL_PREF = "last load url";
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
            return App.getInstance().getDownloadDir() != null;
        }
        else{
            return getDownloadDir() != null;
        }
    }
}
