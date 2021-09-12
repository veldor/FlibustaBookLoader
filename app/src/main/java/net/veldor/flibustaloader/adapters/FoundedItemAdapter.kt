package net.veldor.flibustaloader.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.FoundedItemBinding
import net.veldor.flibustaloader.interfaces.MyAdapterInterface
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_AUTHOR
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_BOOK
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_GENRE
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_SEQUENCE
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.ui.OPDSActivity
import java.util.*

class FoundedItemAdapter(arrayList: ArrayList<FoundedEntity>) :

    RecyclerView.Adapter<FoundedItemAdapter.ViewHolder>(), MyAdapterInterface {
    private var values: ArrayList<FoundedEntity> = arrayListOf()

    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(App.instance.applicationContext)

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = FoundedItemBinding.inflate(
            mLayoutInflater, viewGroup, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(values[i])
    }

    override fun getItemCount(): Int {
        return values.size
    }

    fun setContent(newData: ArrayList<FoundedEntity>) {
        notifyItemRangeRemoved(0, values.size)
        values = newData
        notifyItemRangeInserted(0, newData.size)
    }

    fun sort() {
        Toast.makeText(App.instance, "результаты отсортированы!", Toast.LENGTH_SHORT).show()
    }

    override fun clearList() {
        notifyItemRangeRemoved(0, values.size)
        values = ArrayList()
        notifyItemRangeInserted(0,0)
    }

    fun appendContent(results: ArrayList<FoundedEntity>) {
        val oldLength = values.size
        Log.d("surprise", "append: append to existent results")
        values.addAll(results)
        notifyItemRangeInserted(oldLength, results.size)
    }

    inner class ViewHolder(private val binding: FoundedItemBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        private lateinit var item: FoundedEntity
        fun bind(item: FoundedEntity) {
            this.item = item
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            if (OPDSActivity.sElementForSelectionIndex >= 0 && values.size > OPDSActivity.sElementForSelectionIndex && values.indexOf(
                    item
                ) == OPDSActivity.sElementForSelectionIndex
            ) {
                Log.d("surprise", "ViewHolder bind 118: mark selected")
                binding.root.setBackgroundColor(
                    ResourcesCompat.getColor(
                        App.instance.resources,
                        R.color.selected_item_background,
                        null
                    )
                )

                // очищу переменную с элементом
                OPDSActivity.sElementForSelectionIndex = -1
            } else {
                binding.root.background =
                    ResourcesCompat.getDrawable(
                        App.instance.resources,
                        R.drawable.genre_layout,
                        null
                    )
                when (item.type) {
                    TYPE_BOOK -> {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.book_name_color,
                                null
                            )
                        )
                    }
                    TYPE_AUTHOR -> {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.author_text_color,
                                null
                            )
                        )
                    }
                    TYPE_SEQUENCE -> {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.sequences_text,
                                null
                            )
                        )
                    }
                    TYPE_GENRE -> {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.genre_text_color,
                                null
                            )
                        )
                    }
                }
            }
        }
    }

    init {
        if (arrayList.size > 0) {
            values = arrayList
        }
    }
}