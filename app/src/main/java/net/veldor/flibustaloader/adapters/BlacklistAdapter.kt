package net.veldor.flibustaloader.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.BlacklistItemBinding
import net.veldor.flibustaloader.selections.BlacklistItem
import net.veldor.flibustaloader.utils.BlacklistAuthors
import net.veldor.flibustaloader.utils.BlacklistBooks
import net.veldor.flibustaloader.utils.BlacklistGenres
import net.veldor.flibustaloader.utils.BlacklistSequences
import java.util.*

class BlacklistAdapter(private var mItems: ArrayList<BlacklistItem>) :
    RecyclerView.Adapter<BlacklistAdapter.ViewHolder>() {
    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(App.instance.applicationContext)

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = BlacklistItemBinding.inflate(mLayoutInflater, viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mItems[i])
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun changeList(autocompleteValues: ArrayList<BlacklistItem>) {
        val prevSize = mItems.size
        notifyItemRangeRemoved(0, prevSize)
        mItems = autocompleteValues
        notifyItemRangeInserted(0, mItems.size)
    }

    fun itemAdded(item: BlacklistItem?) {
        if (item != null) {
            // add item to top of list
            mItems.add(0, item)
            notifyItemInserted(0)
        }
    }

    fun itemRemoved(item: BlacklistItem?) {
        if(item != null){
            var foundedItem: BlacklistItem? = null
            mItems.forEach {
                if(it.name == item.name && it.type == item.type){
                    foundedItem = it
                }
            }
            if(foundedItem != null){
                notifyItemRemoved(mItems.indexOf(foundedItem))
                mItems.remove(foundedItem)
            }
        }
    }

    class ViewHolder(private val mBinding: BlacklistItemBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        fun bind(item: BlacklistItem) {
            mBinding.setVariable(BR.blacklists, item)
            mBinding.executePendingBindings()
            val container = mBinding.root
            val name = container.findViewById<TextView>(R.id.name)
            when (item.type) {
                "book" -> name.setTextColor(
                    ResourcesCompat.getColor(
                        App.instance.resources,
                        R.color.book_name_color,
                        null
                    )
                )
                "author" -> name.setTextColor(
                    ResourcesCompat.getColor(
                        App.instance.resources,
                        R.color.author_text_color,
                        null
                    )
                )
                "sequence" -> name.setTextColor(
                    ResourcesCompat.getColor(
                        App.instance.resources,
                        R.color.sequences_text,
                        null
                    )
                )
                "genre" -> name.setTextColor(
                    ResourcesCompat.getColor(
                        App.instance.resources,
                        R.color.genre_text_color,
                        null
                    )
                )
            }
            mBinding.deleteItemBtn.setOnClickListener {
                Log.d("surprise", "bind: del blacklist item")
                when (item.type) {
                    "book" -> BlacklistBooks.instance.deleteValue(item.name)
                    "author" -> BlacklistAuthors.instance.deleteValue(item.name)
                    "sequence" -> BlacklistSequences.instance.deleteValue(item.name)
                    "genre" -> BlacklistGenres.instance.deleteValue(item.name)
                }
            }
        }
    }
}