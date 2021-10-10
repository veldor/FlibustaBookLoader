package net.veldor.flibustaloader.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.DownloadScheduleAdapter
import net.veldor.flibustaloader.databinding.ActivityDownloadScheduleBinding
import net.veldor.flibustaloader.view_models.DownloadScheduleViewModel
import net.veldor.flibustaloader.workers.DownloadBooksWorker

class DownloadScheduleActivity : BaseActivity() {
    private lateinit var binding: ActivityDownloadScheduleBinding
    private lateinit var viewModel: DownloadScheduleViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DownloadScheduleViewModel::class.java)
        viewModel.loadDownloadQueue()
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
        })

        DownloadScheduleViewModel.liveCurrentBookDownloadProgress.observe(this, {
            Log.d("surprise", "setupObservers: have download progress")
            (binding.resultsList.adapter as DownloadScheduleAdapter).setDownloadProgressChanged(it)
        })

        App.instance.liveDownloadState.observe(this, {
            Log.d("surprise", "setupObservers: state changed on $it")
            if(it == DownloadBooksWorker.DOWNLOAD_FINISHED){
                binding.actionButton.text = getString(R.string.start_download)
                (binding.resultsList.adapter as DownloadScheduleAdapter).notifyDataSetChanged()
            }
            else if(it == DownloadBooksWorker.DOWNLOAD_IN_PROGRESS){
                (binding.resultsList.adapter as DownloadScheduleAdapter).notifyDataSetChanged()
                binding.actionButton.text = getString(R.string.stop_download_message)
            }
        })

        App.instance.liveBookJustLoaded.observe(this, {
            (binding.resultsList.adapter as DownloadScheduleAdapter).notifyBookDownloaded(it)
        })
        App.instance.liveBookJustRemovedFromQueue.observe(this, {
            (binding.resultsList.adapter as DownloadScheduleAdapter).notifyBookRemovedFromQueue(it)
        })
        App.instance.liveBookJustError.observe(this, {
            (binding.resultsList.adapter as DownloadScheduleAdapter).notifyBookDownloadError(it)
        })
        App.instance.liveBookDownloadInProgress.observe(this, {
            (binding.resultsList.adapter as DownloadScheduleAdapter).notifyBookDownloadInProgress(it)
        })

    }

    override fun setupInterface() {
        super.setupInterface()
        binding.resultsList.adapter = DownloadScheduleAdapter(arrayListOf())
        binding.resultsList.layoutManager = LinearLayoutManager(this)
        binding.actionButton.setOnClickListener {
            App.instance.switchDownloadState()
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
    }

    override fun onResume() {
        super.onResume()
        (binding.resultsList.adapter as DownloadScheduleAdapter).setData(DownloadScheduleViewModel.schedule.value!!)
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
}