package net.veldor.flibustaloader.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.databinding.SubscriptionItemBinding
import net.veldor.flibustaloader.selections.SubscriptionItem
import net.veldor.flibustaloader.utils.SubscribeAuthors
import net.veldor.flibustaloader.utils.SubscribeBooks
import net.veldor.flibustaloader.utils.SubscribeSequences
import java.util.*

class SubscribesAdapter(private var mItems: ArrayList<SubscriptionItem>) :
    RecyclerView.Adapter<SubscribesAdapter.ViewHolder>() {
    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(App.instance.applicationContext)

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = SubscriptionItemBinding.inflate(mLayoutInflater, viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mItems[i])
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun itemAdded(item: SubscriptionItem?) {
        if (item != null) {
            // add item to top of list
            mItems.add(0, item)
            notifyItemInserted(0)
        }
    }

    fun itemRemoved(item: SubscriptionItem?) {
        if (item != null) {
            Log.d("surprise", "itemRemoved: deleting item")
            var foundedItem: SubscriptionItem? = null
            mItems.forEach {
                Log.d("surprise", "itemRemoved: ${it.name} ${it.type} ${item.name} ${item.type}")
                if (it.name == item.name && it.type == item.type) {
                    foundedItem = it
                }
            }
            if (foundedItem != null) {
                notifyItemRemoved(mItems.indexOf(foundedItem))
                mItems.remove(foundedItem)
            }
        }
    }

    fun changeList(autocompleteValues: ArrayList<SubscriptionItem>) {
        val prevSize = mItems.size
        notifyItemRangeRemoved(0, prevSize)
        mItems = autocompleteValues
        notifyItemRangeInserted(0, mItems.size)
    }

    class ViewHolder(private val mBinding: SubscriptionItemBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        fun bind(item: SubscriptionItem) {
            mBinding.setVariable(BR.subscriptions, item)
            mBinding.executePendingBindings()
            mBinding.deleteItemBtn.setOnClickListener {
                when (item.type) {
                    "book" -> SubscribeBooks.instance.deleteValue(item.name)
                    "author" -> SubscribeAuthors.instance.deleteValue(item.name)
                    "sequence" -> SubscribeSequences.instance.deleteValue(item.name)
                }
            }
        }
    }
}