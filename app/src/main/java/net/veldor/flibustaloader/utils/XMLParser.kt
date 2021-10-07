package net.veldor.flibustaloader.utils

import android.util.Log
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.selections.WebViewParseResult
import org.jsoup.Jsoup
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object XMLParser {
    private const val READ_TYPE = "read"

    @kotlin.jvm.JvmStatic
    fun searchDownloadLinks(content: InputStream?): WebViewParseResult? {
        try {
            val dom: org.jsoup.nodes.Document
            val url = "http://rutracker.org"
            dom = Jsoup.parse(content, "UTF-8", url)
            // попробую найти форму входа. Если она найдена- значит, вход не выполнен. В этом случае удалю идентификационную куку
            if (PreferencesHandler.instance.authCookie != null) {
                val loginForm = dom.select("form#user-login-form")
                if (loginForm != null && loginForm.size == 1) {
                    Log.d(
                        "surprise",
                        "XMLParser.java 378 searchDownloadLinks: founded login FORM!!!!!!!!!!!"
                    )
                    PreferencesHandler.instance.authCookie = null
                    App.sResetLoginCookie.postValue(true)
                }
            }
            val links = dom.select("a")
            if (links != null) {
                val linkPattern = Pattern.compile("^/b/[0-9]+/([a-z0-9]+)$")
                var href: String
                var result: Matcher
                var type: String?
                val types = ArrayList<CharSequence>()
                val linksList = HashMap<String, String>()
                for (link in links) {
                    // проверю ссылку на соответствие формату скачивания
                    href = link.attr("href")
                    result = linkPattern.matcher(href)
                    if (result.matches()) {
                        type = result.group(1)
                        if (type != null && type.isNotEmpty() && type != READ_TYPE) {
                            // добавлю тип в список типов
                            if (!types.contains(type)) {
                                types.add(type)
                            }
                            linksList[result.group()] = type
                        }
                    }
                }
                val res = WebViewParseResult()
                res.linksList = linksList
                res.types = types
                return res
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}