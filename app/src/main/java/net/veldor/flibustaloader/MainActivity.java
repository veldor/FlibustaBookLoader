package net.veldor.flibustaloader;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Intent targetActivityIntent;

        // проверю, не запущено ли приложение с помощью интента. Если да- запущу программу в webView режиме
        if (getIntent().getData() != null) {//check if intent is not null
            Uri data = getIntent().getData();//set a variable for the WebViewActivity
            targetActivityIntent = new Intent(this, ODPSActivity.class);
            targetActivityIntent.setData(data);
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
        finish();
    }
}