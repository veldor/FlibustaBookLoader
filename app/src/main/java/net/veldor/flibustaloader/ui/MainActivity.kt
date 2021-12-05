package net.veldor.flibustaloader.ui

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.constraintlayout.widget.ConstraintLayout
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
import net.veldor.flibustaloader.utils.Grammar.appVersion
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.view_models.StartViewModel
import java.util.*
import kotlin.system.exitProcess

class MainActivity : BaseActivity() {
    private var previousText: String = ""
    private lateinit var binding: ActivityMainBinding
    private var link: Uri? = null
    private var mProgressCounter = 0
    private var flibustaCheckCounter = 0
    private var mCdt: CountDownTimer? = null
    private var mTor: AndroidOnionProxyManager? = null
    private var mTorLoadTooLong = false
    private lateinit var viewModel: StartViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(StartViewModel::class.java)
        if(App.instance.migrationError){
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
            Log.d("surprise", "onCreate: external link here")
            link = intent.data
        }
        setupObservers()
        // если пользователь заходит в приложение впервые- предложу предоставить разрешение на доступ к файлам и выбрать вид
        if (!viewModel.permissionGranted()) {
            // показываю диалог с требованием предоставить разрешения
            showPermissionDialog()
        } else {
            App.instance.startTorInit()
            // если не выбрана папка для загрузки
            if (!PreferencesHandler.instance.downloadDirAssigned) {
                showSelectDownloadFolderDialog()
            }
        }
        // проверю на пропуск главного экрана
        if (PreferencesHandler.instance.isSkipMainScreen() || PreferencesHandler.instance.isExternalVpn) {
            launchView()
            finish()
            return
        }
        setupUI()
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
        if (PreferencesHandler.instance.isEInk) {
            // prepare window for eInk
            checkWiFiEnabled()
            binding.progressBarCircle.rotation = 0F
            binding.progressBarCircle.background =
                ResourcesCompat.getDrawable(resources, R.drawable.eink_progressbar_background, null)
            binding.progressBarCircle.progressDrawable =
                ResourcesCompat.getDrawable(resources, R.drawable.eink_progressbar, null)
            binding.stateTextView?.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.stateTextView?.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.stateTextView?.setShadowLayer(0F, 0F, 0F, R.color.transparent)
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

            binding.statusFirstValue?.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.statusFirstValue?.setShadowLayer(0F, 0F, 0F, R.color.transparent)

            binding.statusSecondValue?.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.statusSecondValue?.setShadowLayer(0F, 0F, 0F, R.color.transparent)

            binding.stateTextView?.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.stateTextView?.setShadowLayer(0F, 0F, 0F, R.color.transparent)


            binding.statusWrapper.setInAnimation(this, android.R.anim.fade_in)
            binding.statusWrapper.setOutAnimation(this, android.R.anim.fade_out)
            val params = binding.statusWrapper.layoutParams as ConstraintLayout.LayoutParams
            params.setMargins(0,30,0,30)
            binding.statusWrapper.layoutParams = params

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

    override fun setupObservers() {

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
                    }
                    WorkInfo.State.RUNNING -> {
                        // запускаю таймер
                        startTimer()
                        binding.statusWrapper.setText(
                            getString(R.string.launch_begin_message)
                        )
                    }
                    WorkInfo.State.FAILED -> showTorNotWorkDialog()
                }
            }
        })

        TorStarter.liveTorLaunchState.observe(this, {
            if (it == TorStarter.TOR_LAUNCH_SUCCESS) {
                torLoaded()
                TorStarter.liveTorLaunchState.removeObservers(this)
            } else if (it == TorStarter.TOR_LAUNCH_FAILED) {
                showTorNotWorkDialog()
            }
        })
        // буду отслеживать проверку доступности флибусты
        viewModel.connectionTestSuccess.observe(this, {
            if (it) {
                launchView()
            }
        })
        viewModel.connectionTestFailed.observe(this, {
            if (it) {
                Log.d("surprise", "setupObservers: TOR START ERROR!!!")
                availabilityTestFailed()
            }
        })
        viewModel.torStartFailed.observe(this, {
            if (it) {
                showTorNotWorkDialog()
            }
        })

        if (!PreferencesHandler.instance.isExternalVpn) {

            // зарегистрирую отслеживание загружающегося TOR
            val loadedTor: LiveData<AndroidOnionProxyManager> = App.instance.mLoadedTor
            loadedTor.observe(this, { tor: AndroidOnionProxyManager? ->
                if (tor != null) {
                    mTor = tor
                }
            })
            // получу данные о работе
//            val workInfoData =
//                WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(App.START_TOR)
//            workInfoData.observe(this, { workInfos: List<WorkInfo>? ->
//                if (workInfos != null && workInfos.isNotEmpty()) {
//                    // переберу статусы
//                    val data = workInfos[0]
//                    when (data.state) {
//                        WorkInfo.State.ENQUEUED -> {
//                            binding.statusWrapper.setText(
//                                getString(R.string.tor_load_waiting_internet_message)
//                            )
//                            stopTimer()
//                        }
//                        WorkInfo.State.RUNNING -> {
//                            // запускаю таймер
//                            startTimer()
//                            binding.statusWrapper.setText(
//                                getString(R.string.launch_begin_message)
//                            )
//                        }
//                        WorkInfo.State.CANCELLED -> {
//                            stopTimer()
//                            binding.statusWrapper.setText(
//                                getString(R.string.launch_cancelled_message)
//                            )
//                        }
//                        WorkInfo.State.FAILED -> showTorNotWorkDialog()
//                    }
//                }
//            })
        }
    }

    private fun torLoaded() {
        if (PreferencesHandler.instance.isCheckAvailability()) {
            checkFlibustaAvailability()
        } else {
            launchView()
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
                App.instance.startTorInit()
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
        stopTimer()
        if (!this@MainActivity.isFinishing) {
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle(getString(R.string.tor_cant_load_message))
                .setMessage(getString(R.string.tor_not_start_body))
                .setPositiveButton(getString(R.string.try_again_message)) { _: DialogInterface?, _: Int ->
                    Handler().postDelayed(ResetApp(), 100)
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
        // проверю очередь скачивания. Если она не пуста- предложу продолжить закачку
        // проверю, не запущено ли приложение с помощью интента. Если да- запущу программу в webView режиме
        val targetActivityIntent = Intent(this, BrowserActivity::class.java)
        if (link != null) {
            Log.d("surprise", "launchView: send link")
            targetActivityIntent.putExtra(BrowserActivity.EXTERNAL_LINK, link.toString())
        }
        targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(targetActivityIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCdt?.cancel()
        viewModel.connectionTestSuccess.removeObservers(this)
    }

    companion object {
        private const val REQUEST_WRITE_READ = 22
        private const val TOR_LOAD_MAX_TIME = 260
    }
}
