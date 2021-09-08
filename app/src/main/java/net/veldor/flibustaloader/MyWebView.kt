package net.veldor.flibustaloader

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import net.veldor.flibustaloader.ui.WebViewActivity

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
        super.loadUrl(url)
        initProgressBar()
    }

    private fun initProgressBar() {
        if (init) {
            return
        }
        init = true
        val progressBar =
            (context as WebViewActivity).findViewById<ProgressBar>(R.id.pageLoadedProgressBar)
        // попробую скрыть бар для начала
        progressBar.visibility = GONE
        this.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                if (progress > 90) {
                    if (progressBar.visibility == VISIBLE) {
                        progressBar.visibility = GONE
                    }
                } else {
                    if (progressBar.visibility == GONE) {
                        progressBar.visibility = VISIBLE
                    }
                    progressBar.progress = progress
                }
            }
        }
    }
}