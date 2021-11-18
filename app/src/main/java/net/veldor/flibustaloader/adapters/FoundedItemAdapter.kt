package net.veldor.flibustaloader.adapters

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.databinding.FoundedItemBinding
import net.veldor.flibustaloader.delegates.FoundedItemActionDelegate
import net.veldor.flibustaloader.handlers.Filter
import net.veldor.flibustaloader.interfaces.MyAdapterInterface
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_AUTHOR
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_AUTHORS
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_BOOK
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_GENRE
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_SEQUENCE
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.utils.*
import net.veldor.flibustaloader.workers.SendLogWorker
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.coroutines.CoroutineContext

class FoundedItemAdapter(
    arrayList: ArrayList<FoundedEntity>,
    val delegate: FoundedItemActionDelegate
) :
    CoroutineScope,
    RecyclerView.Adapter<FoundedItemAdapter.ViewHolder>(), MyAdapterInterface {

    private var lastSortOption: Int = -1
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
            if (hasNext) {
                viewHolder.bindButton()
            } else {
                viewHolder.bindInvisible()
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.timer?.cancel()
    }

    override fun getItemCount(): Int {
        return values.size + 1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setContent(newData: ArrayList<FoundedEntity>) {
        Log.d("surprise", "setContent: set content length by ${newData.size}")
        lastSortOption = -1
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
        lastSortOption = -1
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

        @SuppressLint("NotifyDataSetChanged")
        fun bind(item: FoundedEntity) {
            binding.name.visibility = View.VISIBLE
            binding.rootView.visibility = View.VISIBLE
            this.item = item
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            binding.loadingMoreBar.visibility = View.GONE
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


                    if (PreferencesHandler.instance.isFilterByLongClick()) {
                        binding.name.setOnLongClickListener {
                            Toast.makeText(
                                App.instance,
                                "Добавляю книгу в ЧС: ${item.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            BlacklistBooks.instance.addValue(item.name!!.trim().lowercase())
                            applyFilters()
                            return@setOnLongClickListener true
                        }
                        binding.firstInfoBlockLeftParam.setOnLongClickListener {
                            // add authors to blacklist
                            if (item.authors.isNotEmpty()) {
                                item.authors.forEach {
                                    BlacklistAuthors.instance.addValue(it.name!!.trim().lowercase())
                                    Toast.makeText(
                                        App.instance,
                                        "Добавляю автора в ЧС: ${it.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                applyFilters()
                            }
                            return@setOnLongClickListener true
                        }
                        binding.secondInfoBlockLeftParam.setOnLongClickListener {
                            // add genres to blacklist
                            if (item.genres.isNotEmpty()) {
                                item.genres.forEach {
                                    BlacklistGenres.instance.addValue(it.name!!.trim().lowercase())
                                    Toast.makeText(
                                        App.instance,
                                        "Добавляю жанр в ЧС: ${it.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                applyFilters()
                            }
                            return@setOnLongClickListener true
                        }
                        binding.secondInfoBlockRightParam.setOnLongClickListener {
                            // add sequences to blacklist
                            if (item.sequences.isNotEmpty()) {
                                item.sequences.forEach {
                                    val sequenceName = it.name!!.trim().lowercase().substring(17,it.name!!.trim().length - 1)
                                        BlacklistSequences.instance.addValue(sequenceName)
                                    Toast.makeText(
                                        App.instance,
                                        "Добавляю серию в ЧС: $sequenceName",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                applyFilters()
                            }
                            return@setOnLongClickListener true
                        }
                    }

                    Grammar.getAvailableDownloadFormats(item, binding.availableLinkFormats)
                    binding.availableLinkFormats.visibility = View.VISIBLE
                    binding.availableLinkFormats.setOnClickListener {
                        Toast.makeText(
                            App.instance,
                            "Это список доступных форматов. Скачать книгу можно нажав на кнопку нниже",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    binding.firstGroup.visibility = View.VISIBLE
                    binding.secondGroup.visibility = View.VISIBLE
                    binding.thirdGroup.visibility = View.VISIBLE
                    binding.centerActionBtn.visibility = View.VISIBLE
                    if (PreferencesHandler.instance.isEInk) {
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
                    } else {
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
                        centerItemPressed = binding.item
                        delegate.authorClicked(item)
                    }
                    binding.secondInfoBlockRightParam.setOnClickListener {
                        menuClicked = binding.item
                        centerItemPressed = binding.item
                        delegate.sequenceClicked(item)
                    }
                    binding.name.setOnClickListener {
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
                    if (PreferencesHandler.instance.isPreviews) {
                        // гружу обложку
                        binding.previewImage.visibility = View.VISIBLE
                        if (item.coverUrl == null) {
                            binding.previewImage.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    App.instance.resources,
                                    R.drawable.no_cover,
                                    null
                                )
                            )
                        } else {
                            if (item.cover != null && item.cover!!.isFile && item.cover!!.exists() && item.cover!!.canRead()) {
                                binding.previewImage.setImageBitmap(BitmapFactory.decodeFile(item.cover!!.path))
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
                                            if (item.cover != null && item.cover!!.isFile && item.cover!!.exists() && item.cover!!.canRead()) {
                                                binding.previewImage.setImageBitmap(
                                                    BitmapFactory.decodeFile(
                                                        item.cover!!.path
                                                    )
                                                )
                                                timer?.cancel()
                                            }
                                        }

                                        override fun onFinish() {
                                        }
                                    }
                                timer?.start()
                            }
                        }
                    } else {
                        // скрою окно иконки
                        binding.previewImage.visibility = View.GONE
                    }
                }
                TYPE_AUTHOR -> {
                    if (PreferencesHandler.instance.isFilterByLongClick()) {
                        binding.name.setOnLongClickListener {
                            Toast.makeText(
                                App.instance,
                                "Добавляю автора в ЧС: ${item.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            BlacklistAuthors.instance.addValue(item.name!!.trim().lowercase())
                            applyFilters()
                            return@setOnLongClickListener true
                        }
                    }
                    binding.availableLinkFormats.visibility = View.GONE
                    if (PreferencesHandler.instance.isHideButtons()) {
                        binding.centerActionBtn.visibility = View.GONE
                        binding.rootView.setOnClickListener {
                            centerItemPressed = binding.item
                            delegate.itemPressed(item)
                        }
                    } else {
                        binding.centerActionBtn.visibility = View.VISIBLE
                    }
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
                    if (PreferencesHandler.instance.isFilterByLongClick()) {
                        binding.name.setOnLongClickListener {
                            Toast.makeText(
                                App.instance,
                                "Добавляю серию в ЧС: ${item.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            BlacklistSequences.instance.addValue(item.name!!.trim().lowercase())
                            applyFilters()
                            return@setOnLongClickListener true
                        }
                    }
                    binding.availableLinkFormats.visibility = View.GONE
                    if (PreferencesHandler.instance.isHideButtons()) {
                        binding.centerActionBtn.visibility = View.GONE
                        binding.rootView.setOnClickListener {
                            centerItemPressed = binding.item
                            delegate.itemPressed(item)
                        }
                    } else {
                        binding.centerActionBtn.visibility = View.VISIBLE
                    }
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
                    if (PreferencesHandler.instance.isFilterByLongClick()) {
                        binding.name.setOnLongClickListener {
                            Toast.makeText(
                                App.instance,
                                "Добавляю жанр в ЧС: ${item.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d("surprise", "bind: add to blacklist '${item.name!!.trim().lowercase()}'")
                            BlacklistGenres.instance.addValue(item.name!!.trim().lowercase())
                            applyFilters()
                            return@setOnLongClickListener true
                        }
                    }
                    binding.availableLinkFormats.visibility = View.GONE
                    if (PreferencesHandler.instance.isHideButtons()) {
                        binding.centerActionBtn.visibility = View.GONE
                        binding.rootView.setOnClickListener {
                            centerItemPressed = binding.item
                            delegate.itemPressed(item)
                        }
                    } else {
                        binding.centerActionBtn.visibility = View.VISIBLE
                    }
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
                    binding.availableLinkFormats.visibility = View.GONE
                    if (PreferencesHandler.instance.isHideButtons()) {
                        binding.centerActionBtn.visibility = View.GONE
                        binding.rootView.setOnClickListener {
                            centerItemPressed = binding.item
                            delegate.itemPressed(item)
                        }
                    } else {
                        binding.centerActionBtn.visibility = View.VISIBLE
                    }
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
            binding.rootView.visibility = View.VISIBLE
            binding.root.background =
                ResourcesCompat.getDrawable(
                    App.instance.resources,
                    R.drawable.genre_layout,
                    null
                )
            binding.firstGroup.visibility = View.GONE
            binding.secondGroup.visibility = View.GONE
            binding.thirdGroup.visibility = View.GONE
            binding.previewImage.visibility = View.GONE
            binding.menuButton.visibility = View.GONE
            binding.name.visibility = View.GONE
            if (PreferencesHandler.instance.isShowLoadMoreBtn()) {
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
            } else {
                binding.centerActionBtn.visibility = View.GONE
                binding.availableLinkFormats.visibility = View.GONE
                binding.loadingMoreBar.visibility = View.VISIBLE
            }
        }

        fun bindInvisible() {
            binding.rootView.visibility = View.GONE
        }
    }

    fun getList(): ArrayList<FoundedEntity> {
        return values
    }

    fun bookLoaded(book: BooksDownloadSchedule?) {
        if (book != null) {
            var position: Int = -1
            values.forEach {
                if (it.id == book.bookId) {
                    position = values.lastIndexOf(it)
                }
            }
            if (position >= 0 && values.size > position) {
                if (PreferencesHandler.instance.isHideDownloaded) {
                    values.removeAt(position)
                    notifyItemRemoved(position)
                } else {
                    values[position].downloaded = true
                    notifyItemChanged(position)
                }
            }
        }
    }


    fun bookRead(book: FoundedEntity) {
        var position: Int = -1
        values.forEach {
            if (it.id == book.id) {
                position = values.lastIndexOf(it)
            }
        }
        if (position >= 0 && values.size > position) {
            if (PreferencesHandler.instance.isHideRead) {
                values.removeAt(position)
                notifyItemRemoved(position)
            } else {
                values[position].read = true
                notifyItemChanged(position)
            }
        }
    }

    fun bookDownloaded(book: FoundedEntity) {
        var position: Int = -1
        values.forEach {
            if (it.id == book.id) {
                position = values.lastIndexOf(it)
            }
        }
        if (position >= 0 && values.size > position) {
            if (PreferencesHandler.instance.isHideDownloaded) {
                values.removeAt(position)
                notifyItemRemoved(position)
            } else {
                values[position].downloaded = true
                notifyItemChanged(position)
            }
        }
    }

    fun sortList(sortOption: Int) {
        Log.d("surprise", "sortList: sort option is $sortOption")
        if (values.isNotEmpty()) {
            try {
                // отсортирую в зависимости от типа
                when (values[0].type!!) {
                    TYPE_BOOK -> SortHandler.sortBooks(values, sortOption, lastSortOption)
                    TYPE_AUTHOR -> SortHandler.sortAuthors(values, sortOption)
                    else -> SortHandler.sortList(values, sortOption)
                }
                notifyItemRangeChanged(0, values.size)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                Toast.makeText(
                    App.instance,
                    "Не удалось отсортировать значения. Отправьте мне отчёт, указав, как получили эту ошибку",
                    Toast.LENGTH_LONG
                ).show()
                val work = OneTimeWorkRequest.Builder(SendLogWorker::class.java).build()
                WorkManager.getInstance(App.instance).enqueue(work)
            }
        }
        lastSortOption = if (lastSortOption == sortOption) {
            -1
        } else {
            sortOption
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
            Log.d("surprise", "i have books on start: ${arrayList.size}")
            values = arrayList
        }
    }

    fun setHasNext(isNext: Boolean) {
        hasNext = isNext
    }

    fun markClickedElement(clickedElementIndex: Int) {
        if (clickedElementIndex < values.size) {
            values[clickedElementIndex].selected = true
            notifyItemChanged(clickedElementIndex)
        }
    }

    fun getSize(): Int {
        return values.size
    }

    fun hasBooks(): Boolean {
        if (values.isNotEmpty()) {
            values.forEach {
                if (it.type == TYPE_BOOK) {
                    return true
                }
            }
        }
        return false
    }

    fun dropSelected() {
        values.forEach {
            it.selected = false
        }
    }

    private fun applyFilters() {
        val iterator = values.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (!Filter.check(item).result) {
                // remove item from list
                val itemIndex = values.indexOf(item)
                iterator.remove()
                notifyItemRemoved(itemIndex)
            }
        }
    }

    var isScrolledToLast: Boolean = false
}