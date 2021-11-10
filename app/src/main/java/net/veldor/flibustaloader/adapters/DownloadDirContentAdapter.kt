package net.veldor.flibustaloader.adapters

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.BookListItemBinding
import net.veldor.flibustaloader.selections.Book

class DownloadDirContentAdapter constructor(diffUtilCallback: DiffUtil.ItemCallback<Book>) :
    PagedListAdapter<Book, DownloadDirContentAdapter.BooksViewHolder>(diffUtilCallback) {

    var contexItemPosition = 0
    private var mLayoutInflater: LayoutInflater = LayoutInflater.from(App.instance)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BooksViewHolder {
        val binding = BookListItemBinding.inflate(mLayoutInflater, parent, false)
        return BooksViewHolder(binding)
    }

    class BooksViewHolder(binding: BookListItemBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnCreateContextMenuListener {

        private var mBinding: BookListItemBinding = binding

        fun bind(item: Book?) {
            if (item != null) {
                mBinding.setVariable(BR.item, item)
                mBinding.executePendingBindings()
            } else {
                mBinding.authorName.text = ""
                mBinding.bookFormat.text = ""
                mBinding.bookSize.text = ""
                mBinding.name.text = ""
            }
        }

        override fun onCreateContextMenu(
            menu: ContextMenu,
            v: View,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            menu.setHeaderTitle(App.instance.getString(R.string.books_options_message))
            menu.add(0, v.id, 0, App.instance.getString(R.string.share_link_message))
            menu.add(0, v.id, 0, App.instance.getString(R.string.open_with_menu_item))
        }

        init {
            val mRoot = mBinding.root
            mRoot.setOnCreateContextMenuListener(this)
        }
    }

    override fun onBindViewHolder(holder: BooksViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.setOnLongClickListener {
            contexItemPosition = holder.adapterPosition
            false
        }
    }

    fun requireItem(position: Int): Book? {
        return getItem(position)
    }
}