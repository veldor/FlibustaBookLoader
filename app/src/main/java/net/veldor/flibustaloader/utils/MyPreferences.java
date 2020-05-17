package net.veldor.flibustaloader.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.veldor.flibustaloader.App;

public class MyPreferences {

    private static final String SUBSCRIPTIONS_AUTO_CHECK_PREF = "subscriptions auto check";
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
}
