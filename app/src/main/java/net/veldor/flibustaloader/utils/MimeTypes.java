package net.veldor.flibustaloader.utils;

import java.util.HashMap;

public class MimeTypes {

    public static final String MIME_TYPE = "mime_type";
    public static String[] MIMES_LIST = new String[]{"fb2", "mobi", "epub", "pdf", "djvu", "html", "txt", "rtf"};

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
    private static HashMap<String, String> DOWNLOAD_MIMES = new HashMap<String, String>() {{
        put("application/fb2+zip", "fb2.zip");
        put("application/x-mobipocket-ebook", "mobi");
        put("application/epub+zip", "epub");
        put("application/pdf", "pdf");
        put("application/djvu", "djvu");
        put("application/html+zip", "html.zip");
        put("application/txt+zip", "txt.zip");
        put("application/rtf+zip", "rtf.zip");
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
    public static String getDownloadMime(String mime) {
        if (DOWNLOAD_MIMES.containsKey(mime)){
            return DOWNLOAD_MIMES.get(mime);
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
