package net.veldor.flibustaloader.utils

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.text.Html
import android.util.Log
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.selections.Author
import net.veldor.flibustaloader.selections.FoundedEntity
import java.util.*
import android.text.Spannable

import android.text.style.ForegroundColorSpan

import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.widget.TextView


object Grammar {
    @kotlin.jvm.JvmStatic
    fun textFromHtml(html: String?): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            Html.fromHtml(html).toString()
        }
    }

    @kotlin.jvm.JvmStatic
    val random: Int
        get() {
            val min = 1000
            val max = 9999
            val r = Random()
            return r.nextInt(max - min + 1) + min
        }

    @kotlin.jvm.JvmStatic
    val appVersion: String
        get() {
            try {
                val pInfo = App.instance.packageManager.getPackageInfo(
                    App.instance.packageName,
                    0
                )
                return pInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                Log.d("surprise", "MainActivity setupUI: can't found version")
                e.printStackTrace()
            }
            return "0"
        }

    @kotlin.jvm.JvmStatic
    fun createAuthorDirName(author: Author): String {
        val dirname: String = author.name!!
        return if (dirname.length > 100) {
            dirname.substring(0, 100)
        } else dirname
    }

    @kotlin.jvm.JvmStatic
    fun createAuthorDirName(author: FoundedEntity): String {
        val dirname: String = author.name!!
        return if (dirname.length > 100) {
            dirname.substring(0, 100).trim()
        } else dirname.trim()
    }

    @kotlin.jvm.JvmStatic
    fun clearDirName(dirName: String): String {
        return Regex("[^\\d\\w ]").replace(dirName, "")
    }

    @kotlin.jvm.JvmStatic
    fun getExtension(filename: String): String {
        return filename.substring(filename.lastIndexOf(".") + 1)
    }

    fun changeExtension(name: String, trueFormatExtension: String?): String {
        return name.substring(0, name.lastIndexOf(".")) + "." + trueFormatExtension
    }

    @kotlin.jvm.JvmStatic
    fun getLiteralSize(length: Long): String {
        if (length > 1024 * 1024) {
            return (length / 1024 / 1024).toString() + " мб."
        }
        return if (length > 1024) {
            (length / 1024).toString() + " кб."
        } else "$length байт"
    }

    fun removeExtension(name: String): String {
        return name.substring(0, name.lastIndexOf("."))
    }

    fun isValidUrl(newValue: String): Boolean {
        return "^https?://\\w+.\\w+\$".toRegex().matches(newValue)
    }

    fun getAvailableDownloadFormats(item: FoundedEntity, view: TextView) {
        view.text = ""
        if (item.downloadLinks.isEmpty()) {
            view.text = getColoredString("Не найдены ссылки для загрузки", Color.RED)
        }
        item.downloadLinks.forEach {
            if (it.mime != null) {
                if (it.mime!!.contains("fb2")) {
                    view.append(getColoredString(" FB2 ", Color.parseColor("#FFE91E63")))
                    view.append(" ")
                } else if (it.mime!!.contains("mobi")) {
                    view.append(getColoredString(" MOBI ", Color.parseColor("#FF9C27B0")))
                    view.append(" ")
                } else if (it.mime!!.contains("epub")) {
                    view.append(getColoredString(" EPUB ", Color.parseColor("#FF673AB7")))
                    view.append(" ")
                } else if (it.mime!!.contains("pdf")) {
                    view.append(getColoredString(" PDF ", Color.parseColor("#FF3F51B5")))
                    view.append(" ")
                } else if (it.mime!!.contains("txt")) {
                    view.append(getColoredString(" TXT ", Color.parseColor("#FF2196F3")))
                    view.append(" ")
                } else if (it.mime!!.contains("html")) {
                    view.append(getColoredString(" HTML ", Color.parseColor("#FF009688")))
                    view.append(" ")
                } else if (it.mime!!.contains("doc")) {
                    view.append(getColoredString(" DOC ", Color.parseColor("#FF4CAF50")))
                    view.append(" ")
                } else if (it.mime!!.contains("djvu")) {
                    view.append(getColoredString(" DJVU ", Color.parseColor("#FFFF9800")))
                    view.append(" ")
                } else if (it.mime!!.contains("rtf")) {
                    view.append(getColoredString(" RTF ", Color.parseColor("#030303")))
                    view.append(" ")
                } else {
                    view.append(getColoredString(it.mime!!, Color.parseColor("#030303")))
                    view.append(" ")
                }
            }
        }
    }

    fun getColoredString(mString: String?, colorId: Int): Spannable {
        val spannable: Spannable = SpannableString(mString)
        spannable.setSpan(
            BackgroundColorSpan(colorId),
            0,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }
}

