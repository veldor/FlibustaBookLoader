package net.veldor.flibustaloader.ui

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.ActivityFlibustaNotAvailableBinding
import net.veldor.flibustaloader.utils.PreferencesHandler
import kotlin.system.exitProcess

class FlibustaNotAvailableActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFlibustaNotAvailableBinding

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlibustaNotAvailableBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (PreferencesHandler.instance.isExternalVpn) {
            binding.switchToVpnBtn.visibility = View.GONE
            binding.vpnWarning.visibility = View.VISIBLE
        } else {
            binding.switchToVpnBtn.setOnClickListener {
                PreferencesHandler.instance.isExternalVpn =
                    !PreferencesHandler.instance.isExternalVpn
                val mStartActivity =
                    Intent(this@FlibustaNotAvailableActivity, MainActivity::class.java)
                val mPendingIntentId = 123456
                val mPendingIntent = PendingIntent.getActivity(
                    this@FlibustaNotAvailableActivity,
                    mPendingIntentId,
                    mStartActivity,
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
                val mgr =
                    this@FlibustaNotAvailableActivity.getSystemService(ALARM_SERVICE) as AlarmManager
                mgr[AlarmManager.RTC, System.currentTimeMillis() + 100] = mPendingIntent
                exitProcess(0)
            }
        }
        binding.closeAppButton.setOnClickListener { exitProcess(0) }
        binding.retryButton.setOnClickListener {
            startActivity(Intent(this@FlibustaNotAvailableActivity, MainActivity::class.java))
            finish()
        }
        binding.startCheckerBtn.setOnClickListener {
            App.instance.startCheckWorker()
            Toast.makeText(
                this@FlibustaNotAvailableActivity,
                "Вы получите уведомление, когда сервер Флибусты вернётся",
                Toast.LENGTH_LONG
            ).show()
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
        }
        binding.disableInspectionButton.setOnClickListener {
            PreferencesHandler.instance.setInspectionEnabled(false)
            Toast.makeText(
                this@FlibustaNotAvailableActivity,
                getString(R.string.inspection_disabled_message),
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(this@FlibustaNotAvailableActivity, MainActivity::class.java))
            finish()
        }
        binding.showMainWindowButton.setOnClickListener {
            // запущу главное окно
            startView()
            finish()
        }
        binding.getVpn.setOnClickListener {
            val goToMarket =
                Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://search?q=VPN"))
            startActivity(goToMarket)
        }
    }

    private fun startView() {
        // проверю очередь скачивания. Если она не пуста- предложу продолжить закачку
        // проверю, не запущено ли приложение с помощью интента. Если да- запущу программу в webView режиме
        // проверю, если используем OPDS- перенаправлю в другую активность
        val targetActivityIntent = Intent(this, BrowserActivity::class.java)
        targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(targetActivityIntent)
        finish()
    }
}