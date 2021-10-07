package net.veldor.flibustaloader

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebChromeClient
import android.webkit.WebView
import net.veldor.flibustaloader.ui.BrowserActivity
import net.veldor.flibustaloader.ui.fragments.WebViewFragment
import net.veldor.flibustaloader.utils.URLHelper

class MyWebView(context: Context?, attrs: AttributeSet?) : WebView(context, attrs) {
    private var init = false

    @SuppressLint("SetJavaScriptEnabled")
    fun setup() {
        if (!this.isInEditMode) {
            this.webViewClient = MyWebViewClient()
            val webSettings = this.settings
            webSettings.javaScriptEnabled = true
            webSettings.allowFileAccess = true
            webSettings.builtInZoomControls = true
            webSettings.displayZoomControls = false
        }
    }


    override fun loadUrl(url: String) {
        super.loadUrl(URLHelper.getBaseUrl() + url)
        initProgressBar()
    }

    private fun initProgressBar() {
        if (init) {
            return
        }
        init = true
        val fragment = (context as BrowserActivity).getCurrentFragment()
        if (fragment is WebViewFragment) {
            fragment.binding.pageLoadedProgressBar.visibility = GONE
            this.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, progress: Int) {
                    if (progress > 90) {
                        if (fragment.binding.pageLoadedProgressBar.visibility == VISIBLE) {
                            fragment.binding.pageLoadedProgressBar.visibility = GONE
                        }
                    } else {
                        if (fragment.binding.pageLoadedProgressBar.visibility == GONE) {
                            fragment.binding.pageLoadedProgressBar.visibility = VISIBLE
                        }
                        fragment.binding.pageLoadedProgressBar.progress = progress
                    }
                }
            }
        }
    }
}