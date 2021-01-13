package net.veldor.flibustaloader.utils;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ui.OPDSActivity;

@SuppressWarnings("SameReturnValue")
public class URLHelper {

    public static String getBaseUrl() {
        return "http://flibustahezeous3.onion";
    }

    public static String getBaseOPDSUrl() {
        if(MyPreferences.getInstance().isCustomMirror()){
            return MyPreferences.getInstance().getCustomMirror();
        }
        if (App.getInstance().isExternalVpn()) {
            return "http://flibusta.is";
        }
        return "http://flibustahezeous3.onion";
    }

    public static String getFlibustaIsUrl() {
        return "http://flibusta.is";
    }

    public static String getSearchRequest(String searchType, String request) {
        // базовый URL зависит от исползуемого соединения
        StringBuilder urlConstructor = new StringBuilder();
        if(MyPreferences.getInstance().isCustomMirror()){
            urlConstructor.append(MyPreferences.getInstance().getCustomMirror()).append("/opds/");
        }
        else if (App.getInstance().isExternalVpn()) {
            urlConstructor.append("http://flibusta.is/opds/");
        } else {
            urlConstructor.append("http://flibustahezeous3.onion/opds/");
        }
        switch (searchType) {
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

}
