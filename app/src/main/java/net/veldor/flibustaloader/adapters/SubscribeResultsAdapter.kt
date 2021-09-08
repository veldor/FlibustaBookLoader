package net.veldor.flibustaloader.adapters

import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.database.dao.DownloadedBooksDao
import net.veldor.flibustaloader.database.dao.ReadedBooksDao
import net.veldor.flibustaloader.databinding.SearchedBookItemBinding
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.FoundedBook
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.util.*

class SubscribeResultsAdapter : RecyclerView.Adapter<SubscribeResultsAdapter.ViewHolder>() {
    private var mBooks = ArrayList<FoundedBook>()
    private var mLayoutInflater: LayoutInflater = LayoutInflater.from(App.instance.applicationContext)
    private val mDao: DownloadedBooksDao = App.instance.mDatabase.downloadedBooksDao()
    private val mReadDao: ReadedBooksDao = App.instance.mDatabase.readBooksDao()
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = SearchedBookItemBinding.inflate(mLayoutInflater)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mBooks[i])
    }

    override fun getItemCount(): Int {
        return mBooks.size
    }

    fun setContent(arrayList: ArrayList<FoundedBook>) {
        mBooks = arrayList
        notifyItemRangeChanged(0, mBooks.size)
    }

    fun bookDownloaded(bookId: String) {
        for (f in mBooks) {
            if (f.id == bookId) {
                f.downloaded = true
                notifyItemChanged(mBooks.lastIndexOf(f))
                break
            }
        }
    }

    inner class ViewHolder(private val mBinding: ViewDataBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        private val mRoot: View = mBinding.root
        fun bind(foundedBook: FoundedBook) {
            mBinding.setVariable(BR.book, foundedBook)
            mBinding.executePendingBindings()
            // добавлю действие при клике на кнопку скачивания
            val container = mBinding.root
            val downloadButton = container.findViewById<Button>(R.id.downloadBookBtn)
            downloadButton.setOnClickListener {
                // если ссылка на скачивание одна- скачаю книгу, если несколько- выдам диалоговое окно со списком форматов для скачивания
                if (foundedBook.downloadLinks.size > 1) {
                    val savedMime = PreferencesHandler.instance.favoriteMime
                    if (savedMime != null) {
                        // проверю, нет ли в списке выбранного формата
                        for (dl in foundedBook.downloadLinks) {
                            if (dl.mime!!.contains(savedMime)) {
                                val result = ArrayList<DownloadLink>()
                                result.add(dl)
                                App.instance.mDownloadLinksList.postValue(result)
                                return@setOnClickListener
                            }
                        }
                    }
                }
                App.instance.mDownloadLinksList.postValue(foundedBook.downloadLinks)
            }
            Handler().post {

                // проверю, если книга прочитана- покажу это
                if (mReadDao.getBookById(foundedBook.id) != null) {
                    val readView = mRoot.findViewById<ImageButton>(R.id.book_read)
                    if (readView != null) {
                        readView.visibility = View.VISIBLE
                        readView.setOnClickListener {
                            Toast.makeText(
                                App.instance,
                                "Книга отмечена как прочитанная",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                // проверю, если книга прочитана- покажу это
                if (mDao.getBookById(foundedBook.id) != null) {
                    val downloadedView = mRoot.findViewById<ImageButton>(R.id.book_downloaded)
                    if (downloadedView != null) {
                        downloadedView.visibility = View.VISIBLE
                        downloadedView.setOnClickListener {
                            Toast.makeText(
                                App.instance,
                                "Книга уже скачивалась",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        init {
            val menu = mRoot.findViewById<View>(R.id.menuButton)
            if (menu != null) {
                menu.visibility = View.GONE
            }
        }
    }

}