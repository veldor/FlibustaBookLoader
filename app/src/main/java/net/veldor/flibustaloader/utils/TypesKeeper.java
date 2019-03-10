package net.veldor.flibustaloader.utils;

import static net.veldor.flibustaloader.MyWebViewClient.DJVU_TYPE;
import static net.veldor.flibustaloader.MyWebViewClient.EPUB_TYPE;
import static net.veldor.flibustaloader.MyWebViewClient.FB2_TYPE;
import static net.veldor.flibustaloader.MyWebViewClient.MOBI_TYPE;
import static net.veldor.flibustaloader.MyWebViewClient.PDF_TYPE;

public class TypesKeeper {

    private static TypesKeeper instance;


    private final String DJVU_MIME;


    private TypesKeeper(){
        DJVU_MIME = "image/vnd.djvu";
    }

    public static TypesKeeper getInstance(){
        if(instance == null){
            instance = new TypesKeeper();
        }
        return instance;
    }

    String getMime(String type) {
        // content mimes
        String PDF_MIME = "application/pdf";
        String FB2_MIME = "application/fb2+zip";
        String EPUP_MIME = "application/epub+zip";
        String MOBI_MIME = "application/x-mobipocket-ebook";
        switch (type) {
            case PDF_TYPE:
                return PDF_MIME;
            case FB2_TYPE:
                return FB2_MIME;
            case EPUB_TYPE:
                return EPUP_MIME;
            case MOBI_TYPE:
                return MOBI_MIME;
            case DJVU_TYPE:
                return DJVU_MIME;
        }
        return null;
    }
}
