package net.veldor.flibustaloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

public class MyWebView extends WebView {

    private boolean init = false;

    @SuppressLint("SetJavaScriptEnabled")
    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        WebSettings webSettings = this.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
    }

    @Override
    public void loadUrl(String url) {
        super.loadUrl(url);
        initProgressBar();
    }

    private void initProgressBar() {
        if (init) {
            return;
        }
        init = true;
        final ProgressBar progressBar = ((MainActivity) getContext()).findViewById(R.id.pageLoadedProgressBar);
        this.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (progress > 90) {
                    if (progressBar.getVisibility() == View.VISIBLE) {
                        progressBar.setVisibility(View.GONE);
                    }
                } else {
                    if (progressBar.getVisibility() == View.GONE) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                    progressBar.setProgress(progress);
                }
            }
        });
    }
}
