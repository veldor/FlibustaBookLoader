package net.veldor.flibustaloader;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.work.WorkManager;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_WRITE_READ = 22;

    public static final int START_TOR = 3;
    //private LiveData<AndroidOnionProxyManager> mTorClient;
    private Uri mLink;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // если пользователь заходит в приложение впервые- предложу предоставить разрешение на доступ к файлам и выбрать вид
        if (!permissionGranted()) {
            // показываю диалог с требованием предоставить разрешения
            showPermissionDialog();
        } else {
            handleStart();
        }

    }

    private void handleStart() {

        // проверю, выбран ли внешний вид приложения

        if (App.getInstance().getView() != 0) {
            // если приложению передана ссылка на страницу
            if (getIntent().getData() != null) {//check if intent is not null
                Log.d("surprise", "handleStart: have intent");
                mLink = getIntent().getData();//set a variable for the WebViewActivity
            }

            startActivityForResult(new Intent(this, StartTorActivity.class), START_TOR);
        } else {
            selectView();
        }

    }

    private void selectView() {
        // покажу диалог выбора вида приложения
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Выберите внешний вид")
                .setMessage("Выберите вид приложения. В будущем вы можете переключить вид в меню приложения (Меню => внешний вид). В режиме WebView информация берётся непосредственно с сайта Флибусты и выглядит как страница сайта. В режиме OPDS информация получается из электронного каталога Флибусты. Рекомендую попробовать оба режима, у каждого из них свои плюсы. Приятного поиска.")
                .setCancelable(false)
                .setPositiveButton("Режим WebView", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        App.getInstance().setView(App.VIEW_WEB);
                        handleStart();
                    }
                })
                .setNegativeButton("Режим OPDS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        App.getInstance().setView(App.VIEW_ODPS);
                        handleStart();
                    }
                });
        dialogBuilder.create().show();
    }

    private void showPermissionDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Необходимо предоставить разрешения")
                .setMessage("Для загрузки книг необходимо предоставить доступ к памяти устройства")
                .setCancelable(false)
                .setPositiveButton("Предоставить разрешение", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_WRITE_READ);
                    }
                })
                .setNegativeButton("Нет, закрыть приложение", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        dialogBuilder.create().show();
    }

    private void startApp() {
        // проверю очередь скачивания. Если она не пуста- предложу продолжить закачку
        // проверю, не запущено ли приложение с помощью интента. Если да- запущу программу в webView режиме
        Intent targetActivityIntent;
        if (mLink != null) {//check if intent is not null
            targetActivityIntent = new Intent(this, WebViewActivity.class);
            targetActivityIntent.setData(mLink);
        } else {
            // проверю, если используем ODPS- перенаправлю в другую активность
            if (App.getInstance().getView() == App.VIEW_ODPS) {
                targetActivityIntent = new Intent(this, OPDSActivity.class);
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
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("surprise", "MainActivity onDestroy app destroyed?");
        // остановлю запуск TOR
        WorkManager.getInstance(this).cancelAllWorkByTag(App.START_TOR);
    }


    private boolean permissionGranted() {
        int writeResult;
        int readResult;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            writeResult = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            readResult = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            writeResult = PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            readResult = PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        return writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_READ && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog();
            } else {
                handleStart();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}