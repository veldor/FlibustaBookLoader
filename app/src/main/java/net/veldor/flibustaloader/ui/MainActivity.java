package net.veldor.flibustaloader.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_WRITE_READ = 22;

    public static final int START_TOR = 3;
    private static final int TOR_LOAD_MAX_TIME = 180;
    //private LiveData<AndroidOnionProxyManager> mTorClient;
    private Uri mLink;
    private ProgressBar mTorLoadingProgressIndicator;
    private TextView mTorLoadingStatusText;
    private int mProgressCounter;
    private CountDownTimer mCdt;
    private AndroidOnionProxyManager mTor;
    // отмечу готовность к старту приложения
    private boolean mReadyToStart = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
        // если пользователь заходит в приложение впервые- предложу предоставить разрешение на доступ к файлам и выбрать вид
        if (!permissionGranted()) {
            // показываю диалог с требованием предоставить разрешения
            showPermissionDialog();
        } else {
            handleStart();
        }

    }

    private void setupUI() {
        setContentView(R.layout.activity_main);
        // найду индикатор прогресса
        mTorLoadingProgressIndicator = findViewById(R.id.progressBarCircle);
        mTorLoadingProgressIndicator.setProgress(0);
        mTorLoadingProgressIndicator.setMax(TOR_LOAD_MAX_TIME);
        // найду строку статуса загрузки
        mTorLoadingStatusText = findViewById(R.id.progressTorLoadStatus);
        mTorLoadingStatusText.setText(getString(R.string.begin_tor_init_msg));

        // отображу версию приложения
        TextView versionView = findViewById(R.id.app_version);
        String version;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            versionView.setText(String.format(Locale.ENGLISH, getString(R.string.application_version_message), version));
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("surprise", "MainActivity setupUI: can't found version");
            e.printStackTrace();
        }
    }

    private void setupObservers() {
        // зарегистрирую отслеживание загружающегося TOR
        LiveData<AndroidOnionProxyManager> loadedTor = App.getInstance().mLoadedTor;
        loadedTor.observe(this, new Observer<AndroidOnionProxyManager>() {
            @Override
            public void onChanged(@Nullable AndroidOnionProxyManager tor) {
                if (tor != null) {
                    Log.d("surprise", "MainActivity onChanged: i have TOR");
                    mTor = tor;
                }
            }
        });
        // получу данные о работе
        LiveData<List<WorkInfo>> workInfoData = WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(App.START_TOR);
        workInfoData.observe(this, new Observer<List<WorkInfo>>() {
            @Override
            public void onChanged(List<WorkInfo> workInfos) {
                if (workInfos != null && workInfos.size() > 0) {
                    // переберу статусы
                    WorkInfo data = workInfos.get(0);
                    switch (data.getState()) {
                        case ENQUEUED:
                            if (mTorLoadingStatusText != null) {
                                mTorLoadingStatusText.setText(getString(R.string.tor_load_waiting_internet_message));
                            }
                            stopTimer();
                            break;
                        case RUNNING:
                            // запускаю таймер
                            startTimer();
                            if (mTorLoadingStatusText != null) {
                                mTorLoadingStatusText.setText(getString(R.string.launch_begin_message));
                            }
                            break;
                        case CANCELLED:
                            stopTimer();
                            if (mTorLoadingStatusText != null) {
                                mTorLoadingStatusText.setText(getString(R.string.launch_cancelled_message));
                            }
                            break;
                        case FAILED:
                            showTorNotWorkDialog();
                            break;
                        case SUCCEEDED:
                            torLoaded();
                    }
                }
            }
        });
    }

    private void torLoaded() {
        if (mCdt != null) {
            mCdt.cancel();
        }
        // сбрасываю таймер. Если выбран вид приложения- запущу Activity согласно виду. Иначе- отмечу, что TOR загружен и буду ждать выбора вида
        if (App.getInstance().getView() != 0) {
            startApp();
        }
        else{
            mReadyToStart = true;
        }
    }

    private void handleStart() {
        setupObservers();
        // проверю, выбран ли внешний вид приложения
        if (App.getInstance().getView() != 0) {
            // если приложению передана ссылка на страницу
            if (getIntent().getData() != null) {//check if intent is not null
                Log.d("surprise", "handleStart: have intent");
                mLink = getIntent().getData();//set a variable for the WebViewActivity
            }
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
                        if(mReadyToStart){
                            startApp();
                        }
                    }
                })
                .setNegativeButton("Режим OPDS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        App.getInstance().setView(App.VIEW_ODPS);
                        if(mReadyToStart){
                            startApp();
                        }
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
        finish();
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

    // запущу таймер отсчёта

    private void startTimer() {
        int waitingTime = TOR_LOAD_MAX_TIME * 1000; // 3 minute in milli seconds
        mProgressCounter = 0;
        mCdt = new CountDownTimer(waitingTime, 1000) {
            public void onTick(long millisUntilFinished) {
                mProgressCounter++;
                mTorLoadingProgressIndicator.setProgress(mProgressCounter);
                if (mTor != null) {
                    String last = mTor.getLastLog();
                    if (last != null) {
                        if (!last.isEmpty()) {
                            mTorLoadingStatusText.setText(last);
                        } else {
                            mTorLoadingStatusText.setText(String.format(Locale.ENGLISH, getString(R.string.tor_continue_loading), mProgressCounter));
                        }
                    } else {
                        mTorLoadingStatusText.setText(String.format(Locale.ENGLISH, getString(R.string.tor_continue_loading), mProgressCounter));

                    }
                } else {
                    mTorLoadingStatusText.setText(R.string.wait_tor_loading_message);
                }
            }

            public void onFinish() {
                // tor не загрузился, покажу сообщение с предложением подождать или перезапустить процесс
                torLoadTooLongDialog();
            }
        };
        mCdt.start();
    }


    private void stopTimer() {
        mProgressCounter = 0;
        mTorLoadingProgressIndicator.setProgress(0);
        if (mCdt != null) {
            mCdt.cancel();
        }
    }


    private void torLoadTooLongDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Tor load too long")
                .setMessage("Подождём ещё или перезапустим?")
                .setPositiveButton("Перезапуск", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        App.sTorStartTry = 0;
                        App.getInstance().startTor();
                    }
                })
                .setNegativeButton("Подождать ещё", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startTimer();
                    }
                }).show();
    }


    private void showTorNotWorkDialog() {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(getString(R.string.tor_cant_load_message))
                    .setMessage(getString(R.string.tor_not_start_body))
                    .setPositiveButton(getString(R.string.try_again_message), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            App.getInstance().startTor();

                        }
                    })
                    .setNegativeButton(getString(R.string.try_later_message), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finishAffinity();
                        }
                    })
                    .show();
        }
}