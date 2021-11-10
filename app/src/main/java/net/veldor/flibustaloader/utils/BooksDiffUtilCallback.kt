package net.veldor.flibustaloader.utils

import androidx.recyclerview.widget.DiffUtil
import net.veldor.flibustaloader.selections.Book


class BooksDiffUtilCallback : DiffUtil.ItemCallback<Book>() {

    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem.file == newItem.file
    }

    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem.file?.uri == newItem.file?.uri
    }

}