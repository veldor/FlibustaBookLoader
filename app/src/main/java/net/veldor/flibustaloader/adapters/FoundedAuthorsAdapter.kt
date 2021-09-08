package net.veldor.flibustaloader.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.SearchedAuthorItemBinding
import net.veldor.flibustaloader.interfaces.MyAdapterInterface
import net.veldor.flibustaloader.selections.Author
import net.veldor.flibustaloader.ui.OPDSActivity
import net.veldor.flibustaloader.utils.SortHandler.sortAuthors
import java.util.*

class FoundedAuthorsAdapter(arrayList: ArrayList<Author>) :
    RecyclerView.Adapter<FoundedAuthorsAdapter.ViewHolder>(), MyAdapterInterface {
    private var mAuthors: ArrayList<Author> = ArrayList()
    private var mLayoutInflater: LayoutInflater = LayoutInflater.from(App.instance.applicationContext)
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = SearchedAuthorItemBinding.inflate(mLayoutInflater)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mAuthors[i])
    }

    override fun getItemCount(): Int {
        return mAuthors.size
    }

    fun setContent(newData: ArrayList<Author>) {
        if (newData.size == 0 && mAuthors.size == 0) {
            Toast.makeText(App.instance, "Авторы не найдены", Toast.LENGTH_SHORT).show()
            notifyItemRangeChanged(0, 0)
        } else {
            val previousArrayLen = mAuthors.size
            mAuthors.addAll(newData)
            notifyItemRangeInserted(previousArrayLen, newData.size)
        }
    }

    fun sort() {
        sortAuthors(mAuthors)
        notifyItemRangeChanged(0, mAuthors.size)
        Toast.makeText(App.instance, "Авторы отсортированы!", Toast.LENGTH_SHORT).show()
    }

    override fun clearList() {
        mAuthors = ArrayList()
        notifyItemRangeChanged(0, mAuthors.size)
    }

    inner class ViewHolder(private val mBinding: ViewDataBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        private val mRootView: View
        private var mAuthor: Author? = null
        fun bind(foundedAuthor: Author?) {
            mBinding.setVariable(BR.author, foundedAuthor)
            mBinding.executePendingBindings()
            mAuthor = foundedAuthor
            if (OPDSActivity.sElementForSelectionIndex >= 0 && mAuthors.size > OPDSActivity.sElementForSelectionIndex && mAuthors.indexOf(
                    mAuthor
                ) == OPDSActivity.sElementForSelectionIndex
            ) {
                mRootView.setBackgroundColor(ResourcesCompat.getColor(App.instance.resources, R.color.selected_item_background, null))
                // очищу переменную с элементом
                OPDSActivity.sElementForSelectionIndex = -1
            } else {
                mRootView.background =
                    ResourcesCompat.getDrawable(
                        App.instance.resources,
                        R.drawable.author_layout,
                        null
                    )
            }
        }

        init {
            val container = mBinding.root
            mRootView = container.findViewById(R.id.rootView)
            container.setOnClickListener {
                if (mAuthor!!.id!!.startsWith("tag:search:new:author:")) {
                    App.instance.mAuthorNewBooks.postValue(mAuthor)
                    OPDSActivity.sBookmarkName = "Новинки автора: " + mAuthor!!.name
                } else if (mAuthor!!.uri != null) {
                    App.instance.mSelectedAuthor.postValue(mAuthor)
                }
                // сообщу, по какому именно элементу был клик
                OPDSActivity.sClickedItemIndex = mAuthors.indexOf(mAuthor)
            }
        }
    }

    init {
        if (arrayList.size > 0) {
            mAuthors = arrayList
        }
    }
}