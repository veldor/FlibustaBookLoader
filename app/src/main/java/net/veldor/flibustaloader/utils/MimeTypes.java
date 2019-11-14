package net.veldor.flibustaloader.utils;

import java.util.HashMap;

public class MimeTypes {

    private static HashMap<String, String> MIMES = new HashMap<String, String>() {{
        put("application/fb2+zip", "fb2");
        put("application/x-mobipocket-ebook", "mobi");
        put("application/epub+zip", "epub");
        put("application/pdf", "pdf");
        put("application/djvu", "djvu");
        put("application/html+zip", "html");
        put("application/txt+zip", "txt");
        put("application/rtf+zip", "rtf");
    }};

    private static HashMap<String, String> FULL_MIMES = new HashMap<String, String>() {{
        put("fb2", "application/fb2+zip");
        put("mobi", "application/x-mobipocket-ebook");
        put("epub", "application/epub+zip");
        put("pdf", "application/pdf");
        put("djvu", "application/djvu");
        put("html", "application/html+zip");
        put("txt", "application/txt+zip");
        put("rtf", "application/rtf+zip");
    }};


    public static String getMime(String mime) {
        if (MIMES.containsKey(mime)){
            return MIMES.get(mime);
        }
        return mime;
    }

    public static String getFullMime(String shortMime) {

        if (FULL_MIMES.containsKey(shortMime)){
            return FULL_MIMES.get(shortMime);
        }
        return shortMime;
    }
}
