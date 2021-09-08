package net.veldor.flibustaloader.utils

import android.content.pm.PackageManager
import android.os.Build
import android.text.Html
import android.util.Log
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.selections.Author
import java.util.*

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
    fun clearDirName(dirName: String): String {
        return dirName.replace("[^ а-яА-Яa-zA-Z0-9.\\-]".toRegex(), "")
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
}