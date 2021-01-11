package net.veldor.flibustaloader.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.utils.MyPreferences;

public class FlibustaNotAvailableActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flibusta_not_available);
        Button closeAppBtn = findViewById(R.id.closeAppButton);
        closeAppBtn.setOnClickListener(v -> System.exit(0));
        Button tryAgainBtn = findViewById(R.id.retryButton);
        tryAgainBtn.setOnClickListener(v -> {
            startActivity(new Intent(FlibustaNotAvailableActivity.this, MainActivity.class));
            finish();
        });
        Button startCheckWorker = findViewById(R.id.startCheckerBtn);
        startCheckWorker.setOnClickListener(v -> {
            App.getInstance().startCheckWorker();
            Toast.makeText(FlibustaNotAvailableActivity.this, "Вы получите уведомление, когда сервер Флибусты вернётся", Toast.LENGTH_LONG).show();
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        });
        Button disableInspectionButton = findViewById(R.id.disableInspectionButton);
        disableInspectionButton.setOnClickListener(v -> {
            MyPreferences.getInstance().setInspectionEnabled(false);
            Toast.makeText(FlibustaNotAvailableActivity.this, getString(R.string.inspection_disabled_message), Toast.LENGTH_LONG).show();
            startActivity(new Intent(FlibustaNotAvailableActivity.this, MainActivity.class));
            finish();
        });
        Button showMainWindowButton = findViewById(R.id.showMainWindowButton);
        showMainWindowButton.setOnClickListener(v -> {
            // запущу главное окно
            startView();
            finish();
        });
    }

    private void startView() {
        // проверю очередь скачивания. Если она не пуста- предложу продолжить закачку
        // проверю, не запущено ли приложение с помощью интента. Если да- запущу программу в webView режиме
        Intent targetActivityIntent;
        // проверю, если используем ODPS- перенаправлю в другую активность
        if (App.getInstance().getView() == App.VIEW_ODPS) {
            targetActivityIntent = new Intent(this, OPDSActivity.class);
        } else {
            targetActivityIntent = new Intent(this, WebViewActivity.class);
        }
        targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(targetActivityIntent);
        finish();
    }
}