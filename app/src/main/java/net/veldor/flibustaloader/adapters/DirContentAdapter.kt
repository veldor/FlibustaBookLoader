package net.veldor.flibustaloader.adapters

import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.FileItemBinding
import net.veldor.flibustaloader.selections.Book
import net.veldor.flibustaloader.utils.SortHandler.sortLoadedBooks
import java.util.*

class DirContentAdapter(private val mItems: ArrayList<Book>) :
    RecyclerView.Adapter<DirContentAdapter.ViewHolder>() {
    private var mLayoutInflater: LayoutInflater = LayoutInflater.from(App.instance.applicationContext)
    var position = 0
        private set
    private var lastWhich = 0
    fun getItem(position: Int): Book {
        return mItems[position]
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = FileItemBinding.inflate(mLayoutInflater)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mItems[i])
        viewHolder.itemView.setOnLongClickListener {
            position = viewHolder.adapterPosition
            false
        }
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun delete(book: Book) {
        mItems.removeAt(position)
        notifyItemRemoved(position)
        book.file?.delete()
    }

    fun sort(which: Int) {
        sortLoadedBooks(mItems, which, which == lastWhich)
        lastWhich = if (which != lastWhich) {
            which
        } else {
            -1
        }
        notifyItemRangeChanged(0, mItems.size)
    }

    class ViewHolder(private val mBinding: ViewDataBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ), OnCreateContextMenuListener {
        fun bind(item: Book?) {
            mBinding.setVariable(BR.book, item)
            mBinding.executePendingBindings()
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
            menu.setHeaderTitle(App.instance.getString(R.string.books_options_message))
            menu.add(0, v.id, 0, App.instance.getString(R.string.share_link_message))
            menu.add(0, v.id, 0, App.instance.getString(R.string.open_with_menu_item))
            menu.add(0, v.id, 0, App.instance.getString(R.string.delete_item_message))
        }

        init {
            val mRoot = mBinding.root
            mRoot.setOnCreateContextMenuListener(this)
        }
    }
}