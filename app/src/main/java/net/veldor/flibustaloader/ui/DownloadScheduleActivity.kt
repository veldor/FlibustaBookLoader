package net.veldor.flibustaloader.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.DownloadScheduleAdapter
import net.veldor.flibustaloader.databinding.ActivityDownloadScheduleBinding
import net.veldor.flibustaloader.delegates.DownloadWorkSwitchStateDelegate
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.utils.Grammar
import net.veldor.flibustaloader.view_models.DownloadScheduleViewModel
import net.veldor.flibustaloader.view_models.OPDSViewModel
import net.veldor.flibustaloader.workers.DownloadBooksWorker
import java.util.*

class DownloadScheduleActivity : BaseActivity(), DownloadWorkSwitchStateDelegate {
    private lateinit var binding: ActivityDownloadScheduleBinding
    private lateinit var viewModel: DownloadScheduleViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DownloadScheduleViewModel::class.java)
        binding = ActivityDownloadScheduleBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupInterface()
        setupObservers()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun setupObservers() {
        super.setupObservers()
        DownloadScheduleViewModel.schedule.observe(this, {
            (binding.resultsList.adapter as DownloadScheduleAdapter).setData(it)
            binding.swipeLayout.isRefreshing = false
        })

        DownloadScheduleViewModel.liveCurrentBookDownloadProgress.observe(this, {
            (binding.resultsList.adapter as DownloadScheduleAdapter).setDownloadProgressChanged(it)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.bookLoadProgressBar.setProgress(it.percentDone.toInt(), true)
            }
            else{
                binding.bookLoadProgressBar.setProgress(it.percentDone.toInt())
            }
            binding.bookLoadProgressText.text = String.format(Locale.ENGLISH, "%s/%s", Grammar.humanReadableByteCountBin(it.loadedSize), Grammar.humanReadableByteCountBin(it.fullSize))
            binding.bookLoadProgressBar.visibility = View.VISIBLE
            binding.bookLoadProgressText.visibility = View.VISIBLE
        })
        DownloadScheduleViewModel.liveFullBookDownloadProgress.observe(this, {
            binding.bookTotalProgressText.text = String.format(Locale.ENGLISH, "%d/%d", it.loaded, it.total)
            binding.fullLoadProgressBar.max = it.total
            binding.fullLoadProgressBar.progress = it.loaded
            binding.fullLoadProgressBar.visibility = View.VISIBLE
            binding.bookTotalProgressText.visibility = View.VISIBLE
        })

        App.instance.liveDownloadState.observe(this, {
            if (it == DownloadBooksWorker.DOWNLOAD_FINISHED) {
                binding.fullLoadProgressBar.visibility = View.GONE
                binding.bookLoadProgressBar.visibility = View.GONE
                binding.bookLoadProgressText.visibility = View.GONE
                binding.bookTotalProgressText.visibility = View.GONE
                viewModel.loadDownloadQueue()
                binding.actionButton.text = getString(R.string.start_download)
                (binding.resultsList.adapter as DownloadScheduleAdapter).notifyDataSetChanged()
                binding.dropDownloadQueueBtn.visibility = View.GONE
            } else if (it == DownloadBooksWorker.DOWNLOAD_IN_PROGRESS) {
                binding.fullLoadProgressBar.visibility = View.VISIBLE
                binding.bookLoadProgressBar.visibility = View.VISIBLE
                binding.bookLoadProgressText.visibility = View.VISIBLE
                binding.bookTotalProgressText.visibility = View.VISIBLE
                (binding.resultsList.adapter as DownloadScheduleAdapter).notifyDataSetChanged()
                binding.actionButton.text = getString(R.string.stop_download_message)
                binding.dropDownloadQueueBtn.visibility = View.VISIBLE
            }
        })

        App.instance.liveBookJustLoaded.observe(this, {
            binding.bookLoadProgressBar.progress = 0
            binding.bookLoadProgressText.text = "0/0"
            (binding.resultsList.adapter as DownloadScheduleAdapter).notifyBookDownloaded(it)
        })
        App.instance.liveBookJustRemovedFromQueue.observe(this, {
            (binding.resultsList.adapter as DownloadScheduleAdapter).notifyBookRemovedFromQueue(it)
        })
        App.instance.liveBookJustError.observe(this, {
            (binding.resultsList.adapter as DownloadScheduleAdapter).notifyBookDownloadError(it)
        })
        App.instance.liveBookDownloadInProgress.observe(this, {
            binding.bookLoadProgressBar.progress = 0
            binding.bookLoadProgressText.text = "0/0"
            (binding.resultsList.adapter as DownloadScheduleAdapter).notifyBookDownloadInProgress(it)
        })

    }

    override fun setupInterface() {
        super.setupInterface()

        binding.dropDownloadQueueBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Отмена очереди скачивания")
                .setMessage("Отменить скачивание и удалить все книги из очереди скачивания?")
                .setPositiveButton("Да") { _, _ ->
                    NotificationHandler.instance.hideMassDownloadInQueueMessage()
                    DownloadBooksWorker.dropDownloadsQueue()
                    WorkManager.getInstance(App.instance)
                        .cancelAllWorkByTag(OPDSViewModel.MULTIPLY_DOWNLOAD)
                    // отменяю работу и очищу очередь скачивания
                    NotificationHandler.instance.cancelBookLoadNotification()
                    App.instance.liveDownloadState.postValue(DownloadBooksWorker.DOWNLOAD_FINISHED)
                    Toast.makeText(
                        App.instance,
                        "Скачивание книг отменено и очередь скачивания очищена!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .setNegativeButton("Нет", null)
                .show()
        }

        binding.resultsList.adapter = DownloadScheduleAdapter(arrayListOf())
        binding.resultsList.layoutManager = LinearLayoutManager(this)
        binding.actionButton.setOnClickListener {
            Log.d("surprise", "setupInterface: button clicked!")
            App.instance.switchDownloadState(this)
        }
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToDownloadsList)
        item.isEnabled = false
        item.isChecked = true
        // активирую кнопку возвращения к предыдущему окну
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        // проверю, есть ли книги в очереди скачивания
        val queueSize = App.instance.mDatabase.booksDownloadScheduleDao().queueSize
        if (queueSize == 0) {
            Toast.makeText(this, R.string.download_schedule_empty_message, Toast.LENGTH_LONG).show()
            finish()
        }

        binding.swipeLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
        binding.swipeLayout.setOnRefreshListener {
            viewModel.loadDownloadQueue()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDownloadQueue()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this, BrowserActivity::class.java)
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

    override fun stateSwitched(state: Int) {
        viewModel.loadDownloadQueue()
        (binding.resultsList.adapter as DownloadScheduleAdapter).notifyDataSetChanged()
        if (state == 1) {
            binding.actionButton.text = getString(R.string.start_download)
        } else {
            binding.actionButton.text = getString(R.string.stop_download_message)
        }
    }
}