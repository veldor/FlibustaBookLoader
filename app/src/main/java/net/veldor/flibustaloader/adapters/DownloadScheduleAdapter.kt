package net.veldor.flibustaloader.adapters

import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.databinding.DownloadScheduleBookItemBinding
import net.veldor.flibustaloader.selections.CurrentBookDownloadProgress
import net.veldor.flibustaloader.workers.DownloadBooksWorker.Companion.DOWNLOAD_IN_PROGRESS
import net.veldor.flibustaloader.workers.DownloadBooksWorker.Companion.removeFromQueue

class DownloadScheduleAdapter(private var links: ArrayList<BooksDownloadSchedule>) :
    RecyclerView.Adapter<DownloadScheduleAdapter.ViewHolder>() {
    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(App.instance.applicationContext)

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = DownloadScheduleBookItemBinding.inflate(mLayoutInflater, viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(links[i])
    }

    override fun getItemCount(): Int {
        return links.size
    }

    fun setData(booksDownloadSchedules: ArrayList<BooksDownloadSchedule>) {
        notifyItemRangeRemoved(0, links.size)
        links = booksDownloadSchedules
        notifyItemRangeInserted(0, links.size)
    }

    fun notifyBookDownloaded(book: BooksDownloadSchedule) {
        var index: Int = -1
        links.forEach {
            if (it.link == book.link) {
                index = links.indexOf(it)
            }
        }
        if (index >= 0) {
            links[index] = book
            notifyItemChanged(index)
            Handler(getMainLooper()).postDelayed({
                if(links.isNotEmpty()){
                    var position = -1
                    links.forEach {
                        if(it.bookId == book.bookId){
                            position = links.indexOf(it)
                        }
                    }
                    if(position >= 0 && links.size > position){
                        links.removeAt(position)
                        notifyItemRemoved(position)
                    }
                }
            }, 1000)
        }
    }

    fun notifyBookRemovedFromQueue(book: BooksDownloadSchedule) {
        var index: Int = -1
        links.forEach {
            if (it.link == book.link) {
                index = links.indexOf(it)
            }
        }
        if (index >= 0) {
            links.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun notifyBookDownloadError(book: BooksDownloadSchedule?) {
        if (book != null) {
            var index: Int = -1
            links.forEach {
                if (it.link == book.link) {
                    index = links.indexOf(it)
                }
            }
            if (index >= 0) {
                book.failed = true
                links[index] = book
                notifyItemChanged(index)
            }
        }
    }

    fun notifyBookDownloadInProgress(book: BooksDownloadSchedule?) {
        if (book != null) {
            var index: Int = -1
            links.forEach {
                if (it.link == book.link) {
                    index = links.indexOf(it)
                }
            }
            if (index >= 0) {
                book.inProgress = true
                links[index] = book
                notifyItemChanged(index)
            }
        }
    }

    fun setDownloadProgressChanged(progress: CurrentBookDownloadProgress?) {
        if (progress != null) {
            // get current downloaded book link
            val bookInProgress = App.instance.liveBookDownloadInProgress.value
            if (bookInProgress != null) {
                links.forEach {
                    if (it.link == bookInProgress.link) {
                        val index = links.indexOf(it)
                        it.inProgress = true
                        it.progress = progress
                        notifyItemChanged(index)
                    }
                }
            }
        }
    }

    class ViewHolder(private val mBinding: DownloadScheduleBookItemBinding) :
        RecyclerView.ViewHolder(
            mBinding.root
        ) {

        var item: BooksDownloadSchedule? = null

        fun bind(scheduleItem: BooksDownloadSchedule?) {
            item = scheduleItem
            mBinding.setVariable(BR.book, scheduleItem)
            mBinding.executePendingBindings()
            if (scheduleItem!!.name.isEmpty()) {
                mBinding.name.text = "Имя не найдено"
            }
            when {
                scheduleItem.loaded -> {
                    mBinding.bookStateText.text = App.instance.getString(R.string.book_loaded_text)
                    mBinding.bookStateText.setTextColor(
                        ResourcesCompat.getColor(
                            App.instance.resources,
                            R.color.genre_text_color,
                            null
                        )
                    )
                    mBinding.bookLoadProgress.visibility = View.GONE
                }
                scheduleItem.failed -> {
                    mBinding.bookStateText.text =
                        App.instance.getString(R.string.book_load_failed_text)
                    mBinding.bookStateText.setTextColor(
                        ResourcesCompat.getColor(
                            App.instance.resources,
                            R.color.book_name_color,
                            null
                        )
                    )
                    mBinding.bookLoadProgress.visibility = View.GONE
                }
                scheduleItem.inProgress -> {
                    mBinding.bookStateText.text =
                        App.instance.getString(R.string.book_load_in_progress_text)
                    mBinding.bookStateText.setTextColor(
                        ResourcesCompat.getColor(
                            App.instance.resources,
                            R.color.author_text_color,
                            null
                        )
                    )
                    mBinding.bookLoadProgress.visibility = View.VISIBLE
                    if(scheduleItem.progress != null){
                        mBinding.bookLoadProgress.isIndeterminate = false
                        val progress = scheduleItem.progress!!.percentDone.toInt()
                        Log.d("surprise", "bind: progress is $progress")
                        mBinding.bookLoadProgress.progress = progress
                    }
                    else{
                        mBinding.bookLoadProgress.isIndeterminate = false
                        mBinding.bookLoadProgress.progress = 0
                    }
                }
                else -> {
                    mBinding.bookStateText.text =
                        App.instance.getString(R.string.wait_for_download)
                    mBinding.bookStateText.setTextColor(
                        ResourcesCompat.getColor(
                            App.instance.resources,
                            R.color.dark_gray,
                            null
                        )
                    )
                    if (App.instance.liveDownloadState.value == DOWNLOAD_IN_PROGRESS) {
                        mBinding.bookLoadProgress.visibility = View.VISIBLE
                        mBinding.bookLoadProgress.isIndeterminate = true
                    } else {
                        mBinding.bookLoadProgress.visibility = View.GONE
                    }
                }
            }
            // добавлю действие при клике на кнопку скачивания
            mBinding.deleteItemBtn.setOnClickListener {
                // найду в очереди данную книгу и удалю её из очереди
                removeFromQueue(scheduleItem)
                App.instance.liveBookJustRemovedFromQueue.postValue(item)
                Toast.makeText(
                    App.instance,
                    "Книга удалена из очереди скачивания",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}