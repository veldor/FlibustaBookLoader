package net.veldor.flibustaloader.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.Html;
import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.selections.Author;

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

    public static int getRandom() {
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

    public static String createAuthorDirName(Author author) {
        String dirname;
        // разобью имя автора по пробелу
        String[] parts = author.name.split(" ");
        if (parts.length > 1) {
            dirname = parts[0] + ' ' + parts[1];
        } else {
            dirname = parts[0];
        }
        if (dirname.length() > 100) {
            return dirname.substring(0, 100);
        }
        return dirname;
    }

    public static String clearDirName(String dirName) {
        return dirName.replaceAll("[^ а-яА-Яa-zA-Z0-9.\\-]", "");
    }
}
