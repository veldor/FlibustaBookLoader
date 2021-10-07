package net.veldor.flibustaloader.adapters

import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.database.dao.DownloadedBooksDao
import net.veldor.flibustaloader.database.dao.ReadedBooksDao
import net.veldor.flibustaloader.databinding.SearchedBookItemBinding
import net.veldor.flibustaloader.delegates.FoundedItemActionDelegate
import net.veldor.flibustaloader.selections.FoundedEntity

class SubscribeResultsAdapter(private  var mBooks: ArrayList<FoundedEntity>, val delegate: FoundedItemActionDelegate) : RecyclerView.Adapter<SubscribeResultsAdapter.ViewHolder>() {


    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(App.instance.applicationContext)


    private val mDao: DownloadedBooksDao = App.instance.mDatabase.downloadedBooksDao()
    private val mReadDao: ReadedBooksDao = App.instance.mDatabase.readBooksDao()

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = SearchedBookItemBinding.inflate(mLayoutInflater, viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mBooks[i])
    }

    override fun getItemCount(): Int {
        return mBooks.size
    }

    fun bookFound(book: FoundedEntity) {
        mBooks.add(book)
        notifyItemInserted(mBooks.size - 1)
    }

    fun clear(){
        notifyItemRangeRemoved(0, mBooks.size)
        mBooks = arrayListOf()
    }

    inner class ViewHolder(private val binding: SearchedBookItemBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun bind(foundedBook: FoundedEntity) {
            binding.setVariable(BR.book, foundedBook)
            binding.executePendingBindings()
            if(foundedBook.buttonPressed){
                binding.centerActionBtn.setTextColor(ResourcesCompat.getColor(App.instance.resources, R.color.dark_gray, null))
            }
            else{
                binding.centerActionBtn.setTextColor(ResourcesCompat.getColor(App.instance.resources, R.color.book_name_color, null))
            }
            binding.centerActionBtn.setOnClickListener {
                foundedBook.buttonPressed = true
                binding.centerActionBtn.setTextColor(ResourcesCompat.getColor(App.instance.resources, R.color.dark_gray, null))
                delegate.buttonPressed(foundedBook)
            }
            Handler().post {

                // проверю, если книга прочитана- покажу это
                if (mReadDao.getBookById(foundedBook.id) != null) {
                    binding.bookRead.visibility = View.VISIBLE
                    binding.bookRead.setOnClickListener {
                        Toast.makeText(
                            App.instance,
                            "Книга отмечена как прочитанная",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                // проверю, если книга прочитана- покажу это
                if (mDao.getBookById(foundedBook.id) != null) {
                    binding.bookDownloaded.visibility = View.VISIBLE
                    binding.bookDownloaded.setOnClickListener {
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

}