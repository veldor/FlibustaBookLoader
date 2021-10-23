package net.veldor.flibustaloader.adapters

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.databinding.FoundedItemBinding
import net.veldor.flibustaloader.delegates.FoundedItemActionDelegate
import net.veldor.flibustaloader.interfaces.MyAdapterInterface
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_AUTHOR
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_AUTHORS
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_BOOK
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_GENRE
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_SEQUENCE
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.SortHandler
import java.util.*
import kotlin.coroutines.CoroutineContext

class FoundedItemAdapter(
    arrayList: ArrayList<FoundedEntity>,
    val delegate: FoundedItemActionDelegate
) :
    CoroutineScope,
    RecyclerView.Adapter<FoundedItemAdapter.ViewHolder>(), MyAdapterInterface {

    private var hasNext: Boolean = false
    private var job: Job = Job()
    private var menuClicked: FoundedEntity? = null
    private var centerItemPressed: FoundedEntity? = null
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
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
        if (i < values.size) {
            viewHolder.bind(values[i])
        } else {
            viewHolder.bindButton()
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.timer?.cancel()
    }

    override fun getItemCount(): Int {
        if (PreferencesHandler.instance.isShowLoadMoreBtn() && values.size > 0 && hasNext && values[0].type == TYPE_BOOK) {
            return values.size + 1
        }
        return values.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setContent(newData: ArrayList<FoundedEntity>) {
        values = newData
        notifyDataSetChanged()
    }

    fun sort() {
        Toast.makeText(App.instance, "результаты отсортированы!", Toast.LENGTH_SHORT).show()
    }

    override fun clearList() {
        notifyItemRangeRemoved(0, values.size)
        values = ArrayList()
        notifyItemRangeInserted(0, 0)
    }

    fun appendContent(results: ArrayList<FoundedEntity>) {
        val oldLength = values.size
        values.addAll(results)
        notifyItemRangeInserted(oldLength, results.size)
    }

    inner class ViewHolder(private val binding: FoundedItemBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        var timer: CountDownTimer? = null

        init {
            binding.menuButton.setOnClickListener {
                menuClicked = binding.item
                delegate.menuItemPressed(item, binding.menuButton)
            }
        }

        private lateinit var item: FoundedEntity

        init {
            binding.previewImage.setOnClickListener {
                delegate.imageClicked(item)
            }
        }

        fun bind(item: FoundedEntity) {
            this.item = item
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()

            if (item.selected) {
                Log.d("surprise", "bind: i selected!!!!")
                binding.rootView.setBackgroundColor(
                    ResourcesCompat.getColor(
                        App.instance.resources,
                        R.color.selected_item_background,
                        null
                    )
                )
            } else {
                binding.root.background =
                    ResourcesCompat.getDrawable(
                        App.instance.resources,
                        R.drawable.genre_layout,
                        null
                    )
            }

            if (item.buttonPressed) {
                binding.centerActionBtn.setTextColor(
                    ResourcesCompat.getColor(
                        App.instance.resources,
                        R.color.book_name_color,
                        null
                    )
                )
            } else {
                binding.centerActionBtn.setTextColor(
                    ResourcesCompat.getColor(
                        App.instance.resources,
                        R.color.dark_gray,
                        null
                    )
                )
            }
            binding.centerActionBtn.setOnClickListener {
                centerItemPressed = binding.item
                delegate.buttonPressed(item)
                item.buttonPressed = true
                binding.centerActionBtn.setTextColor(
                    ResourcesCompat.getColor(
                        App.instance.resources,
                        R.color.dark_gray,
                        null
                    )
                )
            }
//            binding.root.background =
//                ResourcesCompat.getDrawable(
//                    App.instance.resources,
//                    R.drawable.genre_layout,
//                    null
//                )
            when (item.type) {
                TYPE_BOOK -> {
                    binding.firstGroup.visibility = View.VISIBLE
                    binding.secondGroup.visibility = View.VISIBLE
                    binding.thirdGroup.visibility = View.VISIBLE
                    if(PreferencesHandler.instance.isEInk){
                        binding.firstInfoBlockLeftParam.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )
                        binding.firstInfoBlockRightParam.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )
                        binding.secondInfoBlockLeftParam.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )
                        binding.secondInfoBlockRightParam.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )

                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )

                        binding.centerActionBtn.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )
                    }
                    else{
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.book_name_color,
                                null
                            )
                        )

                        binding.centerActionBtn.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.book_name_color,
                                null
                            )
                        )
                    }
                    binding.menuButton.visibility = View.VISIBLE
                    binding.leftActionBtn.visibility =
                        if (item.read) View.VISIBLE else View.INVISIBLE
                    binding.rightActionBtn.visibility =
                        if (item.downloaded) View.VISIBLE else View.INVISIBLE
                    binding.centerActionBtn.text = App.instance.getString(R.string.download_message)
                    binding.firstInfoBlockLeftParam.text = item.author
                    binding.firstInfoBlockLeftParam.visibility = View.VISIBLE
                    binding.firstInfoBlockLeftParam.setOnClickListener {
                        menuClicked = binding.item
                        delegate.authorClicked(item)
                    }
                    binding.secondInfoBlockRightParam.setOnClickListener {
                        menuClicked = binding.item
                        delegate.sequenceClicked(item)
                    }
                    binding.name.setOnClickListener{
                        menuClicked = binding.item
                        delegate.nameClicked(item)
                    }
                    binding.firstInfoBlockRightParam.text = item.translate
                    binding.firstInfoBlockRightParam.visibility = View.VISIBLE
                    binding.secondInfoBlockLeftParam.text = item.genreComplex
                    binding.secondInfoBlockLeftParam.visibility = View.VISIBLE
                    binding.secondInfoBlockRightParam.text = item.sequencesComplex
                    binding.secondInfoBlockRightParam.visibility = View.VISIBLE
                    binding.thirdBlockLeftElement.text = item.format
                    binding.thirdBlockLeftElement.visibility = View.VISIBLE
                    binding.thirdBlockCenterElement.text = item.downloadsCount
                    binding.thirdBlockCenterElement.visibility = View.VISIBLE
                    binding.thirdBlocRightElement.text = item.size
                    binding.thirdBlocRightElement.visibility = View.VISIBLE
                    if (PreferencesHandler.instance.isPreviews && item.coverUrl != null) {
                        // гружу обложку
                        binding.previewImage.visibility = View.VISIBLE
                        if (item.cover != null) {
                            binding.previewImage.setImageBitmap(item.cover)
                        } else {
                            binding.previewImage.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    App.instance.resources,
                                    R.drawable.image_wait_load,
                                    null
                                )
                            )
                            // periodic check cover loaded
                            timer =
                                object : CountDownTimer(30000.toLong(), 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                        if (item.cover != null) {
                                            binding.previewImage.setImageBitmap(item.cover)
                                            timer?.cancel()
                                        }
                                    }

                                    override fun onFinish() {
                                    }
                                }
                            timer?.start()
                        }
                    } else {
                        // скрою окно иконки
                        binding.previewImage.visibility = View.GONE
                    }
                }
                TYPE_AUTHOR -> {
                    binding.previewImage.visibility = View.GONE
                    binding.leftActionBtn.visibility = View.INVISIBLE
                    binding.rightActionBtn.visibility = View.INVISIBLE
                    binding.firstGroup.visibility = View.GONE
                    binding.secondGroup.visibility = View.GONE

                    if (PreferencesHandler.instance.isEInk) {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )

                        binding.centerActionBtn.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )
                    } else {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.author_text_color,
                                null
                            )
                        )

                        binding.centerActionBtn.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.author_text_color,
                                null
                            )
                        )
                    }
                    binding.thirdBlocRightElement.text = ""
                    binding.thirdBlockLeftElement.text = ""
                    binding.thirdBlockCenterElement.text = item.content
                    binding.thirdGroup.visibility = View.VISIBLE
                    binding.menuButton.visibility = View.GONE
                    binding.centerActionBtn.text = App.instance.getString(R.string.show_message)
                }
                TYPE_SEQUENCE -> {
                    binding.previewImage.visibility = View.GONE
                    binding.leftActionBtn.visibility = View.INVISIBLE
                    binding.rightActionBtn.visibility = View.INVISIBLE
                    binding.previewImage.visibility = View.GONE
                    binding.firstGroup.visibility = View.GONE
                    binding.secondGroup.visibility = View.GONE
                    binding.thirdBlocRightElement.text = ""
                    binding.thirdBlockLeftElement.text = ""
                    binding.thirdBlockCenterElement.text = item.content
                    binding.thirdGroup.visibility = View.VISIBLE
                    binding.thirdBlockCenterElement.visibility = View.VISIBLE
                    if (PreferencesHandler.instance.isEInk) {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )

                        binding.centerActionBtn.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )
                    } else {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.sequences_text,
                                null
                            )
                        )

                        binding.centerActionBtn.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.sequences_text,
                                null
                            )
                        )
                    }
                    binding.menuButton.visibility = View.GONE
                    binding.centerActionBtn.text = App.instance.getString(R.string.show_message)
                    binding.thirdBlockCenterElement.text = item.description
                    binding.thirdBlockCenterElement.visibility = View.VISIBLE
                }
                TYPE_GENRE -> {
                    binding.previewImage.visibility = View.GONE
                    binding.leftActionBtn.visibility = View.INVISIBLE
                    binding.rightActionBtn.visibility = View.INVISIBLE
                    binding.previewImage.visibility = View.GONE
                    binding.firstGroup.visibility = View.GONE
                    binding.secondGroup.visibility = View.GONE
                    binding.thirdBlocRightElement.text = ""
                    binding.thirdBlockLeftElement.text = ""
                    binding.thirdBlockCenterElement.text = item.content
                    binding.thirdGroup.visibility = View.VISIBLE
                    if (PreferencesHandler.instance.isEInk) {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )
                        binding.centerActionBtn.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )
                    } else {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.genre_text_color,
                                null
                            )
                        )
                        binding.centerActionBtn.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.genre_text_color,
                                null
                            )
                        )
                    }
                    binding.menuButton.visibility = View.GONE
                    binding.centerActionBtn.text = App.instance.getString(R.string.show_message)
                }
                TYPE_AUTHORS -> {
                    binding.leftActionBtn.visibility = View.INVISIBLE
                    binding.rightActionBtn.visibility = View.INVISIBLE
                    binding.previewImage.visibility = View.GONE
                    binding.firstGroup.visibility = View.GONE
                    binding.secondGroup.visibility = View.GONE
                    binding.thirdGroup.visibility = View.GONE
                    if (PreferencesHandler.instance.isEInk) {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )
                        binding.centerActionBtn.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.black,
                                null
                            )
                        )
                    } else {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.author_text_color,
                                null
                            )
                        )
                        binding.centerActionBtn.setTextColor(
                            ResourcesCompat.getColor(
                                App.instance.resources,
                                R.color.author_text_color,
                                null
                            )
                        )
                    }
                    binding.menuButton.visibility = View.GONE
                    binding.centerActionBtn.text = App.instance.getString(R.string.show_message)
                }
            }
        }

        fun bindButton() {
            binding.firstGroup.visibility = View.GONE
            binding.secondGroup.visibility = View.GONE
            binding.thirdGroup.visibility = View.GONE
            binding.previewImage.visibility = View.GONE
            binding.menuButton.visibility = View.GONE
            binding.name.visibility = View.GONE
            binding.centerActionBtn.text = App.instance.getString(R.string.load_more_button)
            binding.centerActionBtn.setOnClickListener {
                it.isEnabled = false
                binding.centerActionBtn.setTextColor(
                    ResourcesCompat.getColor(
                        App.instance.resources,
                        R.color.dark_gray,
                        null
                    )
                )
                delegate.loadMoreBtnClicked()
            }
        }
    }

    fun getList(): ArrayList<FoundedEntity> {
        return values
    }

    fun bookLoaded(book: BooksDownloadSchedule?) {
        if (book != null) {
            values.forEach {
                if (it.id == book.bookId) {
                    val position = values.lastIndexOf(it)
                    if (PreferencesHandler.instance.isHideDownloaded) {
                        values.remove(it)
                        notifyItemRemoved(position)
                    } else {
                        it.downloaded = true
                        notifyItemChanged(position)
                    }
                }
            }
        }
    }


    fun bookRead(book: FoundedEntity) {
        values.forEach {
            if (it.id == book.id) {
                val position = values.lastIndexOf(it)
                if (PreferencesHandler.instance.isHideRead) {
                    values.remove(it)
                    notifyItemRemoved(position)
                } else {
                    it.read = true
                    notifyItemChanged(position)
                }
            }
        }
    }

    fun bookDownloaded(book: FoundedEntity) {

        values.forEach {
            if (it.id == book.id) {
                val position = values.lastIndexOf(it)
                if (PreferencesHandler.instance.isHideDownloaded) {
                    values.remove(it)
                    notifyItemRemoved(position)
                } else {
                    it.downloaded = true
                    notifyItemChanged(position)
                }
            }
        }
    }

    fun sortList(sortOption: Int) {
        if (values.isNotEmpty()) {
            // отсортирую в зависимости от типа
            when (values[0].type!!) {
                TYPE_BOOK -> SortHandler.sortBooks(values, sortOption)
                TYPE_AUTHOR -> SortHandler.sortAuthors(values, sortOption)
                else -> SortHandler.sortList(values, sortOption)
            }
            notifyItemRangeChanged(0, values.size)
        }
    }

    fun getContentItem(): FoundedEntity? {
        return menuClicked
    }

    fun getClickedItem(): Int {
        if (centerItemPressed != null) {
            return values.indexOf(centerItemPressed)
        }
        return 0
    }


    init {
        if (arrayList.size > 0) {
            values = arrayList
        }
    }

    fun setHasNext(isNext: Boolean) {
        hasNext = isNext
    }

    fun markClickedElement(clickedElementIndex: Int) {
        values[clickedElementIndex].selected = true
        notifyItemChanged(clickedElementIndex)
    }

    fun getSize(): Int {
        return values.size
    }

    var isScrolledToLast: Boolean = false
}