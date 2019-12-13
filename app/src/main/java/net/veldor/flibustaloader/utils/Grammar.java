package net.veldor.flibustaloader.utils;

import android.os.Build;
import android.text.Html;

import java.util.Random;

public class Grammar {
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
}
