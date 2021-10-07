package net.veldor.flibustaloader.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.database.dao.BookmarksDao
import net.veldor.flibustaloader.database.entity.Bookmark
import net.veldor.flibustaloader.databinding.BookmarkItemBinding
import net.veldor.flibustaloader.ui.BrowserActivity
import net.veldor.flibustaloader.ui.fragments.OpdsFragment.Companion.TARGET_LINK

class BookmarksAdapter(private var mBookmarks: MutableList<Bookmark>) :
    RecyclerView.Adapter<BookmarksAdapter.ViewHolder>() {
    private var mLayoutInflater: LayoutInflater = LayoutInflater.from(App.instance.applicationContext)
    private val mDao: BookmarksDao = App.instance.mDatabase.bookmarksDao()
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = BookmarkItemBinding.inflate(mLayoutInflater, viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mBookmarks[i])
    }

    override fun getItemCount(): Int {
        return mBookmarks.size
    }

    inner class ViewHolder(private val mBinding: BookmarkItemBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        fun bind(bookmark: Bookmark) {
            mBinding.setVariable(BR.bookmark, bookmark)
            mBinding.executePendingBindings()
            // добавлю действие при клике на кнопку скачивания
            val container = mBinding.root
            val itemName = container.findViewById<View>(R.id.bookmark_name)
            itemName.setOnClickListener {
                val intent = Intent(App.instance, BrowserActivity::class.java)
                intent.putExtra(TARGET_LINK, bookmark.link)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                App.instance.startActivity(intent)
            }
            val deleteItemBtn = container.findViewById<View>(R.id.deleteItemBtn)
            deleteItemBtn.setOnClickListener {
                mDao.delete(bookmark)
                notifyItemRemoved(mBookmarks.indexOf(bookmark))
                mBookmarks.remove(bookmark)
                Toast.makeText(App.instance, "Закладка удалена", Toast.LENGTH_SHORT).show()
            }
        }
    }

}