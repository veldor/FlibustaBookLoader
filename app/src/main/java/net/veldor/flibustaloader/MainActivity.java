package net.veldor.flibustaloader;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // проверю, если используем ODPS- перенаправлю в другую активность
        Intent targetActivityIntent;
        if (App.getInstance().isODPS()) {
            targetActivityIntent = new Intent(this, ODPSActivity.class);
        }
        else{
            targetActivityIntent = new Intent(this, WebViewActivity.class);
        }
        targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(targetActivityIntent);
        finish();
    }
}