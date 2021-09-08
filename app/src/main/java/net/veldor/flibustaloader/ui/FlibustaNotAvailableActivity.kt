package net.veldor.flibustaloader.ui

import android.os.Bundle
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.App
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.app.PendingIntent
import android.app.AlarmManager
import android.view.*
import android.widget.*
import net.veldor.flibustaloader.utils.PreferencesHandler

class FlibustaNotAvailableActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flibusta_not_available)
        val switchToVpnBtn = findViewById<Button>(R.id.switchToVpnBtn)
        if (PreferencesHandler.instance.isExternalVpn) {
            switchToVpnBtn.visibility = View.GONE
        } else {
            switchToVpnBtn.setOnClickListener { view: View? ->
                PreferencesHandler.instance.isExternalVpn = !PreferencesHandler.instance.isExternalVpn
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
                System.exit(0)
            }
        }
        val closeAppBtn = findViewById<Button>(R.id.closeAppButton)
        closeAppBtn.setOnClickListener { v: View? -> System.exit(0) }
        val tryAgainBtn = findViewById<Button>(R.id.retryButton)
        tryAgainBtn.setOnClickListener { v: View? ->
            startActivity(Intent(this@FlibustaNotAvailableActivity, MainActivity::class.java))
            finish()
        }
        val startCheckWorker = findViewById<Button>(R.id.startCheckerBtn)
        startCheckWorker.setOnClickListener { v: View? ->
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
        val disableInspectionButton = findViewById<Button>(R.id.disableInspectionButton)
        disableInspectionButton.setOnClickListener { v: View? ->
            PreferencesHandler.instance.setInspectionEnabled(false)
            Toast.makeText(
                this@FlibustaNotAvailableActivity,
                getString(R.string.inspection_disabled_message),
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(this@FlibustaNotAvailableActivity, MainActivity::class.java))
            finish()
        }
        val showMainWindowButton = findViewById<Button>(R.id.showMainWindowButton)
        showMainWindowButton.setOnClickListener { v: View? ->
            // запущу главное окно
            startView()
            finish()
        }
    }

    private fun startView() {
        // проверю очередь скачивания. Если она не пуста- предложу продолжить закачку
        // проверю, не запущено ли приложение с помощью интента. Если да- запущу программу в webView режиме
        val targetActivityIntent: Intent
        // проверю, если используем ODPS- перенаправлю в другую активность
        targetActivityIntent = if (App.instance.view == App.VIEW_ODPS) {
            Intent(this, OPDSActivity::class.java)
        } else {
            Intent(this, WebViewActivity::class.java)
        }
        targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(targetActivityIntent)
        finish()
    }
}