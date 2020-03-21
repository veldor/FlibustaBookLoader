package net.veldor.flibustaloader.utils;

import net.veldor.flibustaloader.App;

public class URLHandler {

    public static String getBaseUrl() {
        if(App.getInstance().isExternalVpn()){
            return "http://flibustahezeous3.onion";
        }
        return "http://flibustahezeous3.onion";
    }

    public static String getSearchRequestBase(){
        if(App.getInstance().isExternalVpn()){
            return "http://flibusta.is/booksearch?ask=";
        }
        return "http://flibustahezeous3.onion/booksearch?ask=";
    }
}
