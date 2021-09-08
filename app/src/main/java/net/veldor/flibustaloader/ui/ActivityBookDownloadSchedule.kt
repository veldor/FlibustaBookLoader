package net.veldor.flibustaloader.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkInfo
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.DownloadScheduleAdapter
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.view_models.MainViewModel
import net.veldor.flibustaloader.workers.DownloadBooksWorker.Companion.downloadProgress
import net.veldor.flibustaloader.workers.DownloadBooksWorker.Companion.noActiveDownloadProcess

class ActivityBookDownloadSchedule : BaseActivity() {
    private lateinit var myViewModel: MainViewModel
    private lateinit var booksAdapter: DownloadScheduleAdapter
    private lateinit var stopDownloadBtn: View
    private lateinit var continueDownloadBtn: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_download_schedule_activity)
        setupInterface()
        myViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        observeChanges()
    }

    private fun observeChanges() {
        // получу состояние загрузки
        val loadProgress: LiveData<List<WorkInfo>> = downloadProgress
        loadProgress.observe(this, { workInfos: List<WorkInfo?>? ->
            if (workInfos != null && workInfos.size > 0) {
                // получу статус закачки
                val work = workInfos[0]
                if (work != null) {
                    when (work.state) {
                        WorkInfo.State.CANCELLED, WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.BLOCKED -> showContinue()
                        WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> showStop()
                    }
                }
            }
        })
    }

    private fun showContinue() {
        stopDownloadBtn.visibility = View.GONE
        continueDownloadBtn.visibility = View.VISIBLE
    }

    private fun showStop() {
        stopDownloadBtn.visibility = View.VISIBLE
        continueDownloadBtn.visibility = View.GONE
    }

    override fun setupInterface() {
        super.setupInterface()

        // скрою переход на данное активити
        val menuNav = mNavigationView!!.menu
        val item = menuNav.findItem(R.id.goToDownloadsList)
        item.isEnabled = false
        item.isChecked = true
        val statusContainer = App.instance.mDownloadAllWork

        // проверю, есть ли активный процесс загрузки
        val noActiveDownload = noActiveDownloadProcess()

        // активирую кнопку возвращения к предыдущему окну
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        // проверю, есть ли книги в очереди скачивания
        val queueSize = App.instance.mDatabase.booksDownloadScheduleDao().queueSize
        if (queueSize == 0) {
            Toast.makeText(this, R.string.download_schedule_empty_message, Toast.LENGTH_LONG).show()
            finish()
        }

        // получу данные о книгах в очереди в виде liveData
        val schedule = App.instance.mDatabase.booksDownloadScheduleDao().allBooksLive
        schedule!!.observe(this, { booksDownloadSchedules: List<BooksDownloadSchedule> ->
            if (booksDownloadSchedules.isNotEmpty()) {
                booksAdapter.setData(booksDownloadSchedules)
                booksAdapter.notifyDataSetChanged()
            } else {
                finish()
            }
        })


        // найду кнопку остановки скачивания
        stopDownloadBtn = findViewById(R.id.stopMassDownload)
        continueDownloadBtn = findViewById(R.id.continueMassDownload)
        if (statusContainer != null && statusContainer.value != null && statusContainer.value!!
                .state == WorkInfo.State.RUNNING
        ) {
            stopDownloadBtn.visibility = View.GONE
            continueDownloadBtn.visibility = View.VISIBLE
        }
        stopDownloadBtn.setOnClickListener {
            Log.d("surprise", "ActivityBookDownloadSchedule onClick: work cancelled")
            myViewModel.cancelMassDownload()
            NotificationHandler.instance.cancelBookLoadNotification()
            NotificationHandler.instance.createMassDownloadStoppedNotification()
            stopDownloadBtn.visibility = View.GONE
            Toast.makeText(
                this@ActivityBookDownloadSchedule,
                "Загрузка книг остановлена",
                Toast.LENGTH_LONG
            ).show()
            continueDownloadBtn.visibility = View.VISIBLE
        }
        continueDownloadBtn.setOnClickListener {
            myViewModel.initiateMassDownload()
            continueDownloadBtn.visibility = View.GONE
            stopDownloadBtn.visibility = View.VISIBLE
        }
        if (noActiveDownload) {
            continueDownloadBtn.visibility = View.VISIBLE
            stopDownloadBtn.visibility = View.GONE
        } else {
            continueDownloadBtn.visibility = View.GONE
            stopDownloadBtn.visibility = View.VISIBLE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this, OPDSActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}