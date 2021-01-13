package net.veldor.flibustaloader.utils;

import java.util.HashMap;

public class MimeTypes {

    public static final String[] MIMES_LIST = new String[]{"fb2", "mobi", "epub", "pdf", "djvu", "html", "txt", "rtf"};

    private static final HashMap<String, String> MIMES = new HashMap<String, String>() {{
        put("application/fb2+zip", "fb2");
        put("application/fb2", "fb2");
        put("application/x-mobipocket-ebook", "mobi");
        put("application/epub+zip", "epub");
        put("application/epub", "epub");
        put("application/pdf", "pdf");
        put("application/djvu", "djvu");
        put("application/html+zip", "html");
        put("application/txt+zip", "txt");
        put("application/txt", "txt");
        put("application/rtf+zip", "rtf");
    }};
    private static final HashMap<String, String> DOWNLOAD_MIMES = new HashMap<String, String>() {{
        put("application/fb2+zip", "fb2.zip");
        put("application/x-mobipocket-ebook", "mobi");
        put("application/epub+zip", "epub");
        put("view_models", "djvu");
        put("application/epub", "epub");
        put("application/octet-stream", "epub");
        put("application/pdf", "pdf");
        put("application/djvu", "djvu");
        put("application/html+zip", "html.zip");
        put("application/txt+zip", "txt.zip");
        put("application/rtf+zip", "rtf.zip");
        put("application/zip", "zip");
    }};

    private static final HashMap<String, String> FULL_MIMES = new HashMap<String, String>() {{
        put("fb2", "application/fb2+zip");
        put("mobi", "application/x-mobipocket-ebook");
        put("epub", "application/epub+zip");
        put("pdf", "application/pdf");
        put("djvu", "application/djvu");
        put("html", "application/html+zip");
        put("txt", "application/txt+zip");
        put("rtf", "application/rtf+zip");
        put("zip", "application/zip");
    }};


    public static String getMime(String mime) {
        if (MIMES.containsKey(mime)) {
            return MIMES.get(mime);
        }
        return mime;
    }

    public static String getDownloadMime(String mime) {
        if (mime.equals("application/epub")) {
            return "epub";
        }
        if (mime.equals("application/djvu+zip")) {
            return "djvu.zip";
        }
        if (mime.equals("application/doc")) {
            return "doc";
        }
        if (mime.equals("application/docx")) {
            return "docx";
        }
        if (mime.equals("application/jpg")) {
            return "jpg";
        }
        if (mime.equals("application/pdf+zip")) {
            return "pdf.zip";
        }
        if (mime.equals("application/rtf")) {
            return "rtf";
        }
        if (mime.equals("application/txt")) {
            return "txt";
        }
        if (DOWNLOAD_MIMES.containsKey(mime)) {
            return DOWNLOAD_MIMES.get(mime);
        }
        return mime;
    }

    public static String getFullMime(String shortMime) {
        if (shortMime.endsWith(".zip")) {
            return "application/zip";
        }
        if (FULL_MIMES.containsKey(shortMime)) {
            return FULL_MIMES.get(shortMime);
        }
        return shortMime;
    }

    public static String getTrueFormatExtension(String trueFormat) {
        if(trueFormat.equals("text/plain") || trueFormat.equals("application/txt")){
            return "txt";
        }
        if(trueFormat.equals("application/zip")){
            return "zip";
        }
        return null;
    }

    public static boolean isBookFormat(String mime) {
        return DOWNLOAD_MIMES.containsKey(mime);
    }
}
