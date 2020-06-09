package net.veldor.flibustaloader.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.Html;
import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;

import java.util.Locale;
import java.util.Random;

public class Grammar {
    @SuppressWarnings("deprecation")
    public static String textFromHtml(String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return (Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)).toString();
        } else {
            return (Html.fromHtml(html)).toString();
        }
    }
    public static int getRandom(){
        int min = 1000;
        int max = 9999;

        Random r = new Random();
        return r.nextInt(max - min + 1) + min;
    }

    public static String getAppVersion() {
        try {
            PackageInfo pInfo = App.getInstance().getPackageManager().getPackageInfo(App.getInstance().getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("surprise", "MainActivity setupUI: can't found version");
            e.printStackTrace();
        }
        return "0";
    }
}
