package net.veldor.flibustaloader.utils

import android.util.Log
import java.util.HashMap

object MimeTypes {
    @kotlin.jvm.JvmField
    val MIMES_LIST = arrayOf("fb2", "mobi", "epub", "pdf", "djvu", "html", "txt", "rtf")

    private val MIMES: HashMap<String, String> = hashMapOf(
        "application/fb2+zip" to "fb2",
        "application/fb2" to "fb2",
        "application/x-mobipocket-ebook" to "mobi",
        "application/epub+zip" to "epub",
        "application/epub" to "epub",
        "application/pdf" to "pdf",
        "application/djvu" to "djvu",
        "application/html+zip" to "html",
        "application/octet-stream" to "html",
        "application/txt+zip" to "txt",
        "application/txt" to "txt",
        "application/rtf+zip" to "rtf",
        " application/vnd.ms-htmlhelp" to "chm",
    )

    private val DOWNLOAD_MIMES: HashMap<String, String> = hashMapOf(
        "application/fb2+zip" to "fb2.zip",
        "application/x-mobipocket-ebook" to "mobi",
        "application/epub+zip" to "epub",
        "view_models" to "djvu",
        "application/epub" to "epub",
        "application/pdf" to "pdf",
        "application/djvu" to "djvu",
        "application/msword" to "doc",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "docx",
        "application/html+zip" to "html.zip",
        "application/txt+zip" to "txt.zip",
        "application/rtf+zip" to "rtf",
        "application/zip" to "zip",
        " application/vnd.ms-htmlhelp" to "chm",
    )

    private val FULL_MIMES: HashMap<String, String> = hashMapOf(
        "fb2" to "application/fb2+zip",
        "mobi" to "application/x-mobipocket-ebook",
        "epub" to "application/epub+zip",
        "pdf" to "application/pdf",
        "djvu" to "application/djvu",
        "html" to "application/html+zip",
        "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "txt" to "application/txt+zip",
        "rtf" to "application/rtf+zip",
        "zip" to "application/zip",
        "chm" to " application/vnd.ms-htmlhelp",
    )


    @kotlin.jvm.JvmStatic
    fun getMime(mime: String): String? {
        return if (MIMES.containsKey(mime)) {
            MIMES[mime]
        } else mime
    }

    @kotlin.jvm.JvmStatic
    fun getDownloadMime(mime: String): String? {
        if (mime == "application/epub") {
            return "epub"
        }
        if (mime == "application/djvu+zip") {
            return "djvu.zip"
        }
        if (mime == "application/doc") {
            return "doc"
        }
        if (mime == "application/docx") {
            return "docx"
        }
        if (mime == "application/jpg") {
            return "jpg"
        }
        if (mime == "application/pdf+zip") {
            return "pdf.zip"
        }
        if (mime == "application/rtf+zip") {
            return "rtf"
        }
        if (mime == "application/txt") {
            return "txt"
        }
        if (mime == " application/vnd.ms-htmlhelp") {
            return "chm"
        }
        return if (DOWNLOAD_MIMES.containsKey(mime)) {
            DOWNLOAD_MIMES[mime]
        } else mime
    }

    @kotlin.jvm.JvmStatic
    fun getFullMime(shortMime: String?): String? {
        Log.d("surprise", "MimeTypes: 89 GET MIME FOR $shortMime")
        return if (FULL_MIMES.containsKey(shortMime)) {
            FULL_MIMES[shortMime]
        } else shortMime
    }

    fun getTrueFormatExtension(trueFormat: String): String? {
        if (trueFormat == "text/plain" || trueFormat == "application/txt") {
            return "txt"
        }
        if (trueFormat == "application/zip") {
            return "zip"
        }
        return if (MIMES.containsKey(trueFormat)) {
            MIMES[trueFormat]
        } else null
    }

    @kotlin.jvm.JvmStatic
    fun isBookFormat(mime: String): Boolean {
        return DOWNLOAD_MIMES.containsKey(mime)
    }
}