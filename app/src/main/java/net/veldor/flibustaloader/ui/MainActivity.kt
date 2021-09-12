package net.veldor.flibustaloader.ui

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager
import lib.folderpicker.FolderPicker
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.ActivityMainBinding
import net.veldor.flibustaloader.utils.Grammar.appVersion
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.TransportUtils.intentCanBeHandled
import net.veldor.flibustaloader.view_models.StartViewModel
import java.io.File
import java.util.*
import kotlin.system.exitProcess

class MainActivity : BaseActivity() {
    private var previousText: String = ""
    private lateinit var binding: ActivityMainBinding
    private var mLink: Uri? = null
    private var mProgressCounter = 0
    private var flibustaCheckCounter = 0
    private lateinit var mCdt: CountDownTimer
    private var mTor: AndroidOnionProxyManager? = null

    // отмечу готовность к старту приложения
    private var mReadyToStart = false
    private var mActivityVisible = false
    private var mTorLoadTooLong = false
    private lateinit var viewModel: StartViewModel

    private var compatDirSelectResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data != null && data.extras != null && data.extras!!.containsKey("data")) {
                    val folderLocation = data.extras!!.getString("data")
                    val file = File(folderLocation)
                    if (file.isDirectory && PreferencesHandler.instance.saveDownloadFolder(
                            folderLocation
                        )
                    ) {
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

    private var dirSelectResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
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
                                PreferencesHandler.instance.downloadDir = dl
                                handleStart()
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
                                compatDirSelectResultLauncher.launch(intent)
                            }
                        }
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("surprise", "onCreate: launch main activity")
        viewModel = ViewModelProvider(this).get(StartViewModel::class.java)
        // буду отслеживать проверку доступности флибусты
        viewModel.connectionTestSuccess.observe(this, {
            if (it) {
                launchView()
            }
        })
        viewModel.connectionTestFailed.observe(this, {
            if (it) {
                availabilityTestFailed()
            }
        })
        // проверю на пропуск главного экрана
        if (PreferencesHandler.instance.isSkipMainScreen() || PreferencesHandler.instance.isExternalVpn) {
            Toast.makeText(this, getString(R.string.lockscreen_scipped_message), Toast.LENGTH_LONG)
                .show()
            launchView()
            finish()
            return
        }
        setupUI()

        // если пользователь заходит в приложение впервые- предложу предоставить разрешение на доступ к файлам и выбрать вид
        if (!viewModel.permissionGranted()) {
            // показываю диалог с требованием предоставить разрешения
            showPermissionDialog()
        } else {
            // если не выбрана папка для загрузки
            if (!PreferencesHandler.instance.downloadDirAssigned) {
                showSelectDownloadFolderDialog()
            } else {
                handleStart()
            }
        }
    }

    private fun showSelectDownloadFolderDialog() {
        val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
        dialogBuilder.setTitle(getString(R.string.select_download_folder_title))
            .setMessage(getString(R.string.select_download_folder_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.download_folder_select_accept)) { _: DialogInterface?, _: Int ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    var intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                    )
                    if (intentCanBeHandled(intent)) {
                        dirSelectResultLauncher.launch(intent)
                    } else {
                        intent = Intent(this, FolderPicker::class.java)
                        intent.addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        )
                        compatDirSelectResultLauncher.launch(intent)
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
                    compatDirSelectResultLauncher.launch(intent)
                }
            }
            .setNegativeButton(getString(R.string.dismiss_permissions_button)) { _: DialogInterface?, _: Int ->
                exitProcess(
                    0
                )
            }
            .setNeutralButton(getString(R.string.alternate_dir_select_button)) { _: DialogInterface?, _: Int -> showAlterDirSelectDialog() }
        if (!this@MainActivity.isFinishing) {
            val dialog = dialogBuilder.create()
            lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
            dialog.show()
        }
    }

    private fun showAlterDirSelectDialog() {
        val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            .setTitle(getString(R.string.alter_dir_selection_title))
            .setMessage(getString(R.string.alter_dir_selection_body))
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
                compatDirSelectResultLauncher.launch(intent)
            }
        val dialog = dialogBuilder.create()
        lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
        dialog.show()
    }

    private fun setupUI() {
        if (PreferencesHandler.instance.hardwareAcceleration) {
            // проверю аппаратное ускорение
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        if (PreferencesHandler.instance.isEInk) {
            // prepare window for eInk
            checkWiFiEnabled()
        } else {
            if (!PreferencesHandler.instance.isPicHide()) {
                // назначу фон
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    binding.rootView.background = ContextCompat.getDrawable(this, R.drawable.back_3)
                } else {
                    binding.rootView.background =
                        ResourcesCompat.getDrawable(resources, R.drawable.back_3, null)
                }
            }
            binding.isEbook.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
            binding.useHardwareAccelerationSwitcher.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )

            binding.statusWrapper.setInAnimation(this, android.R.anim.slide_in_left)
            binding.statusWrapper.setOutAnimation(this, android.R.anim.slide_out_right)
        }
        setContentView(binding.rootView)

        binding.useHardwareAccelerationSwitcher.isChecked =
            PreferencesHandler.instance.hardwareAcceleration
        binding.useHardwareAccelerationSwitcher.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean ->
            PreferencesHandler.instance.hardwareAcceleration =
                !PreferencesHandler.instance.hardwareAcceleration
        }
        // переключатель электронной книги
        binding.isEbook.isChecked = PreferencesHandler.instance.isEInk
        binding.isEbook.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                showEInkEnabledDialog()
            }
            PreferencesHandler.instance.setEInk(isChecked)
            recreate()
        }

        // найду индикатор прогресса
        binding.progressBarCircle.progress = 0
        binding.progressBarCircle.max = TOR_LOAD_MAX_TIME
        // найду строку статуса загрузки
        binding.statusWrapper.setText(getString(R.string.begin_tor_init_msg))

        // отображу версию приложения
        val version = appVersion
        binding.appVersion.text = String.format(
            Locale.ENGLISH,
            getString(R.string.application_version_message),
            version
        )
        binding.testStartApp.setOnClickListener {
            // покажу диалог, предупреждающий о том, что это не запустит приложение
            launchView()
        }
    }

    private fun showEInkEnabledDialog() {
        val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            .setTitle(getString(R.string.e_ink_enabled_title))
            .setMessage(getString(R.string.e_ink_enabled_message))

        val dialog = dialogBuilder.create()
        lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
        dialog.show()
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
                .setPositiveButton(getString(android.R.string.ok)) { _: DialogInterface?, _: Int ->
                    val wifiManager = applicationContext.getSystemService(
                        WIFI_SERVICE
                    ) as WifiManager
                    wifiManager.isWifiEnabled = true
                }
                .setNegativeButton(getString(android.R.string.cancel)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
            val dialog = dialogBuilder.create()
            lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
            dialog.show()
        }
    }

    private fun showCheckTooLongDialog() {
        val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
        dialogBuilder.setMessage(getString(R.string.check_too_long_message))
        dialogBuilder.setPositiveButton(getString(R.string.disable_connectivity_check_message)) { _: DialogInterface?, _: Int ->
            Toast.makeText(
                this@MainActivity,
                getString(R.string.option_re_enabled_message),
                Toast.LENGTH_SHORT
            ).show()
            PreferencesHandler.instance.setInspectionEnabled(false)
            launchView()
        }
        dialogBuilder.setNegativeButton(getString(R.string.wait_more_item)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
        dialogBuilder.setNeutralButton(getString(R.string.skip_inspection_item)) { _: DialogInterface?, _: Int -> launchView() }

        val dialog = dialogBuilder.create()
        lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
        dialog.show()
    }


    private fun removeObservers() {
        App.instance.mLoadedTor.removeObservers(this)
        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(App.START_TOR)
            .removeObservers(this)
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
                            binding.statusWrapper.setText(
                                getString(R.string.tor_load_waiting_internet_message)
                            )
                            stopTimer()
                        }
                        WorkInfo.State.RUNNING -> {
                            // запускаю таймер
                            startTimer()
                            binding.statusWrapper.setText(
                                getString(R.string.launch_begin_message)
                            )
                        }
                        WorkInfo.State.CANCELLED -> {
                            stopTimer()
                            binding.statusWrapper.setText(
                                getString(R.string.launch_cancelled_message)
                            )
                        }
                        WorkInfo.State.FAILED -> showTorNotWorkDialog()
                        else -> {
                            torLoaded()
                            workInfoData.removeObservers(this)
                        }
                    }
                }
            })
        }
    }

    private fun torLoaded() {
        // сбрасываю таймер. Если выбран вид приложения- запущу Activity согласно виду. Иначе- отмечу, что TOR загружен и буду ждать выбора вида
        if (PreferencesHandler.instance.view != 0) {
            if (PreferencesHandler.instance.isCheckAvailability()) {
                checkFlibustaAvailability()
            } else {
                launchView()
            }
        } else {
            mReadyToStart = true
        }
    }

    private fun handleStart() {
        // проверю, выбран ли внешний вид приложения
        if (PreferencesHandler.instance.view != 0) {
            // если приложению передана ссылка на страницу
            if (intent.data != null) {
                mLink = intent.data
            }
        } else {
            selectView()
        }
    }

    private fun selectView() {
        if (!this@MainActivity.isFinishing) {
            // покажу диалог выбора вида приложения
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder
                .setTitle(getString(R.string.select_view_title))
                .setMessage(getString(R.string.select_view_body))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.web_view_mode_button)) { _: DialogInterface?, _: Int ->
                    PreferencesHandler.instance.view = App.VIEW_WEB
                    if (mReadyToStart) {
                        if (PreferencesHandler.instance.isCheckAvailability()) {
                            checkFlibustaAvailability()
                        } else {
                            launchView()
                        }
                    }
                }
                .setNegativeButton(getString(R.string.opds_view_button)) { _: DialogInterface?, _: Int ->
                    PreferencesHandler.instance.view = App.VIEW_OPDS
                    if (mReadyToStart) {
                        if (PreferencesHandler.instance.isCheckAvailability()) {
                            checkFlibustaAvailability()
                        } else {
                            launchView()
                        }
                    }
                }

            val dialog = dialogBuilder.create()
            lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
            dialog.show()
        }
    }

    private fun showPermissionDialog() {
        if (!this@MainActivity.isFinishing) {
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle(getString(R.string.permissions_required_title))
                .setMessage(getString(R.string.permissions_required_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.accept_permissions_button)) { _: DialogInterface?, _: Int ->
                    ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ), REQUEST_WRITE_READ
                    )
                }
                .setNegativeButton(getString(R.string.dismiss_permissions_button)) { _: DialogInterface?, _: Int ->
                    exitProcess(
                        0
                    )
                }

            val dialog = dialogBuilder.create()
            lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
            dialog.show()
        }
    }

    private fun checkFlibustaAvailability() {
        Log.d("surprise", "checkFlibustaAvailability: init check")
        binding.statusWrapper.setText(getString(R.string.check_flibusta_availability_message))
        previousText = getString(R.string.check_flibusta_availability_message)
        viewModel.checkFlibustaAvailability()
        // тут проверю доступность флибусты. Если она недоступна- перенаправлю на страницу ожидания подключения
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_WRITE_READ && grantResults.isNotEmpty()) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog()
            } else {
                // проверю, выбрана ли папка
                if (!PreferencesHandler.instance.downloadDirAssigned) {
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
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        binding.progressBarCircle.progress = mProgressCounter
                        val text = previousText
                        if (text.isNotEmpty() && text == getString(R.string.check_flibusta_availability_message)) {
                            flibustaCheckCounter++
                            if (flibustaCheckCounter == checkAvailabilityLimit) {
                                showCheckTooLongDialog()
                            }
                        } else {
                            if (mTor != null) {
                                val last = mTor!!.lastLog
                                if (last != null) {
                                    if (last.isNotEmpty()) {
                                        if (last != previousText) {
                                            binding.statusWrapper.setText(last)
                                            previousText = last
                                        }
                                    } else {
                                        binding.statusWrapper.setText(
                                            String.format(
                                                Locale.ENGLISH,
                                                getString(R.string.tor_continue_loading),
                                                mProgressCounter
                                            )
                                        )
                                    }
                                } else {
                                    binding.statusWrapper.setText(
                                        String.format(
                                            Locale.ENGLISH,
                                            getString(R.string.tor_continue_loading),
                                            mProgressCounter
                                        )
                                    )
                                }
                            } else {
                                binding.statusWrapper.setText(getString(R.string.wait_tor_loading_message))
                            }
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
        binding.progressBarCircle.progress = 0
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

                val dialog = dialogBuilder.create()
                lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
                dialog.show()
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
                .setNegativeButton(getString(R.string.try_later_message)) { _: DialogInterface?, _: Int -> finishAffinity() }
                .setNeutralButton(getString(R.string.use_external_proxy_message)) { _: DialogInterface?, _: Int -> handleUseExternalVpn() }

            val dialog = dialogBuilder.create()
            lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
            dialog.show()
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
                    PreferencesHandler.instance.isExternalVpn =
                        !PreferencesHandler.instance.isExternalVpn
                    torLoaded()
                }
            val dialog = dialogBuilder.create()
            lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
            dialog.show()
        }
    }

    override fun onPause() {
        super.onPause()
        mActivityVisible = false
        removeObservers()
    }


    override fun onResume() {
        super.onResume()
        setupObservers()
        mActivityVisible = true
        if (mTorLoadTooLong) {
            mTorLoadTooLong = false
            torLoadTooLongDialog()
        }
    }

    private fun availabilityTestFailed() {
        // запущу активити, которое напишет, что флибуста недоступна и предложит попробовать позже или закрыть приложение
        startActivity(Intent(this@MainActivity, FlibustaNotAvailableActivity::class.java))
        finish()
    }

    private fun launchView() {
        viewModel.cancelCheck()
        // проверю очередь скачивания. Если она не пуста- предложу продолжить закачку
        // проверю, не запущено ли приложение с помощью интента. Если да- запущу программу в webView режиме
        val targetActivityIntent: Intent
        if (mLink != null) {
            //check if intent is not null
            targetActivityIntent = Intent(this, WebViewActivity::class.java)
            targetActivityIntent.data = mLink
        } else {
            // проверю, если используем OPDS- перенаправлю в другую активность
            targetActivityIntent = if (PreferencesHandler.instance.view == App.VIEW_OPDS) {
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
        viewModel.connectionTestSuccess.removeObservers(this)
    }

    companion object {
        private const val REQUEST_WRITE_READ = 22
        private const val TOR_LOAD_MAX_TIME = 180
    }

}
