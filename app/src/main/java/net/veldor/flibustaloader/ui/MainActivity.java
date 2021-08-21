package net.veldor.flibustaloader.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.core.content.res.ResourcesCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.utils.Grammar;
import net.veldor.flibustaloader.utils.MyPreferences;
import net.veldor.flibustaloader.view_models.StartViewModel;
import net.veldor.flibustaloader.workers.CheckFlibustaAvailabilityWorker;

import java.io.File;
import java.util.List;
import java.util.Locale;

import lib.folderpicker.FolderPicker;

import static androidx.work.WorkInfo.State.CANCELLED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.SUCCEEDED;
import static net.veldor.flibustaloader.utils.BookOpener.intentCanBeHandled;

public class MainActivity extends BaseActivity {


    private static final int REQUEST_WRITE_READ = 22;

    public static final int START_TOR = 3;
    private static final int TOR_LOAD_MAX_TIME = 180;
    private static final int DOWNLOAD_FOLDER_SELECT_REQUEST_CODE = 23;
    private static final int DOWNLOAD_FOLDER_SELECT_OLD_REQUEST_CODE = 24;
    private Uri mLink;
    private ProgressBar mTorLoadingProgressIndicator;
    private TextView mTorLoadingStatusText;
    private int mProgressCounter;
    private int FlibustaCheckCounter;
    private CountDownTimer mCdt;
    private AndroidOnionProxyManager mTor;
    // отмечу готовность к старту приложения
    private boolean mReadyToStart = false;
    private boolean mActivityVisible;
    private boolean mTorLoadTooLong;
    private boolean AvailabilityCheckBegin = false;
    private StartViewModel mViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !App.getInstance().isExternalVpn()) {
            // show dialog window about tor not working here
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
            dialogBuilder
                    .setTitle(getString(R.string.tor_not_availiable_title))
                    .setMessage(getString(R.string.tor_not_availiable_message))
                    .setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
                        App.getInstance().switchExternalVpnUse();
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
                    })
                    .setNegativeButton(getString(android.R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                    .show();
        }
        if(MyPreferences.getInstance().isSkipMainScreen()){
            Toast.makeText(this, getString(R.string.lockscreen_scipped_message),Toast.LENGTH_LONG).show();
            startView();
            finish();
            return;
        }
        mViewModel = new ViewModelProvider(this).get(StartViewModel.class);
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
        Log.d("surprise", "onCreate:  hide pictures " + MyPreferences.getInstance().isPicHide());
        if (!MyPreferences.getInstance().isPicHide()) {
            View rootView = findViewById(R.id.rootView);
            if(rootView != null){
                // назначу фон
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    rootView.setBackground(ContextCompat.getDrawable(this, R.drawable.back_3));
                } else {
                    rootView.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.back_3, null));
                }
            }
        }
    }

    private void showSelectDownloadFolderDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
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
                        if (intentCanBeHandled(intent)) {
                            startActivityForResult(intent, DOWNLOAD_FOLDER_SELECT_REQUEST_CODE);
                        } else {
                            intent = new Intent(this, FolderPicker.class);
                            intent.addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            );
                            startActivityForResult(intent, DOWNLOAD_FOLDER_SELECT_OLD_REQUEST_CODE);
                        }
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
                .setNegativeButton("Нет, закрыть приложение", (dialog, which) -> finish())
                .setNeutralButton("Да (v2)", (dialog, which) -> showAlterDirSelectDialog());
        if (!MainActivity.this.isFinishing()) {
            dialogBuilder.create().show();
        }
    }

    private void showAlterDirSelectDialog() {
        new AlertDialog.Builder(this, R.style.MyDialogStyle)
                .setTitle("Альтернативный выбор папки")
                .setMessage("На случай, если папка для скачивания не выбирается основным методом. Только для совместимости, никаких преимуществ этот способ не даёт, также выбранная папка может сбрасываться при перезагрузке смартфона и её придётся выбирать заново")
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Intent intent = new Intent(this, FolderPicker.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        intent.addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        );
                    }
                    startActivityForResult(intent, DOWNLOAD_FOLDER_SELECT_OLD_REQUEST_CODE);
                })
                .create().show();
    }

    private void setupUI() {
        if (MyPreferences.getInstance().isHardwareAcceleration()) {
            // проверю аппаратное ускорение
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
        // если используем внешнй VPN- проверяю только выдачу разрешений и настройку внешнего вида
        if (MyPreferences.getInstance().isEInk()) {
            setContentView(R.layout.activity_main_eink);
            // проверю, включен ли wi-fi
            checkWiFiEnabled();
        } else {
            setContentView(R.layout.activity_main);
        }

        if (App.getInstance().isExternalVpn()) {
            // пропускаю дальше
            startView();
        } else {
            // переключатель аппаратного ускорения
            SwitchCompat switcher = findViewById(R.id.useHardwareAccelerationSwitcher);
            if (switcher != null) {
                switcher.setChecked(MyPreferences.getInstance().isHardwareAcceleration());
                switcher.setOnCheckedChangeListener((buttonView, isChecked) -> MyPreferences.getInstance().switchHardwareAcceleration());
            }
            // переключатель электронной книги
            switcher = findViewById(R.id.isEbook);
            if (switcher != null) {
                switcher.setChecked(MyPreferences.getInstance().isEInk());
                switcher.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if(isChecked){
                        Log.d("surprise", "setupUI: show dialog");
                        showEinkEnabledDialog();
                    }
                    MyPreferences.getInstance().setEInk(isChecked);
                    recreate();
                });
            }

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
                startView();
            });
        }
    }

    private void showEinkEnabledDialog() {
        new AlertDialog.Builder(this, R.style.MyDialogStyle)
                .setTitle("Enabled e-ink theme")
                .setMessage("It not support night mode and can be switched off here or on settings screen")
                .show();
    }

    private void checkWiFiEnabled() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            showEnableWifiDialog();
        }
    }

    private void showEnableWifiDialog() {
        if (!MainActivity.this.isFinishing()) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
            dialogBuilder
                    .setTitle(getString(R.string.enable_wifi_title))
                    .setMessage(getString(R.string.wifi_enable_message))
                    .setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
                        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        wifiManager.setWifiEnabled(true);
                    })
                    .setNegativeButton(getString(android.R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                    .show();
        }
    }

    private void showCheckTooLongDialog() {
        if (!MainActivity.this.isFinishing()) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
            dialogBuilder.setMessage(getString(R.string.check_too_long_message));
            dialogBuilder.setPositiveButton(getString(R.string.disable_connectivity_check_message), (dialogInterface, i) -> {
                Toast.makeText(MainActivity.this, getString(R.string.option_re_enabled_message), Toast.LENGTH_SHORT).show();
                MyPreferences.getInstance().setInspectionEnabled(false);
                startView();
            });
            dialogBuilder.setNegativeButton(getString(R.string.wait_more_item), (dialogInterface, i) -> dialogInterface.dismiss());
            dialogBuilder.setNeutralButton(getString(R.string.skip_inspection_item), (dialog, which) -> startView());
            dialogBuilder.show();
        }
    }

    protected void setupObservers() {
        if (!App.getInstance().isExternalVpn()) {
            // зарегистрирую отслеживание загружающегося TOR
            LiveData<AndroidOnionProxyManager> loadedTor = App.getInstance().mLoadedTor;
            loadedTor.observe(this, tor -> {
                if (tor != null) {
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
        // сбрасываю таймер. Если выбран вид приложения- запущу Activity согласно виду. Иначе- отмечу, что TOR загружен и буду ждать выбора вида
        if (App.getInstance().getView() != 0) {
            if (MyPreferences.getInstance().isCheckAvailability()) {
                checkFlibustaAvailability();
            } else {
                startView();
            }
        } else {
            mReadyToStart = true;
        }
    }

    private void handleStart() {
        // проверю, выбран ли внешний вид приложения
        if (App.getInstance().getView() != 0) {
            // если приложению передана ссылка на страницу
            if (getIntent().getData() != null) {//check if intent is not null
                mLink = getIntent().getData();//set a variable for the WebViewActivity
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                startView();
            }
        } else {
            selectView();
        }
        setupObservers();
    }

    private void selectView() {
        if (!MainActivity.this.isFinishing()) {
            // покажу диалог выбора вида приложения
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
            dialogBuilder.setTitle("Выберите внешний вид")
                    .setMessage("Выберите вид приложения. В будущем вы можете переключить вид в меню приложения (Меню => внешний вид). В режиме WebView информация берётся непосредственно с сайта Флибусты и выглядит как страница сайта. В режиме OPDS информация получается из электронного каталога Флибусты. Рекомендую попробовать оба режима, у каждого из них свои плюсы. Приятного поиска.")
                    .setCancelable(false)
                    .setPositiveButton("Режим WebView", (dialog, which) -> {
                        App.getInstance().setView(App.VIEW_WEB);
                        if (mReadyToStart) {
                            if (MyPreferences.getInstance().isCheckAvailability()) {
                                checkFlibustaAvailability();
                            } else {
                                startView();
                            }
                        }
                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                            startView();
                        }
                    })
                    .setNegativeButton("Режим OPDS", (dialog, which) -> {
                        App.getInstance().setView(App.VIEW_ODPS);
                        if (mReadyToStart) {
                            if (MyPreferences.getInstance().isCheckAvailability()) {
                                checkFlibustaAvailability();
                            } else {
                                startView();
                            }
                        }
                    });
            dialogBuilder.create().show();
        }
    }

        private void showPermissionDialog() {
            if (!MainActivity.this.isFinishing()) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
                dialogBuilder.setTitle("Необходимо предоставить разрешения")
                        .setMessage("Для загрузки книг необходимо предоставить доступ к памяти устройства")
                        .setCancelable(false)
                        .setPositiveButton("Предоставить разрешение", (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_WRITE_READ))
                        .setNegativeButton("Нет, закрыть приложение", (dialog, which) -> finish());
                dialogBuilder.create().show();
            }
        }

    private void checkFlibustaAvailability() {
        if (mTorLoadingStatusText != null) {
            mTorLoadingStatusText.setText(getString(R.string.check_flibusta_availability_message));
        }
        if (mViewModel == null) {
            mViewModel = new ViewModelProvider(this).get(StartViewModel.class);
        }
        if (!AvailabilityCheckBegin) {
            handleAction(mViewModel.checkFlibustaAvailability());
            AvailabilityCheckBegin = true;
        }
        // тут проверю доступность флибусты. Если она недоступна- перенаправлю на страницу ожидания подключения
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
        int checkAvailabilityLimit;
        if(MyPreferences.getInstance().isEInk()){
            checkAvailabilityLimit = 60;
        }
        else{
            checkAvailabilityLimit = 30;
        }
        if (mProgressCounter == 0 && mTorLoadingProgressIndicator != null) {
            mProgressCounter = 1;
            mCdt = new CountDownTimer(waitingTime, 1000) {
                public void onTick(long millisUntilFinished) {
                    mProgressCounter++;
                    mTorLoadingProgressIndicator.setProgress(mProgressCounter);
                    CharSequence text = mTorLoadingStatusText.getText();
                    if (text != null && text.length() > 0 && text.toString().equals(getString(R.string.check_flibusta_availability_message))) {
                        FlibustaCheckCounter++;
                        if (FlibustaCheckCounter == checkAvailabilityLimit) {
                            showCheckTooLongDialog();
                        }
                    } else {
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
                }

                public void onFinish() {
                    // tor не загрузился, покажу сообщение с предложением подождать или перезапустить процесс
                    torLoadTooLongDialog();
                }
            };
            mCdt.start();
        }
    }


    private void stopTimer() {
        mProgressCounter = 0;
        if(mTorLoadingProgressIndicator != null){
            mTorLoadingProgressIndicator.setProgress(0);
            if (mCdt != null) {
                mCdt.cancel();
            }
        }
    }


    private void torLoadTooLongDialog() {
        if (!MainActivity.this.isFinishing()) {
            if (mActivityVisible) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
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
        if (!MainActivity.this.isFinishing()) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
            dialogBuilder.setTitle(getString(R.string.tor_cant_load_message))
                    .setMessage(getString(R.string.tor_not_start_body))
                    .setPositiveButton(getString(R.string.try_again_message), (dialogInterface, i) -> App.getInstance().startTor())
                    .setNegativeButton(getString(R.string.try_later_message), (dialogInterface, i) -> finishAffinity())
                    .setNeutralButton(getString(R.string.use_external_proxy_message), (dialog, which) -> handleUseExternalVpn())
                    .show();
        }
    }


    private void handleUseExternalVpn() {
        if (!MainActivity.this.isFinishing()) {
            // покажу диалог с объяснением последствий включения VPN
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
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
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    App.getInstance().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    App.getInstance().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                }
                                App.getInstance().setDownloadDir(treeUri);
                                handleStart();
                                return;
                            } catch (Exception e) {
                                Toast.makeText(this, "Не удалось выдать разрешения на доступ, попробуем другой метод", Toast.LENGTH_SHORT).show();
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
                    if (file.isDirectory() && MyPreferences.getInstance().saveDownloadFolder(folderLocation)) {
                        Toast.makeText(this, "Папка сохранена!", Toast.LENGTH_SHORT).show();
                        handleStart();
                    } else {
                        Toast.makeText(this, "Не удалось сохранить папку, попробуйте ещё раз!", Toast.LENGTH_SHORT).show();
                        showSelectDownloadFolderDialog();
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleAction(LiveData<WorkInfo> confirmTask) {
        if (confirmTask != null) {
            // отслежу выполнение задачи, после чего обновлю информацию
            confirmTask.observe(this, workInfo -> {
                if (workInfo != null) {
                    if (workInfo.getState() == SUCCEEDED) {
                        Data data = workInfo.getOutputData();
                        if (data.getBoolean(CheckFlibustaAvailabilityWorker.AVAILABILITY_STATE, false)) {
                            startView();
                        } else {
                            availabilityTestFailed();
                        }
                    } else if (workInfo.getState() == FAILED || workInfo.getState() == CANCELLED) {
                        availabilityTestFailed();
                    }
                }
            });
        } else {
            startView();
        }
    }

    private void availabilityTestFailed() {
        // запущу активити, которое напишет, что флибуста недоступна и предложит попробовать позже или закрыть приложение
        startActivity(new Intent(MainActivity.this, FlibustaNotAvailableActivity.class));
        finish();
    }

    private void startView() {
        // проверю очередь скачивания. Если она не пуста- предложу продолжить закачку
        // проверю, не запущено ли приложение с помощью интента. Если да- запущу программу в webView режиме
        Intent targetActivityIntent;
        if (mLink != null) {
            //check if intent is not null
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCdt != null) {
            mCdt.cancel();
        }
    }
}