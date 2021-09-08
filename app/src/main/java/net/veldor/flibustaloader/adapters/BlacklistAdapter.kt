package net.veldor.flibustaloader.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.BlacklistItemBinding
import net.veldor.flibustaloader.selections.BlacklistItem
import java.util.*

class BlacklistAdapter(private var mItems: ArrayList<BlacklistItem>) :
    RecyclerView.Adapter<BlacklistAdapter.ViewHolder>() {
    private var mLayoutInflater: LayoutInflater = LayoutInflater.from(App.instance.applicationContext)
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = BlacklistItemBinding.inflate(mLayoutInflater)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mItems[i])
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun changeList(autocompleteValues: ArrayList<BlacklistItem>) {
        mItems = autocompleteValues
    }

    class ViewHolder(private val mBinding: ViewDataBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        fun bind(item: BlacklistItem) {
            mBinding.setVariable(BR.blacklists, item)
            mBinding.executePendingBindings()
            val container = mBinding.root
            val name = container.findViewById<TextView>(R.id.book_name)
            when (item.type) {
                "book" -> name.setTextColor(ResourcesCompat.getColor(App.instance.resources, R.color.book_name_color, null))
                "author" -> name.setTextColor(ResourcesCompat.getColor(App.instance.resources, R.color.author_text_color, null))
                "sequence" -> name.setTextColor(ResourcesCompat.getColor(App.instance.resources, R.color.sequences_text, null))
                "genre" -> name.setTextColor(ResourcesCompat.getColor(App.instance.resources, R.color.genre_text_color, null))
            }
            val deleteBtn = container.findViewById<View>(R.id.deleteItemBtn)
            deleteBtn?.setOnClickListener {
                when (item.type) {
                    "book" -> App.instance.booksBlacklist.deleteValue(item.name!!)
                    "author" -> App.instance.authorsBlacklist.deleteValue(item.name!!)
                    "sequence" -> App.instance.sequencesBlacklist.deleteValue(item.name!!)
                    "genre" -> App.instance.genresBlacklist.deleteValue(item.name!!)
                }
            }
        }
    }
}