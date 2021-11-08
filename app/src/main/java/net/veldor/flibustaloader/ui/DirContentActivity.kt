package net.veldor.flibustaloader.ui

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.DirContentAdapter
import net.veldor.flibustaloader.databinding.ActivityShowDownloadFolderContentBinding
import net.veldor.flibustaloader.selections.*
import net.veldor.flibustaloader.utils.FilesHandler.openFile
import net.veldor.flibustaloader.utils.FilesHandler.shareFile
import net.veldor.flibustaloader.utils.Grammar.getExtension
import net.veldor.flibustaloader.utils.Grammar.getLiteralSize
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.io.File
import java.util.*

class DirContentActivity : BaseActivity() {
    private lateinit var binding: ActivityShowDownloadFolderContentBinding
    private lateinit var recycler: RecyclerView
    private var adapter: DirContentAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowDownloadFolderContentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInterface()
        // получу recyclerView
        recycler = findViewById(R.id.showDirContent)
        recycler.layoutManager = LinearLayoutManager(this)
        // получу список файлов из папки
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val downloadsDir = PreferencesHandler.instance.getDownloadDir()
            if (downloadsDir != null && downloadsDir.isDirectory) {
                val files = downloadsDir.listFiles()
                val books = recursiveScan(files, "")
                Log.d(
                    "surprise",
                    "ShowDownloadFolderContentActivity onCreate 37: books size is " + books.size
                )
                if (books.size > 0) {
                    adapter = DirContentAdapter(books)
                    recycler.adapter = adapter
                }
            }
        } else {
            val downloadDir = PreferencesHandler.instance.getDownloadDir()
            if (downloadDir != null && downloadDir.isDirectory) {
                val files = downloadDir.listFiles()
                val books = recursiveScan(files, "")
                Log.d(
                    "surprise",
                    "ShowDownloadFolderContentActivity onCreate 57: len is " + books.size
                )
                if (books.size > 0) {
                    adapter = DirContentAdapter(books)
                    recycler.adapter = adapter
                }
            }
        }
    }

    override fun setupInterface() {
        super.setupInterface()
        paintToolbar(binding.toolbar)
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToFileList)
        item.isEnabled = false
        item.isChecked = true
    }

    private fun recursiveScan(files: Array<DocumentFile>, prefix: String): ArrayList<Book> {
        val answer = ArrayList<Book>()
        if (files.isNotEmpty()) {
            var bookItem: Book
            var value: String
            var value1: String
            for (df in files) {
                if (df.isFile) {
                    value = df.name!!
                    bookItem = Book()
                    // получу имя автора- это значение до первого слеша
                    val index = value.indexOf("_")
                    val lastIndex = value.lastIndexOf("_")
                    if (index > 0 && lastIndex > 0 && index != lastIndex) {
                        value1 = value.substring(0, index)
                        bookItem.author = prefix + value1
                        value1 = value.substring(index + 1, lastIndex)
                        bookItem.name = value1
                    } else {
                        bookItem.author = prefix + "Неизвестно"
                        bookItem.name = value
                    }
                    bookItem.size = getLiteralSize(df.length())
                    bookItem.file = df
                    bookItem.extension = getExtension(value)
                    answer.add(bookItem)
                } else if (df.isDirectory) {
                    answer.addAll(recursiveScan(df.listFiles(), prefix + df.name + "/"))
                }
            }
        }
        return answer
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        Log.d(
            "surprise",
            "ShowDownloadFolderContentActivity onContextItemSelected 87: handle " + item.title
        )
        val position: Int
        try {
            val adapter = recycler.adapter as DirContentAdapter?
            if (adapter != null) {
                position = adapter.position
                val book = adapter.getItem(position)
                if (item.title == getString(R.string.share_link_message)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        shareFile(book.file!!)
                    }
                } else if (item.title == getString(R.string.delete_item_message)) {
                    adapter.delete(book)
                    Toast.makeText(
                        this@DirContentActivity,
                        getString(R.string.item_deleted_message),
                        Toast.LENGTH_LONG
                    ).show()
                } else if (item.title == getString(R.string.open_with_menu_item)) {
                    Log.d(
                        "surprise",
                        "ShowDownloadFolderContentActivity onContextItemSelected 105: open file"
                    )
                    openFile(book.file!!)
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
                if (adapter != null) adapter!!.sort(
                    which
                )
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
}