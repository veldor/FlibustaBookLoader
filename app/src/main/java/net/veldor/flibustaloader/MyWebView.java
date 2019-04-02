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

    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setup(){
        if (!this.isInEditMode()) {
            this.setWebViewClient(new MyWebViewClient(this));
            WebSettings webSettings = this.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
        }
    }

    @Override
    public void loadUrl(String url) {
        super.loadUrl(url);
        initProgressBar();
    }

    private void initProgressBar() {
        Log.d("surprise", "MyWebView initProgressBar: i init progress bar");
        if (init) {
            return;
        }
        init = true;
        final ProgressBar progressBar = ((MainActivity) getContext()).findViewById(R.id.pageLoadedProgressBar);
        // попробую скрыть бар для начала
        progressBar.setVisibility(View.GONE);
        this.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                Log.d("surprise", "MyWebView onProgressChanged: progress changed on " + progress);
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
