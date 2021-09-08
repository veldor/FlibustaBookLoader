package net.veldor.flibustaloader.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.SubscriptionItemBinding
import net.veldor.flibustaloader.selections.SubscriptionItem
import java.util.*

class SubscribesAdapter(private var mItems: ArrayList<SubscriptionItem>) :
    RecyclerView.Adapter<SubscribesAdapter.ViewHolder>() {
    private var mLayoutInflater: LayoutInflater = LayoutInflater.from(App.instance.applicationContext)
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = SubscriptionItemBinding.inflate(mLayoutInflater)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mItems[i])
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun changeList(autocompleteValues: ArrayList<SubscriptionItem>) {
        mItems = autocompleteValues
    }

    class ViewHolder(private val mBinding: ViewDataBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        fun bind(item: SubscriptionItem) {
            mBinding.setVariable(BR.subscriptions, item)
            mBinding.executePendingBindings()
            val container = mBinding.root
            val deleteBtn = container.findViewById<View>(R.id.deleteItemBtn)
            deleteBtn?.setOnClickListener {
                when (item.type) {
                    "book" -> App.instance.booksSubscribe.deleteValue(item.name!!)
                    "author" -> App.instance.authorsSubscribe.deleteValue(item.name!!)
                    "sequence" -> App.instance.sequencesSubscribe.deleteValue(item.name!!)
                }
            }
        }
    }
}