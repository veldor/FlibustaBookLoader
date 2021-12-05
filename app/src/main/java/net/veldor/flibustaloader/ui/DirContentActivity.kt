package net.veldor.flibustaloader.ui

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.DownloadDirContentAdapter
import net.veldor.flibustaloader.databinding.ActivityShowDownloadFolderContentBinding
import net.veldor.flibustaloader.selections.*
import net.veldor.flibustaloader.utils.BooksDataSource
import net.veldor.flibustaloader.utils.BooksDiffUtilCallback
import net.veldor.flibustaloader.utils.FilesHandler.openFile
import net.veldor.flibustaloader.utils.FilesHandler.shareFile
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.view_models.DirContentViewModel
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class DirContentActivity : BaseActivity() {
    private lateinit var viewModel: DirContentViewModel

    private lateinit var binding: ActivityShowDownloadFolderContentBinding
    private lateinit var recycler: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DirContentViewModel::class.java)
        binding = ActivityShowDownloadFolderContentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        rootView = binding.root
        setupInterface()
        // получу recyclerView
        recycler = findViewById(R.id.showDirContent)
        recycler.layoutManager = LinearLayoutManager(this)
        // получу список файлов из папки
        viewModel.loadDirContent()
    }

    override fun setupInterface() {
        super.setupInterface()
        if (PreferencesHandler.instance.isEInk) {
            paintToolbar(binding.toolbar)
        }
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToFileList)
        item.isEnabled = false
        item.isChecked = true

        viewModel.liveFilesLoaded.observe(this, {
            binding.waiter.visibility = View.GONE
            val dataSource = BooksDataSource(it)
            val config = PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(20)
                .build()
            val pageList: PagedList<Book?> = PagedList.Builder(dataSource, config)
                .setFetchExecutor(Executors.newSingleThreadExecutor())
                .setNotifyExecutor(MainThreadExecutor())
                .build()
            val adapter = DownloadDirContentAdapter(BooksDiffUtilCallback())
            adapter.submitList(pageList)
            binding.showDirContent.adapter = adapter
        })
    }


    override fun onContextItemSelected(item: MenuItem): Boolean {
        Log.d(
            "surprise",
            "ShowDownloadFolderContentActivity onContextItemSelected 87: handle " + item.title
        )
        val position: Int
        try {
            val adapter = recycler.adapter as DownloadDirContentAdapter?
            if (adapter != null) {
                position = adapter.contexItemPosition
                val book = adapter.requireItem(position)
                if (book != null) {
                    if (item.title == getString(R.string.share_link_message)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            shareFile(book.file!!)
                        }
                    } else if (item.title == getString(R.string.open_with_menu_item)) {
                        Log.d(
                            "surprise",
                            "ShowDownloadFolderContentActivity onContextItemSelected 105: open file"
                        )
                        openFile(book.file!!)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return super.onContextItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.loaded_books_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_sort_by) {
            selectSorting()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun selectSorting() {
        val dialog = AlertDialog.Builder(this, R.style.MyDialogStyle)
        dialog.setTitle("Выберите тип сортировки")
            .setItems(bookSortOptions) { _: DialogInterface?, which: Int ->
                binding.waiter.visibility = View.VISIBLE
                viewModel.loadDirContent(which)
            }
        // покажу список типов сортировки
        if (!this@DirContentActivity.isFinishing) {
            dialog.show()
        }
    }

    companion object {
        private val bookSortOptions = arrayOf(
            "По названию книги",
            "По размеру",
            "По автору",
            "По формату",
            "По дате загрузки"
        )
    }

    internal class MainThreadExecutor : Executor {
        private val handler: Handler = Handler(Looper.getMainLooper())
        override fun execute(command: Runnable) {
            handler.post(command)
        }
    }
}