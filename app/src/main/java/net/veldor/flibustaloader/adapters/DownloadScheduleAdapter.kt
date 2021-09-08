package net.veldor.flibustaloader.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.databinding.DownloadScheduleBookItemBinding
import net.veldor.flibustaloader.workers.DownloadBooksWorker.Companion.removeFromQueue

class DownloadScheduleAdapter(private var mBooks: List<BooksDownloadSchedule>) :
    RecyclerView.Adapter<DownloadScheduleAdapter.ViewHolder>() {
    private var mLayoutInflater: LayoutInflater = LayoutInflater.from(App.instance.applicationContext)
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = DownloadScheduleBookItemBinding.inflate(mLayoutInflater)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mBooks[i])
    }

    override fun getItemCount(): Int {
        return mBooks.size
    }

    fun setData(booksDownloadSchedules: List<BooksDownloadSchedule>) {
        mBooks = booksDownloadSchedules
    }

    class ViewHolder(private val mBinding: ViewDataBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        fun bind(scheduleItem: BooksDownloadSchedule?) {
            mBinding.setVariable(BR.book, scheduleItem)
            mBinding.executePendingBindings()
            // добавлю действие при клике на кнопку скачивания
            val container = mBinding.root
            val deleteItem = container.findViewById<View>(R.id.deleteItemBtn)
            deleteItem?.setOnClickListener {
                // найду в очереди данную книгу и удалю её из очереди
                removeFromQueue(scheduleItem)
                //DownloadScheduleAdapter.this.notifyDataSetChanged();
                Toast.makeText(
                    App.instance,
                    "Книга удалена из очереди скачивания",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}