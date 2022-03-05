package net.veldor.flibustaloader.ui

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.ActivityMainBinding
import net.veldor.flibustaloader.http.TorStarter
import net.veldor.flibustaloader.utils.*
import net.veldor.flibustaloader.utils.Grammar.appVersion
import net.veldor.flibustaloader.view_models.StartViewModel
import java.util.*
import kotlin.system.exitProcess

class MainActivity : BaseActivity() {
    private var previousText: String = ""
    private lateinit var binding: ActivityMainBinding
    private var link: Uri? = null
    private var flibustaCheckCounter = 0
    private var mCdt: CountDownTimer? = null
    private var mTor: AndroidOnionProxyManager? = null
    private var mTorLoadTooLong = false
    private lateinit var viewModel: StartViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // если это первый запуск- перенаправлю в гайд по настройкам приложения
        if (PreferencesHandler.instance.isFirstUse()) {
            val targetActivityIntent = Intent(this, IntroductionGuideActivity::class.java)
            targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(targetActivityIntent)
            finish()
        } else {
            viewModel = ViewModelProvider(this).get(StartViewModel::class.java)
            setupObservers()
            if (App.instance.migrationError) {
                // покажу диалог с ошибкой загрузки
                val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
                dialogBuilder
                    .setTitle(getString(R.string.db_migration_error_title))
                    .setMessage(getString(R.string.db_migration_error_message))
                    .setPositiveButton(getString(R.string.open_settings_message)) { _: DialogInterface?, _: Int ->
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    }
                    .setNegativeButton(getString(R.string.open_app_info_message)) { _: DialogInterface?, _: Int ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNeutralButton(getString(R.string.clear_tables_message)) { _: DialogInterface?, _: Int ->
                        App.instance.mDatabase.clearAllTables()
                        Handler().postDelayed(ResetApp(), 100)
                    }
                    .setNegativeButton(getString(android.R.string.cancel)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
                val dialog = dialogBuilder.create()
                lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
                dialog.show()
                return
            }
            if (intent.data != null) {
                link = intent.data
            }
            if (PreferencesHandler.instance.isTorBlockedErrorShowed) {
                // если пользователь заходит в приложение впервые- предложу предоставить разрешение на доступ к файлам и выбрать вид
                if (!viewModel.permissionGranted()) {
                    // показываю диалог с требованием предоставить разрешения
                    showPermissionDialog()
                } else {
                    //viewModel.ping()
                    //App.instance.startTorInit()
                    // если не выбрана папка для загрузки
                    if (!PreferencesHandler.instance.downloadDirAssigned) {
                        showSelectDownloadFolderDialog()
                    }
                }
            } else {
                showTorBlockedDialog()
            }
            setupUI()

            if (PreferencesHandler.instance.isExternalVpn) {
                binding.clientProgressText.text =
                    Grammar.getColoredString(
                        getString(R.string.vpn_use),
                        Color.parseColor("#5403ad")
                    )
                binding.clientRunningProgress.visibility = View.INVISIBLE
                binding.testFlibustaIsUpText.visibility = View.VISIBLE
                binding.testFlibustaIsUpProgress.visibility = View.VISIBLE
                viewModel.ping()
            }
        }
    }

    private fun showTorBlockedDialog() {
        val adb = AlertDialog.Builder(this)
        val adbInflater = LayoutInflater.from(this)
        val eulaLayout: View = adbInflater.inflate(R.layout.dialog_tor_problems, null)
        val doNotShowAgain = eulaLayout.findViewById<View>(R.id.skip) as CheckBox
        adb.setView(eulaLayout)
        adb.setTitle(getString(R.string.attention_title))
        adb.setMessage(getString(R.string.tor_blocked_message))


        adb.setPositiveButton(
            android.R.string.ok,
            DialogInterface.OnClickListener { _, _ ->
                Log.d(
                    "surprise",
                    "MainActivity.kt 133 showTorBlockedDialog ${doNotShowAgain.isChecked}"
                )
                if (doNotShowAgain.isChecked) {
                    PreferencesHandler.instance.isTorBlockedErrorShowed = true
                }
                if (!viewModel.permissionGranted()) {
                    // показываю диалог с требованием предоставить разрешения
                    showPermissionDialog()
                } else {
                    // если не выбрана папка для загрузки
                    if (!PreferencesHandler.instance.downloadDirAssigned) {
                        showSelectDownloadFolderDialog()
                    }
                }
                // Do what you want to do on "OK" action
                return@OnClickListener
            })
        adb.setNegativeButton(
            getString(R.string.get_vpn_message),
            DialogInterface.OnClickListener { _, _ ->
                if (doNotShowAgain.isChecked) {
                    PreferencesHandler.instance.isTorBlockedErrorShowed = true
                }
                val goToMarket =
                    Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://search?q=VPN"))
                if (TransportUtils.intentCanBeHandled(goToMarket)) {
                    startActivity(goToMarket)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.no_playmarket_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // Do what you want to do on "OK" action
                return@OnClickListener
            })
        adb.show()
    }

    private fun setupUI() {
        setupInterface()
        if (PreferencesHandler.instance.hardwareAcceleration) {
            // проверю аппаратное ускорение
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        }

        binding = ActivityMainBinding.inflate(layoutInflater)

        binding.startConnectionTestBtn.setOnClickListener {
            Log.d("surprise", "MainActivity.kt 202 setupUI start connection test")
            val targetActivityIntent = Intent(this, ConnectivityGuideActivity::class.java)
            targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(targetActivityIntent)
            App.instance.stopTorInit()
            finish()
        }
        if (PreferencesHandler.instance.isEInk) {
            // prepare window for eInk
            checkWiFiEnabled()
            binding.progressBarCircle.rotation = 0F
            binding.progressBarCircle.background =
                ResourcesCompat.getDrawable(resources, R.drawable.eink_progressbar_background, null)
            binding.progressBarCircle.progressDrawable =
                ResourcesCompat.getDrawable(resources, R.drawable.eink_progressbar, null)
            binding.appVersion.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.appVersion.setShadowLayer(0F, 0F, 0F, R.color.transparent)
            binding.testStartApp.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.testStartApp.setShadowLayer(0F, 0F, 0F, R.color.transparent)

            binding.isEbook.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.isEbook.setShadowLayer(0F, 0F, 0F, R.color.transparent)

            binding.useHardwareAccelerationSwitcher.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.useHardwareAccelerationSwitcher.setShadowLayer(0F, 0F, 0F, R.color.transparent)

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
        dialogBuilder.setPositiveButton(getString(R.string.test_connection)) { _: DialogInterface?, _: Int ->
            val targetActivityIntent = Intent(this, ConnectivityGuideActivity::class.java)
            targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(targetActivityIntent)
            App.instance.stopTorInit()
            finish()
        }
        dialogBuilder.setNegativeButton(getString(R.string.wait_more_item)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
        dialogBuilder.setNeutralButton(getString(R.string.skip_inspection_item)) { _: DialogInterface?, _: Int -> launchView() }

        val dialog = dialogBuilder.create()
        lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
        dialog.show()
    }

    override fun setupObservers() {
        viewModel.flibustaServerCheckState.observe(this) {
            when (it) {
                FlibustaChecker.STATE_PASSED -> {
                    binding.testFlibustaIsUpProgress.visibility = View.INVISIBLE
                    binding.testFlibustaIsUpText.text =
                        Grammar.getColoredString(
                            getString(R.string.cant_check_flibusta_message),
                            Color.parseColor("#5403ad")
                        )
                    showFlibustaServerPassedDialog()
                }
                FlibustaChecker.STATE_AVAILABLE -> {
                    Log.d("surprise", "MainActivity.kt 293 setupObservers available")
                    binding.testFlibustaIsUpProgress.visibility = View.INVISIBLE
                    binding.testFlibustaIsUpText.text =
                        Grammar.getColoredString(
                            getString(R.string.flibusta_server_is_up),
                            Color.parseColor("#0c6126")
                        )
                    flibustaServerChecked()
                }
                FlibustaChecker.STATE_UNAVAILABLE -> {
                    binding.testFlibustaIsUpProgress.visibility = View.INVISIBLE
                    binding.testFlibustaIsUpText.text =
                        Grammar.getColoredString(
                            getString(R.string.flibusta_server_is_down),
                            Color.parseColor("#881515")
                        )
                    showFlibustaIsDownDialog()
                }
            }
        }

        val workInfoData =
            WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(App.START_TOR)
        workInfoData.observe(this) { workInfos: List<WorkInfo>? ->
            if (workInfos != null && workInfos.isNotEmpty()) {
                // переберу статусы
                val data = workInfos[0]
                when (data.state) {
                    WorkInfo.State.RUNNING -> {
                        // запускаю таймер
                        startTimer()
                    }
                    WorkInfo.State.FAILED -> {
                        showTorNotWorkDialog()
                        Log.d("surprise", "MainActivity.kt 357 setupObservers worker error")
                    }
                    WorkInfo.State.ENQUEUED -> Log.d(
                        "surprise",
                        "MainActivity.kt 357 setupObservers tor load enqueued"
                    )
                    WorkInfo.State.BLOCKED -> Log.d(
                        "surprise",
                        "MainActivity.kt 357 setupObservers tor load blocked"
                    )
                    WorkInfo.State.CANCELLED -> Log.d(
                        "surprise",
                        "MainActivity.kt 357 setupObservers tor load cancelled"
                    )
                    WorkInfo.State.SUCCEEDED -> Log.d(
                        "surprise",
                        "MainActivity.kt 357 setupObservers tor load finished"
                    )
                }
            }
        }

        TorStarter.liveTorLaunchState.observe(this) {
            if (it == TorStarter.TOR_LAUNCH_SUCCESS) {
                torLoaded()
                TorStarter.liveTorLaunchState.removeObservers(this)
            } else if (it == TorStarter.TOR_LAUNCH_FAILED) {
                showTorNotWorkDialog()
            }
        }
        // буду отслеживать проверку доступности флибусты
        viewModel.connectionTestSuccess.observe(this) {
            if (it) {
                launchView()
            }
        }
        viewModel.connectionTestFailed.observe(this) {
            if (it) {
                Log.d("surprise", "setupObservers: TOR START ERROR!!!")
                availabilityTestFailed()
            }
        }
        viewModel.torStartFailed.observe(this) {
            if (it) {
                showTorNotWorkDialog()
            }
        }

        if (!PreferencesHandler.instance.isExternalVpn) {

            // зарегистрирую отслеживание загружающегося TOR
            val loadedTor: LiveData<AndroidOnionProxyManager> = App.instance.mLoadedTor
            loadedTor.observe(this) { tor: AndroidOnionProxyManager? ->
                if (tor != null) {
                    mTor = tor
                }
            }
        }
    }

    private fun flibustaServerChecked() {
        binding.testFlibustaIsUpProgress.visibility = View.INVISIBLE
        binding.connectionTestText.visibility = View.VISIBLE
        binding.connectionTestProgress.visibility = View.VISIBLE
        viewModel.checkFlibustaAvailability()
    }

    private fun showFlibustaServerPassedDialog() {
        val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
        dialogBuilder.setTitle(getString(R.string.flibusta_server_check_passed))
            .setCancelable(false)
            .setMessage(getString(R.string.server_check_passed_message))
            .setPositiveButton(getString(R.string.try_again_message)) { _, _ ->
                binding.testFlibustaIsUpProgress.visibility = View.VISIBLE
                binding.testFlibustaIsUpText.text = getString(R.string.retry_check_message)
                viewModel.ping()
            }
            .setNegativeButton(getString(R.string.skip_inspection_item)) { _, _ ->
                flibustaServerChecked()
            }
            .show()
    }

    private fun showFlibustaIsDownDialog() {
        val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
        dialogBuilder.setTitle(getString(R.string.flibusta_server_is_down))
            .setMessage(getString(R.string.flibusta_down_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.skip_inspection_item)) { _, _ ->
                flibustaServerChecked()
            }
            .setNegativeButton(getString(R.string.try_again_message)) { _, _ ->
                binding.testFlibustaIsUpProgress.visibility = View.VISIBLE
                binding.testFlibustaIsUpText.text = getString(R.string.retry_check_message)
                viewModel.ping()
            }
            .setNeutralButton(getString(R.string.start_checker_message)) { _, _ ->
                App.instance.startCheckWorker()
                Toast.makeText(
                    this,
                    getString(R.string.checker_promise),
                    Toast.LENGTH_LONG
                ).show()
                val startMain = Intent(Intent.ACTION_MAIN)
                startMain.addCategory(Intent.CATEGORY_HOME)
                startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(startMain)
            }
            .show()
    }

    private fun torLoaded() {
        binding.clientProgressText.text = Grammar.getColoredString(
            getString(R.string.tor_loaded),
            Color.parseColor("#0c6126")
        )
        binding.clientRunningProgress.visibility = View.INVISIBLE
        binding.testFlibustaIsUpText.visibility = View.VISIBLE
        binding.testFlibustaIsUpProgress.visibility = View.VISIBLE
        viewModel.ping()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_WRITE_READ && grantResults.isNotEmpty()) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog()
            } else {
                viewModel.ping()
                // проверю, выбрана ли папка
                if (!PreferencesHandler.instance.downloadDirAssigned) {
                    showSelectDownloadFolderDialog()
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // запущу таймер отсчёта
    private fun startTimer() {
        mProgressCounter = 0
        val waitingTime = TOR_LOAD_MAX_TIME * 1000 // 3 minute in milli seconds
        val checkAvailabilityLimit: Int = if (PreferencesHandler.instance.isEInk) {
            90
        } else {
            60
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
                                            previousText = last
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onFinish() {
                    // tor не загрузился, покажу сообщение с предложением подождать или перезапустить процесс
                    torLoadTooLongDialog()
                }
            }
            mCdt?.start()
        }
    }

    private fun torLoadTooLongDialog() {
        if (!this@MainActivity.isFinishing) {
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle(getString(R.string.tor_load_long_message))
                .setMessage(getString(R.string.wait_or_restart_message))
                .setPositiveButton(getString(R.string.reset_title)) { _: DialogInterface?, _: Int ->
                    // reset app
                    Handler().postDelayed(ResetApp(), 100)
                }
                .setNegativeButton(getString(R.string.wait_more_message)) { _: DialogInterface?, _: Int -> startTimer() }

            val dialog = dialogBuilder.create()
            lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
            dialog.show()
        } else {
            mTorLoadTooLong = true
        }
    }

    private fun stopTimer() {
        mProgressCounter = 0
        binding.progressBarCircle.progress = 0
        mCdt?.cancel()
    }

    private fun showTorNotWorkDialog() {
        Log.d("surprise", "MainActivity.kt 607 showTorNotWorkDialog show tor not work")
        binding.clientRunningProgress.visibility = View.INVISIBLE
        binding.clientProgressText.text = Grammar.getColoredString(
            getString(R.string.failed_message),
            Color.parseColor("#881515")
        )
        stopTimer()
        if (!this@MainActivity.isFinishing) {
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle(getString(R.string.tor_cant_load_message))
                .setMessage(getString(R.string.tor_not_start_body))
                .setPositiveButton(getString(R.string.try_again_message)) { _: DialogInterface?, _: Int ->
                    Handler().postDelayed(ResetApp(), 100)
                }
                .setNegativeButton(getString(R.string.set_tor_custom_bridges)) { _: DialogInterface?, _: Int -> showSetCustomBridgesDialog() }
                .setNeutralButton(getString(R.string.start_connection_test_btn)) { _: DialogInterface?, _: Int ->
                    val targetActivityIntent = Intent(this, ConnectivityGuideActivity::class.java)
                    targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(targetActivityIntent)
                    App.instance.stopTorInit()
                    finish()
                }

            val dialog = dialogBuilder.create()
            lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
            dialog.show()
        }
    }

    private fun showSetCustomBridgesDialog() {
        val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
        val view = layoutInflater.inflate(R.layout.dialog_tor_add_bridges, null, false)
        val showBridgesBtn = view.findViewById<Button>(R.id.button)
        showBridgesBtn.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://bridges.torproject.org/bridges?transport=obfs4")
            )
            startActivity(browserIntent)
        }
        dialogBuilder.setTitle(getString(R.string.set_tor_custom_bridges))
            .setView(view)
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
                Toast.makeText(
                    this,
                    getString(R.string.alternate_options_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                // save new bridges, try to start TOR.
                // If it starts, save bridges to cloud
                val bridgesInput = view.findViewById<EditText>(R.id.customBridgesInput)
                val bridgesValue = bridgesInput.text
                if (!bridgesValue.isNullOrEmpty()) {
                    App.instance.isCustomBridgesSet = true
                    PreferencesHandler.instance.setCustomBridges(bridgesValue.toString())
                    FilesHandler.saveBridges(bridgesValue.toString())
                    Toast.makeText(
                        this,
                        getString(R.string.restart_tor_with_new_bridges_message),
                        Toast.LENGTH_SHORT
                    ).show()
                    stopTimer()
                    startTimer()
                    App.instance.startTorInit()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.alternate_options_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
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
                    Handler().postDelayed(ResetApp(), 100)

                }
            val dialog = dialogBuilder.create()
            lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
            dialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mTorLoadTooLong) {
            mTorLoadTooLong = false
            torLoadTooLongDialog()
        }
        if (viewModel.connectionTestSuccess.value == true) {
            launchView()
        }
    }

    private fun availabilityTestFailed() {
        // запущу активити, которое напишет, что флибуста недоступна и предложит попробовать позже или закрыть приложение
        startActivity(Intent(this@MainActivity, FlibustaNotAvailableActivity::class.java))
        finish()
    }

    private fun launchView() {
        viewModel.cancelCheck()
        binding.connectionTestProgress.visibility = View.INVISIBLE
        binding.connectionTestText.text = Grammar.getColoredString(
            getString(R.string.connected_message),
            Color.parseColor("#0c6126")
        )
        Handler().postDelayed({
            // проверю очередь скачивания. Если она не пуста- предложу продолжить закачку
            // проверю, не запущено ли приложение с помощью интента. Если да- запущу программу в webView режиме
            val targetActivityIntent = Intent(this, BrowserActivity::class.java)
            if (link != null) {
                targetActivityIntent.putExtra(BrowserActivity.EXTERNAL_LINK, link.toString())
            }
            targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(targetActivityIntent)
            finish()
        }, 500)
    }

    override fun onDestroy() {
        super.onDestroy()
        mCdt?.cancel()
        if (this::viewModel.isInitialized) {
            viewModel.connectionTestSuccess.removeObservers(this)
        }
    }

    companion object {
        private const val REQUEST_WRITE_READ = 22
        private const val TOR_LOAD_MAX_TIME = 260
        private var mProgressCounter = 0
    }
}
