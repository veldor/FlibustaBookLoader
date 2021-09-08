package net.veldor.flibustaloader.adapters

import net.veldor.flibustaloader.utils.SortHandler.sortSequences
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import net.veldor.flibustaloader.R
import androidx.databinding.ViewDataBinding
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.ui.OPDSActivity
import android.widget.Toast
import android.view.View
import net.veldor.flibustaloader.interfaces.MyAdapterInterface
import androidx.core.content.res.ResourcesCompat
import net.veldor.flibustaloader.databinding.SearchedSequenceItemBinding
import net.veldor.flibustaloader.selections.FoundedSequence
import java.util.ArrayList

class FoundedSequencesAdapter(arrayList: ArrayList<FoundedSequence>) :
    RecyclerView.Adapter<FoundedSequencesAdapter.ViewHolder>(), MyAdapterInterface {
    private var mSequences: ArrayList<FoundedSequence> = arrayListOf()
    private var mLayoutInflater: LayoutInflater = LayoutInflater.from(App.instance.applicationContext)
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding: SearchedSequenceItemBinding = DataBindingUtil.inflate(
            mLayoutInflater, R.layout.searched_sequence_item, viewGroup, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mSequences[i])
    }

    override fun getItemCount(): Int {
        return mSequences.size
    }

    fun setContent(newData: ArrayList<FoundedSequence>) {
        if (newData.size == 0 && mSequences.size == 0) {
            Toast.makeText(App.instance, "Жанры не найдены", Toast.LENGTH_SHORT).show()
            notifyItemRangeChanged(0,0)
        } else {
            val previousArrayLen = mSequences.size
            mSequences.addAll(newData)
            notifyItemRangeInserted(previousArrayLen, newData.size)
        }
    }

    fun sort() {
        sortSequences(mSequences)
        notifyItemRangeChanged(0,mSequences.size)
        Toast.makeText(App.instance, "Серии отсортированы!", Toast.LENGTH_SHORT).show()
    }

    override fun clearList() {
        mSequences = ArrayList()
        notifyItemRangeChanged(0, 0)
    }

    inner class ViewHolder(private val mBinding: ViewDataBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        private val mRootView: View
        private var mSequence: FoundedSequence? = null
        fun bind(sequence: FoundedSequence?) {
            mSequence = sequence
            mBinding.setVariable(BR.sequence, sequence)
            mBinding.executePendingBindings()
            if (OPDSActivity.sElementForSelectionIndex >= 0 && mSequences.size > OPDSActivity.sElementForSelectionIndex && mSequences.indexOf(
                    mSequence
                ) == OPDSActivity.sElementForSelectionIndex
            ) {
                mRootView.setBackgroundColor(ResourcesCompat.getColor(App.instance.resources, R.color.selected_item_background, null))
                // очищу переменную с элементом
                OPDSActivity.sElementForSelectionIndex = -1
            } else {
                mRootView.background =
                    ResourcesCompat.getDrawable(
                        App.instance.resources,
                        R.drawable.sequence_layout,
                        null
                    )
            }
        }

        init {
            val container = mBinding.root
            mRootView = container.findViewById(R.id.rootView)
            container.setOnClickListener {
                App.instance.mSelectedSequence.postValue(mSequence)

                // сообщу, по какому именно элементу был клик
                OPDSActivity.sClickedItemIndex = mSequences.indexOf(mSequence)
                if (mSequence!!.link!!.contains("newsequences")) {
                    OPDSActivity.sBookmarkName = "Новинки в серии: " + mSequence!!.title
                } else {
                    OPDSActivity.sBookmarkName = "Серия: " + mSequence!!.title
                }
            }
        }
    }

    init {
        if (arrayList.size > 0) {
            mSequences = arrayList
        }
    }
}