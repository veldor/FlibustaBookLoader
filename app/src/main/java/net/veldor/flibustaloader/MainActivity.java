package net.veldor.flibustaloader;

import android.arch.lifecycle.LiveData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import androidx.work.WorkManager;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

public class MainActivity extends AppCompatActivity {

    public static final int START_TOR = 3;
    private LiveData<AndroidOnionProxyManager> mTorClient;
    private Uri mLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // если приложению передана ссылка на страницу
        if (getIntent().getData() != null) {//check if intent is not null
            mLink = getIntent().getData();//set a variable for the WebViewActivity
        }

        if (mTorClient == null) {
            startActivityForResult(new Intent(this, StartTorActivity.class), START_TOR);
        }
        else{
            startApp();
        }

        // если не работает TOR- запущу Activity с его загрузкой

        /*       */
    }

    private void startApp() {
        // проверю, не запущено ли приложение с помощью интента. Если да- запущу программу в webView режиме
        Intent targetActivityIntent;
        if (mLink != null) {//check if intent is not null
            targetActivityIntent = new Intent(this, WebViewActivity.class);
            targetActivityIntent.setData(mLink);
        } else {
            // проверю, если используем ODPS- перенаправлю в другую активность
            if (App.getInstance().isODPS()) {
                targetActivityIntent = new Intent(this, ODPSActivity.class);
            } else {
                targetActivityIntent = new Intent(this, WebViewActivity.class);
            }
        }
        targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(targetActivityIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            startApp();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("surprise", "MainActivity onDestroy app destroyed?");
        WorkManager.getInstance().cancelAllWork();
    }
}