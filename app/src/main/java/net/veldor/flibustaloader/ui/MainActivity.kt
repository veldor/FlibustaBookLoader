package net.veldor.flibustaloader.ui

import android.Manifest
import net.veldor.flibustaloader.utils.Grammar.appVersion
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibustaloader.R
import androidx.lifecycle.LiveData
import net.veldor.flibustaloader.App
import androidx.appcompat.widget.SwitchCompat
import android.content.DialogInterface
import android.os.Build
import android.content.Intent
import android.os.CountDownTimer
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager
import net.veldor.flibustaloader.view_models.StartViewModel
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import lib.folderpicker.FolderPicker
import android.net.wifi.WifiManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import android.content.pm.PackageManager
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibustaloader.workers.CheckFlibustaAvailabilityWorker
import android.net.Uri
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.TransportUtils.intentCanBeHandled
import java.io.File
import java.lang.Exception
import java.util.*

class MainActivity : BaseActivity() {
    private var mLink: Uri? = null
    private lateinit var mTorLoadingProgressIndicator: ProgressBar
    private lateinit var mTorLoadingStatusText: TextView
    private var mProgressCounter = 0
    private var FlibustaCheckCounter = 0
    private lateinit var mCdt: CountDownTimer
    private var mTor: AndroidOnionProxyManager? = null

    // отмечу готовность к старту приложения
    private var mReadyToStart = false
    private var mActivityVisible = false
    private var mTorLoadTooLong = false
    private var AvailabilityCheckBegin = false
    private var mViewModel: StartViewModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !App.getInstance().isExternalVpn()) {
//            // show dialog window about tor not working here
//            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
//            dialogBuilder
//                    .setTitle(getString(R.string.tor_not_availiable_title))
//                    .setMessage(getString(R.string.tor_not_availiable_message))
//                    .setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
//                        App.getInstance().switchExternalVpnUse();
//                        if (!permissionGranted()) {
//                            // показываю диалог с требованием предоставить разрешения
//                            showPermissionDialog();
//                        } else {
//                            if (MyPreferences.getInstance().isDownloadDir()) {
//                                showSelectDownloadFolderDialog();
//                            } else {
//                                handleStart();
//                            }
//                        }
//                    })
//                    .setNegativeButton(getString(android.R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
//                    .show();
//        }
        if (PreferencesHandler.instance.isSkipMainScreen()) {
            Toast.makeText(this, getString(R.string.lockscreen_scipped_message), Toast.LENGTH_LONG)
                .show()
            startView()
            finish()
            return
        }
        mViewModel = ViewModelProvider(this).get(StartViewModel::class.java)
        setupUI()
        // если пользователь заходит в приложение впервые- предложу предоставить разрешение на доступ к файлам и выбрать вид
        if (!permissionGranted()) {
            // показываю диалог с требованием предоставить разрешения
            showPermissionDialog()
        } else {
            if (PreferencesHandler.instance.isDownloadDir()) {
                showSelectDownloadFolderDialog()
            } else {
                handleStart()
            }
        }
        if (PreferencesHandler.instance.isPicHide()) {
            val rootView = findViewById<View>(R.id.rootView)
            if (rootView != null) {
                // назначу фон
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    rootView.background = ContextCompat.getDrawable(this, R.drawable.back_3)
                } else {
                    rootView.background =
                        ResourcesCompat.getDrawable(resources, R.drawable.back_3, null)
                }
            }
        }
    }

    private fun showSelectDownloadFolderDialog() {
        val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
        dialogBuilder.setTitle("Выберите папку для сохранения")
            .setMessage("Выберите папку, в которой будут храниться скачанные книги")
            .setCancelable(false)
            .setPositiveButton("Ок") { _: DialogInterface?, _: Int ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    var intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                    )
                    if (intentCanBeHandled(intent)) {
                        startActivityForResult(intent, DOWNLOAD_FOLDER_SELECT_REQUEST_CODE)
                    } else {
                        intent = Intent(this, FolderPicker::class.java)
                        intent.addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        )
                        startActivityForResult(intent, DOWNLOAD_FOLDER_SELECT_OLD_REQUEST_CODE)
                    }
                } else {
                    val intent = Intent(this, FolderPicker::class.java)
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                        intent.addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        )
                    }
                    startActivityForResult(intent, DOWNLOAD_FOLDER_SELECT_OLD_REQUEST_CODE)
                }
            }
            .setNegativeButton("Нет, закрыть приложение") { _: DialogInterface?, _: Int -> finish() }
            .setNeutralButton("Да (v2)") { _: DialogInterface?, _: Int -> showAlterDirSelectDialog() }
        if (!this@MainActivity.isFinishing) {
            dialogBuilder.create().show()
        }
    }

    private fun showAlterDirSelectDialog() {
        AlertDialog.Builder(this, R.style.MyDialogStyle)
            .setTitle("Альтернативный выбор папки")
            .setMessage("На случай, если папка для скачивания не выбирается основным методом. Только для совместимости, никаких преимуществ этот способ не даёт, также выбранная папка может сбрасываться при перезагрузке смартфона и её придётся выбирать заново")
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                val intent = Intent(this, FolderPicker::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    )
                }
                startActivityForResult(intent, DOWNLOAD_FOLDER_SELECT_OLD_REQUEST_CODE)
            }
            .create().show()
    }

    private fun setupUI() {
        if (PreferencesHandler.instance.isHardwareAcceleration()) {
            // проверю аппаратное ускорение
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        }
        // если используем внешнй VPN- проверяю только выдачу разрешений и настройку внешнего вида
        if (PreferencesHandler.instance.isEInk) {
            setContentView(R.layout.activity_main_eink)
            // проверю, включен ли wi-fi
            checkWiFiEnabled()
        } else {
            setContentView(R.layout.activity_main)
        }
        if (PreferencesHandler.instance.isExternalVpn) {
            // пропускаю дальше
            startView()
        } else {
            // переключатель аппаратного ускорения
            var switcher = findViewById<SwitchCompat>(R.id.useHardwareAccelerationSwitcher)
            if (switcher != null) {
                switcher.isChecked = PreferencesHandler.instance.isHardwareAcceleration()
                switcher.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean ->
                    PreferencesHandler.instance.switchHardwareAcceleration()
                }
            }
            // переключатель электронной книги
            switcher = findViewById(R.id.isEbook)
            if (switcher != null) {
                switcher.isChecked = PreferencesHandler.instance.isEInk
                switcher.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                    if (isChecked) {
                        Log.d("surprise", "setupUI: show dialog")
                        showEinkEnabledDialog()
                    }
                    PreferencesHandler.instance.setEInk(isChecked)
                    recreate()
                }
            }

            // найду индикатор прогресса
            mTorLoadingProgressIndicator = findViewById(R.id.progressBarCircle)!!
            mTorLoadingProgressIndicator.setProgress(0)
            mTorLoadingProgressIndicator.setMax(TOR_LOAD_MAX_TIME)
            // найду строку статуса загрузки
            mTorLoadingStatusText = findViewById(R.id.progressTorLoadStatus)
            mTorLoadingStatusText.setText(getString(R.string.begin_tor_init_msg))

            // отображу версию приложения
            val versionView = findViewById<TextView>(R.id.app_version)
            val version = appVersion
            versionView.text = String.format(
                Locale.ENGLISH,
                getString(R.string.application_version_message),
                version
            )
            val startBtn = findViewById<Button>(R.id.testStartApp)
            startBtn.setOnClickListener { v: View? ->
                // покажу диалог, предупреждающий о том, что это не запустит приложение
                startView()
            }
        }
    }

    private fun showEinkEnabledDialog() {
        AlertDialog.Builder(this, R.style.MyDialogStyle)
            .setTitle("Enabled e-ink theme")
            .setMessage("It not support night mode and can be switched off here or on settings screen")
            .show()
    }

    private fun checkWiFiEnabled() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            showEnableWifiDialog()
        }
    }

    private fun showEnableWifiDialog() {
        if (!this@MainActivity.isFinishing) {
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder
                .setTitle(getString(R.string.enable_wifi_title))
                .setMessage(getString(R.string.wifi_enable_message))
                .setPositiveButton(getString(android.R.string.ok)) { dialogInterface: DialogInterface?, i: Int ->
                    val wifiManager = applicationContext.getSystemService(
                        WIFI_SERVICE
                    ) as WifiManager
                    wifiManager.isWifiEnabled = true
                }
                .setNegativeButton(getString(android.R.string.cancel)) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
                .show()
        }
    }

    private fun showCheckTooLongDialog() {
        if (!this@MainActivity.isFinishing) {
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setMessage(getString(R.string.check_too_long_message))
            dialogBuilder.setPositiveButton(getString(R.string.disable_connectivity_check_message)) { dialogInterface: DialogInterface?, i: Int ->
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.option_re_enabled_message),
                    Toast.LENGTH_SHORT
                ).show()
                PreferencesHandler.instance.setInspectionEnabled(false)
                startView()
            }
            dialogBuilder.setNegativeButton(getString(R.string.wait_more_item)) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
            dialogBuilder.setNeutralButton(getString(R.string.skip_inspection_item)) { dialog: DialogInterface?, which: Int -> startView() }
            dialogBuilder.show()
        }
    }

    override fun setupObservers() {
        if (!PreferencesHandler.instance.isExternalVpn) {
            // зарегистрирую отслеживание загружающегося TOR
            val loadedTor: LiveData<AndroidOnionProxyManager> = App.instance.mLoadedTor
            loadedTor.observe(this, { tor: AndroidOnionProxyManager? ->
                if (tor != null) {
                    mTor = tor
                }
            })
            // получу данные о работе
            val workInfoData =
                WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(App.START_TOR)
            workInfoData.observe(this, { workInfos: List<WorkInfo>? ->
                if (workInfos != null && workInfos.isNotEmpty()) {
                    // переберу статусы
                    val data = workInfos[0]
                    when (data.state) {
                        WorkInfo.State.ENQUEUED -> {
                            mTorLoadingStatusText.text =
                                getString(R.string.tor_load_waiting_internet_message)
                            stopTimer()
                        }
                        WorkInfo.State.RUNNING -> {
                            // запускаю таймер
                            startTimer()
                            mTorLoadingStatusText.text =
                                getString(R.string.launch_begin_message)
                        }
                        WorkInfo.State.CANCELLED -> {
                            stopTimer()
                            mTorLoadingStatusText.text =
                                getString(R.string.launch_cancelled_message)
                        }
                        WorkInfo.State.FAILED -> showTorNotWorkDialog()
                        WorkInfo.State.SUCCEEDED -> torLoaded()
                    }
                }
            })
        } else {
            torLoaded()
        }
    }

    private fun torLoaded() {
        // сбрасываю таймер. Если выбран вид приложения- запущу Activity согласно виду. Иначе- отмечу, что TOR загружен и буду ждать выбора вида
        if (App.instance.view != 0) {
            if (PreferencesHandler.instance.isCheckAvailability()) {
                checkFlibustaAvailability()
            } else {
                startView()
            }
        } else {
            mReadyToStart = true
        }
    }

    private fun handleStart() {
        // проверю, выбран ли внешний вид приложения
        if (App.instance.view != 0) {
            // если приложению передана ссылка на страницу
            if (intent.data != null) { //check if intent is not null
                mLink = intent.data //set a variable for the WebViewActivity
            }
            //            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
//                startView();
//            }
        } else {
            selectView()
        }
        setupObservers()
    }

    private fun selectView() {
        if (!this@MainActivity.isFinishing) {
            // покажу диалог выбора вида приложения
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle("Выберите внешний вид")
                .setMessage("Выберите вид приложения. В будущем вы можете переключить вид в меню приложения (Меню => внешний вид). В режиме WebView информация берётся непосредственно с сайта Флибусты и выглядит как страница сайта. В режиме OPDS информация получается из электронного каталога Флибусты. Рекомендую попробовать оба режима, у каждого из них свои плюсы. Приятного поиска.")
                .setCancelable(false)
                .setPositiveButton("Режим WebView") { dialog: DialogInterface?, which: Int ->
                    App.instance.view = App.VIEW_WEB
                    if (mReadyToStart) {
                        if (PreferencesHandler.instance.isCheckAvailability()) {
                            checkFlibustaAvailability()
                        } else {
                            startView()
                        }
                    }
                }
                .setNegativeButton("Режим OPDS") { dialog: DialogInterface?, which: Int ->
                    App.instance.view = App.VIEW_ODPS
                    if (mReadyToStart) {
                        if (PreferencesHandler.instance.isCheckAvailability()) {
                            checkFlibustaAvailability()
                        } else {
                            startView()
                        }
                    }
                }
            dialogBuilder.create().show()
        }
    }

    private fun showPermissionDialog() {
        if (!this@MainActivity.isFinishing) {
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle("Необходимо предоставить разрешения")
                .setMessage("Для загрузки книг необходимо предоставить доступ к памяти устройства")
                .setCancelable(false)
                .setPositiveButton("Предоставить разрешение") { _: DialogInterface?, _: Int ->
                    ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ), REQUEST_WRITE_READ
                    )
                }
                .setNegativeButton("Нет, закрыть приложение") { _: DialogInterface?, _: Int -> finish() }
            dialogBuilder.create().show()
        }
    }

    private fun checkFlibustaAvailability() {
        mTorLoadingStatusText.text = getString(R.string.check_flibusta_availability_message)
        if (mViewModel == null) {
            mViewModel = ViewModelProvider(this).get(StartViewModel::class.java)
        }
        if (!AvailabilityCheckBegin) {
            handleAction(mViewModel!!.checkFlibustaAvailability())
            AvailabilityCheckBegin = true
        }
        // тут проверю доступность флибусты. Если она недоступна- перенаправлю на страницу ожидания подключения
    }

    private fun permissionGranted(): Boolean {
        val writeResult: Int
        val readResult: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            writeResult = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            readResult = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            writeResult = PermissionChecker.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            readResult = PermissionChecker.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        return writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_WRITE_READ && grantResults.size > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog()
            } else {
                // проверю, выбрана ли папка
                if (PreferencesHandler.instance.isDownloadDir()) {
                    showSelectDownloadFolderDialog()
                } else {
                    handleStart()
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // запущу таймер отсчёта
    private fun startTimer() {
        val waitingTime = TOR_LOAD_MAX_TIME * 1000 // 3 minute in milli seconds
        val checkAvailabilityLimit: Int = if (PreferencesHandler.instance.isEInk) {
            60
        } else {
            30
        }
        if (mProgressCounter == 0) {
            mProgressCounter = 1
            mCdt = object : CountDownTimer(waitingTime.toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    mProgressCounter++
                    mTorLoadingProgressIndicator.progress = mProgressCounter
                    val text = mTorLoadingStatusText.text
                    if (text != null && text.isNotEmpty() && text.toString() == getString(R.string.check_flibusta_availability_message)) {
                        FlibustaCheckCounter++
                        if (FlibustaCheckCounter == checkAvailabilityLimit) {
                            showCheckTooLongDialog()
                        }
                    } else {
                        if (mTor != null) {
                            val last = mTor!!.lastLog
                            if (last != null) {
                                if (!last.isEmpty()) {
                                    mTorLoadingStatusText.text = last
                                } else {
                                    mTorLoadingStatusText.text = String.format(
                                        Locale.ENGLISH,
                                        getString(R.string.tor_continue_loading),
                                        mProgressCounter
                                    )
                                }
                            } else {
                                mTorLoadingStatusText.text = String.format(
                                    Locale.ENGLISH,
                                    getString(R.string.tor_continue_loading),
                                    mProgressCounter
                                )
                            }
                        } else {
                            mTorLoadingStatusText.setText(R.string.wait_tor_loading_message)
                        }
                    }
                }

                override fun onFinish() {
                    // tor не загрузился, покажу сообщение с предложением подождать или перезапустить процесс
                    torLoadTooLongDialog()
                }
            }
            mCdt.start()
        }
    }

    private fun stopTimer() {
        mProgressCounter = 0
        mTorLoadingProgressIndicator.progress = 0
        mCdt.cancel()
    }

    private fun torLoadTooLongDialog() {
        if (!this@MainActivity.isFinishing) {
            if (mActivityVisible) {
                val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
                dialogBuilder.setTitle("Tor load too long")
                    .setMessage("Подождём ещё или перезапустим?")
                    .setPositiveButton("Перезапуск") { _: DialogInterface?, _: Int ->
                        App.sTorStartTry = 0
                        App.instance.startTor()
                    }
                    .setNegativeButton("Подождать ещё") { _: DialogInterface?, _: Int -> startTimer() }
                    .show()
            } else {
                mTorLoadTooLong = true
            }
        }
    }

    private fun showTorNotWorkDialog() {
        if (!this@MainActivity.isFinishing) {
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle(getString(R.string.tor_cant_load_message))
                .setMessage(getString(R.string.tor_not_start_body))
                .setPositiveButton(getString(R.string.try_again_message)) { _: DialogInterface?, _: Int ->
                    App.instance.startTor()
                }
                .setNegativeButton(getString(R.string.try_later_message)) { dialogInterface: DialogInterface?, i: Int -> finishAffinity() }
                .setNeutralButton(getString(R.string.use_external_proxy_message)) { _: DialogInterface?, _: Int -> handleUseExternalVpn() }
                .show()
        }
    }

    private fun handleUseExternalVpn() {
        if (!this@MainActivity.isFinishing) {
            // покажу диалог с объяснением последствий включения VPN
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder
                .setTitle("Использование внешнего VPN")
                .setMessage("Оповестить об использовании внешнего VPN. В этом случае внутренний клиент TOR будет отключен и траффик приложения не будет обрабатываться. В этом случае вся ответственность за получение контента ложится на внешний VPN. Если вы будете получать сообщения об ошибках загрузки- значит, он работает неправильно. Сделано для версий Android ниже 6.0, где могут быть проблемы с доступом, но может быть использовано по желанию на ваш страх и риск.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm) { _: DialogInterface?, _: Int ->
                    PreferencesHandler.instance.isExternalVpn = !PreferencesHandler.instance.isExternalVpn
                    torLoaded()
                }
            dialogBuilder.create().show()
        }
    }

    override fun onPause() {
        super.onPause()
        mActivityVisible = false
    }

    override fun onResume() {
        super.onResume()
        mActivityVisible = true
        if (mTorLoadTooLong) {
            mTorLoadTooLong = false
            torLoadTooLongDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == DOWNLOAD_FOLDER_SELECT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    val treeUri = data.data
                    if (treeUri != null) {
                        // проверю наличие файла
                        val dl = DocumentFile.fromTreeUri(App.instance, treeUri)
                        if (dl != null && dl.isDirectory) {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    App.instance.contentResolver.takePersistableUriPermission(
                                        treeUri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                    App.instance.contentResolver.takePersistableUriPermission(
                                        treeUri,
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    )
                                }
                                App.instance.setDownloadDir(treeUri)
                                handleStart()
                                return
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this,
                                    "Не удалось выдать разрешения на доступ, попробуем другой метод",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(this, FolderPicker::class.java)
                                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                                    intent.addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                    )
                                }
                                startActivityForResult(
                                    intent,
                                    DOWNLOAD_FOLDER_SELECT_OLD_REQUEST_CODE
                                )
                            }
                        }
                    }
                }
            }
            showSelectDownloadFolderDialog()
        } else if (requestCode == DOWNLOAD_FOLDER_SELECT_OLD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (data != null && data.extras != null) {
                    val folderLocation = data.extras.getString("data")
                    val file = File(folderLocation)
                    if (file.isDirectory && PreferencesHandler.instance.saveDownloadFolder(folderLocation)) {
                        Toast.makeText(this, "Папка сохранена!", Toast.LENGTH_SHORT).show()
                        handleStart()
                    } else {
                        Toast.makeText(
                            this,
                            "Не удалось сохранить папку, попробуйте ещё раз!",
                            Toast.LENGTH_SHORT
                        ).show()
                        showSelectDownloadFolderDialog()
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleAction(confirmTask: LiveData<WorkInfo>?) {
        if (confirmTask != null) {
            // отслежу выполнение задачи, после чего обновлю информацию
            confirmTask.observe(this, Observer { workInfo: WorkInfo? ->
                if (workInfo != null) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        val data = workInfo.outputData
                        if (data.getBoolean(
                                CheckFlibustaAvailabilityWorker.AVAILABILITY_STATE,
                                false
                            )
                        ) {
                            startView()
                        } else {
                            availabilityTestFailed()
                        }
                    } else if (workInfo.state == WorkInfo.State.FAILED || workInfo.state == WorkInfo.State.CANCELLED) {
                        availabilityTestFailed()
                    }
                }
            })
        } else {
            startView()
        }
    }

    private fun availabilityTestFailed() {
        // запущу активити, которое напишет, что флибуста недоступна и предложит попробовать позже или закрыть приложение
        startActivity(Intent(this@MainActivity, FlibustaNotAvailableActivity::class.java))
        finish()
    }

    private fun startView() {
        // проверю очередь скачивания. Если она не пуста- предложу продолжить закачку
        // проверю, не запущено ли приложение с помощью интента. Если да- запущу программу в webView режиме
        val targetActivityIntent: Intent
        if (mLink != null) {
            //check if intent is not null
            targetActivityIntent = Intent(this, WebViewActivity::class.java)
            targetActivityIntent.data = mLink
        } else {
            // проверю, если используем ODPS- перенаправлю в другую активность
            targetActivityIntent = if (App.instance.view == App.VIEW_ODPS) {
                Intent(this, OPDSActivity::class.java)
            } else {
                Intent(this, WebViewActivity::class.java)
            }
        }
        targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(targetActivityIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCdt.cancel()
    }

    companion object {
        private const val REQUEST_WRITE_READ = 22
        const val START_TOR = 3
        private const val TOR_LOAD_MAX_TIME = 180
        private const val DOWNLOAD_FOLDER_SELECT_REQUEST_CODE = 23
        private const val DOWNLOAD_FOLDER_SELECT_OLD_REQUEST_CODE = 24
    }
}