package net.veldor.flibustaloader.utils;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ui.OPDSActivity;

public class URLHelper {

    public static String getBaseUrl() {
        if(App.getInstance().isExternalVpn()){
            return "http://flibustahezeous3.onion";
        }
        return "http://flibustahezeous3.onion";
    }
    public static String getBaseOPDSUrl() {
        if(App.getInstance().isExternalVpn()){
            return "http://flibusta.is";
        }
        return "http://flibustahezeous3.onion";
    }

    public static String getSearchRequest(String searchType, String request) {
        // базовый URL зависит от исползуемого соединения
        StringBuilder urlConstructor = new StringBuilder();
        if(App.getInstance().isExternalVpn()){
            urlConstructor.append("http://flibusta.is/opds/");
        }
        else{
            urlConstructor.append("http://flibustahezeous3.onion/opds/");
        }
        switch (searchType){
            case OPDSActivity.SEARCH_TYPE_BOOKS:
                urlConstructor.append("search?searchType=books&searchTerm=").append(request);
                break;
            case OPDSActivity.SEARCH_TYPE_AUTHORS:
                urlConstructor.append("search?searchType=authors&searchTerm=").append(request);
                break;
            case OPDSActivity.SEARCH_TYPE_SEQUENCES:
            case OPDSActivity.SEARCH_TYPE_GENRE:
                urlConstructor.append(request);
                break;
        }
        return urlConstructor.toString();
    }

    public static boolean isAuthorRequest(String request) {
        return request.contains("search?searchType=authors");
    }

    public static boolean isBookRequest(String request) {
        return request.contains("search?searchType=books") || request.contains("http://flibustahezeous3.onion/opds/new/") || request.contains("/alphabet");
    }
}
