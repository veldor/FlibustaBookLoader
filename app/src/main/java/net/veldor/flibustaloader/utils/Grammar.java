package net.veldor.flibustaloader.utils;

import android.os.Build;
import android.text.Html;

public class Grammar {
    public static String textFromHtml(String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return (Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)).toString();
        } else {
            return (Html.fromHtml(html)).toString();
        }
    }
}
