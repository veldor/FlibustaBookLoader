package net.veldor.flibustaloader.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.utils.Grammar;
import net.veldor.flibustaloader.utils.MyPreferences;

import java.io.File;
import java.util.List;
import java.util.Locale;

import lib.folderpicker.FolderPicker;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_WRITE_READ = 22;

    public static final int START_TOR = 3;
    private static final int TOR_LOAD_MAX_TIME = 180;
    private static final int DOWNLOAD_FOLDER_SELECT_REQUEST_CODE = 23;
    private static final int DOWNLOAD_FOLDER_SELECT_OLD_REQUEST_CODE = 24;
    private Uri mLink;
    private ProgressBar mTorLoadingProgressIndicator;
    private TextView mTorLoadingStatusText;
    private int mProgressCounter;
    private CountDownTimer mCdt;
    private AndroidOnionProxyManager mTor;
    // отмечу готовность к старту приложения
    private boolean mReadyToStart = false;
    private boolean mActivityVisible;
    private boolean mTorLoadTooLong;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
        // если пользователь заходит в приложение впервые- предложу предоставить разрешение на доступ к файлам и выбрать вид
        if (!permissionGranted()) {
            // показываю диалог с требованием предоставить разрешения
            showPermissionDialog();
        } else {
            if (MyPreferences.getInstance().isDownloadDir()) {
                showSelectDownloadFolderDialog();
            } else {
                handleStart();
            }
        }
    }

    private void showSelectDownloadFolderDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Выберите папку для сохранения")
                .setMessage("Выберите папку, в которой будут храниться скачанные книги")
                .setCancelable(false)
                .setPositiveButton("Ок", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        intent.addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                        );
                        startActivityForResult(intent, DOWNLOAD_FOLDER_SELECT_REQUEST_CODE);
                    } else {
                        Intent intent = new Intent(this, FolderPicker.class);
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                            intent.addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            );
                        }
                        startActivityForResult(intent, DOWNLOAD_FOLDER_SELECT_OLD_REQUEST_CODE);

                    }
                })
                .setNegativeButton("Нет, закрыть приложение", (dialog, which) -> finish());
        if(!MainActivity.this.isFinishing()) {
            dialogBuilder.create().show();
        }
    }

    private void setupUI() {
        if(MyPreferences.getInstance().isHardwareAcceleration()){
            // проверю аппаратное ускорение
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }


        // если используем внешнй VPN- проверяю только выдачу разрешений и настройку внешнего вида
        setContentView(R.layout.activity_main);

        View rootView = findViewById(R.id.rootView);
        if(rootView != null){
            try{
                // назначу фон
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    rootView.setBackground(getDrawable(R.drawable.back_3));
                }
                else{
                    rootView.setBackground(getResources().getDrawable(R.drawable.back_3));
                }
            }
            catch (Exception e){
                Log.d("surprise", "MainActivity setupUI 137: can't set drawable");
            }
        }

        // переключатель аппаратного ускорения
        Switch switcher = findViewById(R.id.useHardwareAccelerationSwitcher);
        if(switcher != null){
            switcher.setChecked(MyPreferences.getInstance().isHardwareAcceleration());
            switcher.setOnCheckedChangeListener((buttonView, isChecked) -> MyPreferences.getInstance().switchHardwareAcceleration());
        }
        if (App.getInstance().isExternalVpn()) {
            Log.d("surprise", "MainActivity setupUI external vpn used");
        } else {
            // найду индикатор прогресса
            mTorLoadingProgressIndicator = findViewById(R.id.progressBarCircle);
            mTorLoadingProgressIndicator.setProgress(0);
            mTorLoadingProgressIndicator.setMax(TOR_LOAD_MAX_TIME);
            // найду строку статуса загрузки
            mTorLoadingStatusText = findViewById(R.id.progressTorLoadStatus);
            mTorLoadingStatusText.setText(getString(R.string.begin_tor_init_msg));

            // отображу версию приложения
            TextView versionView = findViewById(R.id.app_version);
            String version = Grammar.getAppVersion();
            versionView.setText(String.format(Locale.ENGLISH, getString(R.string.application_version_message), version));

            Button startBtn = findViewById(R.id.testStartApp);
            startBtn.setOnClickListener(v -> {
                // покажу диалог, предупреждающий о том, что это не запустит приложение
                showForceStartDialog();
            });
        }
    }

    private void showForceStartDialog() {
        if(!MainActivity.this.isFinishing()) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage("Принудительный запуск приложения. Внимание, это не значит, что приложение будет работать! Данная функция сделана исключительно для тех ситуаций, когда приложение не смогло само определить, что клиент TOR успешно запустился (проблема некоторорых китайских аппаратов). Так что используйте только если точно знаете, что делаете");
            dialogBuilder.setPositiveButton("Да, я знаю, что делаю", (dialogInterface, i) -> torLoaded());
            dialogBuilder.setNegativeButton("Нет, подождать ещё", (dialogInterface, i) -> dialogInterface.dismiss());
            dialogBuilder.show();
        }
    }

    private void setupObservers() {
        if (!App.getInstance().isExternalVpn()) {
            // зарегистрирую отслеживание загружающегося TOR
            LiveData<AndroidOnionProxyManager> loadedTor = App.getInstance().mLoadedTor;
            loadedTor.observe(this, tor -> {
                if (tor != null) {
                    Log.d("surprise", "MainActivity onChanged: i have TOR");
                    mTor = tor;
                }
            });
            // получу данные о работе
            LiveData<List<WorkInfo>> workInfoData = WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(App.START_TOR);
            workInfoData.observe(this, workInfos -> {
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
            });
        } else {
            torLoaded();
        }
    }

    private void torLoaded() {
        if (mCdt != null) {
            mCdt.cancel();
        }
        // сбрасываю таймер. Если выбран вид приложения- запущу Activity согласно виду. Иначе- отмечу, что TOR загружен и буду ждать выбора вида
        if (App.getInstance().getView() != 0) {
            startApp();
        } else {
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
        if(!MainActivity.this.isFinishing()) {
            // покажу диалог выбора вида приложения
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Выберите внешний вид")
                    .setMessage("Выберите вид приложения. В будущем вы можете переключить вид в меню приложения (Меню => внешний вид). В режиме WebView информация берётся непосредственно с сайта Флибусты и выглядит как страница сайта. В режиме OPDS информация получается из электронного каталога Флибусты. Рекомендую попробовать оба режима, у каждого из них свои плюсы. Приятного поиска.")
                    .setCancelable(false)
                    .setPositiveButton("Режим WebView", (dialog, which) -> {
                        App.getInstance().setView(App.VIEW_WEB);
                        if (mReadyToStart) {
                            startApp();
                        }
                    })
                    .setNegativeButton("Режим OPDS", (dialog, which) -> {
                        App.getInstance().setView(App.VIEW_ODPS);
                        if (mReadyToStart) {
                            startApp();
                        }
                    });
            dialogBuilder.create().show();
        }
    }

    private void showPermissionDialog() {
        if(!MainActivity.this.isFinishing()) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Необходимо предоставить разрешения")
                    .setMessage("Для загрузки книг необходимо предоставить доступ к памяти устройства")
                    .setCancelable(false)
                    .setPositiveButton("Предоставить разрешение", (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_WRITE_READ))
                    .setNegativeButton("Нет, закрыть приложение", (dialog, which) -> finish());
            dialogBuilder.create().show();
        }
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
                //targetActivityIntent = new Intent(this, OPDSActivity.class);
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
                // проверю, выбрана ли папка
                if (MyPreferences.getInstance().isDownloadDir()) {
                    showSelectDownloadFolderDialog();
                } else {
                    handleStart();
                }
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
        if(!MainActivity.this.isFinishing()) {
            Log.d("surprise", "MainActivity torLoadTooLongDialog: tor load too long err");
            if (mActivityVisible) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle("Tor load too long")
                        .setMessage("Подождём ещё или перезапустим?")
                        .setPositiveButton("Перезапуск", (dialog, which) -> {
                            App.sTorStartTry = 0;
                            App.getInstance().startTor();
                        })
                        .setNegativeButton("Подождать ещё", (dialog, which) -> startTimer()).show();
            } else {
                mTorLoadTooLong = true;
            }
        }
    }


    private void showTorNotWorkDialog() {
        if(!MainActivity.this.isFinishing()) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            AlertDialog show = dialogBuilder.setTitle(getString(R.string.tor_cant_load_message))
                    .setMessage(getString(R.string.tor_not_start_body))
                    .setPositiveButton(getString(R.string.try_again_message), (dialogInterface, i) -> App.getInstance().startTor())
                    .setNegativeButton(getString(R.string.try_later_message), (dialogInterface, i) -> finishAffinity())
                    .setNeutralButton(getString(R.string.use_external_proxy_message), (dialog, which) -> handleUseExternalVpn())
                    .show();
        }
    }


    private void handleUseExternalVpn() {
        if(!MainActivity.this.isFinishing()) {
            // покажу диалог с объяснением последствий включения VPN
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder
                    .setTitle("Использование внешнего VPN")
                    .setMessage("Оповестить об использовании внешнего VPN. В этом случае внутренний клиент TOR будет отключен и траффик приложения не будет обрабатываться. В этом случае вся ответственность за получение контента ложится на внешний VPN. Если вы будете получать сообщения об ошибках загрузки- значит, он работает неправильно. Сделано для версий Android ниже 6.0, где могут быть проблемы с доступом, но может быть использовано по желанию на ваш страх и риск.")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                        App.getInstance().switchExternalVpnUse();
                        torLoaded();
                    });
            dialogBuilder.create().show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mActivityVisible = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActivityVisible = true;
        if (mTorLoadTooLong) {
            mTorLoadTooLong = false;
            torLoadTooLongDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == DOWNLOAD_FOLDER_SELECT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Uri treeUri = data.getData();
                    if (treeUri != null) {
                        // проверю наличие файла
                        DocumentFile dl = DocumentFile.fromTreeUri(App.getInstance(), treeUri);
                        if (dl != null && dl.isDirectory()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                App.getInstance().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                App.getInstance().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            }
                            App.getInstance().setDownloadDir(treeUri);
                            handleStart();
                            return;
                        }
                    }
                }
            }
            showSelectDownloadFolderDialog();
        } else if (requestCode == DOWNLOAD_FOLDER_SELECT_OLD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getExtras() != null) {
                    String folderLocation = data.getExtras().getString("data");
                    File file = new File(folderLocation);
                    if(file.isDirectory() && MyPreferences.getInstance().saveDownloadFolder(folderLocation)){
                        Toast.makeText(this, "Папка сохранена!", Toast.LENGTH_SHORT).show();
                        handleStart();
                    }
                    else{
                        Toast.makeText(this, "Не удалось сохранить папку, попробуйте ещё раз!", Toast.LENGTH_SHORT).show();
                        showSelectDownloadFolderDialog();
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}