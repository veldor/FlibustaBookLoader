package net.veldor.flibustaloader.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;

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
        startCheckWorker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.getInstance().startCheckWorker();
                Toast.makeText(FlibustaNotAvailableActivity.this, "Вы получите уведомление, когда сервер Флибусты вернётся",Toast.LENGTH_LONG).show();
                finishActivity(0);
            }
        });
    }
}