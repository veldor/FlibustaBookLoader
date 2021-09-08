package net.veldor.flibustaloader.adapters

import net.veldor.flibustaloader.utils.SortHandler.sortGenres
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import net.veldor.flibustaloader.R
import androidx.databinding.ViewDataBinding
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.App
import android.util.Log
import net.veldor.flibustaloader.ui.OPDSActivity
import android.widget.Toast
import android.view.View
import net.veldor.flibustaloader.interfaces.MyAdapterInterface
import net.veldor.flibustaloader.selections.Genre
import androidx.core.content.res.ResourcesCompat
import net.veldor.flibustaloader.databinding.SearchedGenreItemBinding
import java.util.ArrayList

class FoundedGenresAdapter(arrayList: ArrayList<Genre>) :
    RecyclerView.Adapter<FoundedGenresAdapter.ViewHolder>(), MyAdapterInterface {
    private var mGenres: ArrayList<Genre> = arrayListOf()
    private var mLayoutInflater: LayoutInflater = LayoutInflater.from(App.instance.applicationContext)
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding: SearchedGenreItemBinding = DataBindingUtil.inflate(
            mLayoutInflater, R.layout.searched_genre_item, viewGroup, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mGenres[i])
    }

    override fun getItemCount(): Int {
        return mGenres.size
    }

    fun setContent(newData: ArrayList<Genre>) {
        if (newData.size == 0 && mGenres.size == 0) {
            Toast.makeText(App.instance, "Жанры не найдены", Toast.LENGTH_SHORT).show()
            notifyItemRangeChanged(0,0)
        } else {
            val previousArrayLen = mGenres.size
            mGenres.addAll(newData)
            notifyItemRangeInserted(previousArrayLen, newData.size)
        }
    }

    fun sort() {
        sortGenres(mGenres)
        notifyItemRangeChanged(0, mGenres.size)
        Toast.makeText(App.instance, "Серии отсортированы!", Toast.LENGTH_SHORT).show()
    }

    override fun clearList() {
        mGenres = ArrayList()
        notifyItemRangeChanged(0,0)
    }

    inner class ViewHolder(private val mBinding: ViewDataBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        private val mRootView: View
        private var mGenre: Genre? = null
        fun bind(foundedGenre: Genre?) {
            mGenre = foundedGenre
            mBinding.setVariable(BR.genre, foundedGenre)
            mBinding.executePendingBindings()
            if (OPDSActivity.sElementForSelectionIndex >= 0 && mGenres.size > OPDSActivity.sElementForSelectionIndex && mGenres.indexOf(
                    mGenre
                ) == OPDSActivity.sElementForSelectionIndex
            ) {
                Log.d("surprise", "ViewHolder bind 118: mark selected")
                mRootView.setBackgroundColor(ResourcesCompat.getColor(App.instance.resources, R.color.selected_item_background, null))

                // очищу переменную с элементом
                OPDSActivity.sElementForSelectionIndex = -1
            } else {
                mRootView.background =
                    ResourcesCompat.getDrawable(
                        App.instance.resources,
                        R.drawable.genre_layout,
                        null
                    )
            }
        }

        init {
            val container = mBinding.root
            mRootView = container.findViewById(R.id.rootView)
            container.setOnClickListener {
                OPDSActivity.sLiveSearchLink.postValue(mGenre!!.term)
                // сообщу, по какому именно элементу был клик
                OPDSActivity.sClickedItemIndex = mGenres.indexOf(mGenre)
                if (mGenre!!.term!!.contains("newgenres")) {
                    OPDSActivity.sBookmarkName = "Новинки в жанре: " + mGenre!!.label
                } else {
                    OPDSActivity.sBookmarkName = "Жанр: " + mGenre!!.label
                }
            }
        }
    }

    init {
        if (arrayList.size > 0) {
            mGenres = arrayList
        }
    }
}