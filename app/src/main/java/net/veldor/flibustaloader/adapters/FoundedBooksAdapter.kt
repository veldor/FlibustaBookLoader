package net.veldor.flibustaloader.adapters

import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BR
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.database.dao.DownloadedBooksDao
import net.veldor.flibustaloader.database.dao.ReadedBooksDao
import net.veldor.flibustaloader.databinding.SearchedBookWithPreviewItemBinding
import net.veldor.flibustaloader.interfaces.MyAdapterInterface
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.FoundedBook
import net.veldor.flibustaloader.ui.OPDSActivity
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.SortHandler
import java.util.*

class FoundedBooksAdapter(arrayList: ArrayList<FoundedBook>) :
    RecyclerView.Adapter<FoundedBooksAdapter.ViewHolder?>(), MyAdapterInterface {
    private var mBooks: ArrayList<FoundedBook> = ArrayList<FoundedBook>()
    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(App.instance.applicationContext)
    private val mDao: DownloadedBooksDao
    private val mReadDao: ReadedBooksDao
    private var mCurrentLink: DownloadLink? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = SearchedBookWithPreviewItemBinding.inflate(mLayoutInflater)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        if (i == mBooks.size && PreferencesHandler.instance.isShowLoadMoreBtn() || i == mBooks.size - 1 && !PreferencesHandler.instance.isShowLoadMoreBtn()
        ) {
            val params: RecyclerView.LayoutParams =
                viewHolder.itemView.layoutParams as RecyclerView.LayoutParams
            params.bottomMargin = 300
            viewHolder.itemView.layoutParams = params
        } else {
            val params: RecyclerView.LayoutParams =
                viewHolder.itemView.layoutParams as RecyclerView.LayoutParams
            params.bottomMargin = 30
            viewHolder.itemView.layoutParams = params
        }
        if (i < mBooks.size) {
            viewHolder.bind(mBooks[i])
        } else {
            viewHolder.bindLoadMoreBtn()
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.cancelImageLoad()
    }

    override fun getItemCount(): Int {
        return if (PreferencesHandler.instance.isShowLoadMoreBtn() && !PreferencesHandler.instance.isDownloadAll && OPDSActivity.sNextPage != null
        ) {
            mBooks.size + 1
        } else {
            mBooks.size
        }
    }

    fun sort() {
        SortHandler.sortBooks(mBooks)
        notifyItemRangeChanged(0, mBooks.size)
        Toast.makeText(App.instance.applicationContext, "Книги отсортированы!", Toast.LENGTH_SHORT)
            .show()
    }

    fun setContent(newData: ArrayList<FoundedBook>?, addToLoaded: Boolean) {
        if (newData == null) {
            if (!addToLoaded) {
                mBooks = ArrayList<FoundedBook>()
                OPDSActivity.sBooksForDownload = mBooks
                notifyItemRangeChanged(0, mBooks.size)
            }
        } else if (newData.size == 0 && mBooks.size == 0) {
            Toast.makeText(App.instance.applicationContext, "Книги не найдены", Toast.LENGTH_SHORT)
                .show()
            notifyItemRangeChanged(0, 0)
        } else {
            // если выбрана загрузка всех результатов сразу- добавлю вновь пришедшие к уже существующим
            if (PreferencesHandler.instance.isDownloadAll || addToLoaded) {
                val previousArrayLen = mBooks.size
                mBooks.addAll(newData)
                OPDSActivity.sBooksForDownload = mBooks
                if (OPDSActivity.sNextPage != null) {
                    notifyItemRangeInserted(previousArrayLen, newData.size)
                } else {
                    notifyItemRangeChanged(0, mBooks.size)
                }
            } else {
                mBooks = newData
                OPDSActivity.sBooksForDownload = mBooks
                notifyItemRangeChanged(0, mBooks.size)
            }
        }
    }

    fun bookDownloaded(bookId: String) {
        for (f in mBooks) {
            if (f.id == bookId) {
                f.downloaded = true
                if (PreferencesHandler.instance.isHideDownloaded) {
                    mBooks.remove(f)
                    notifyItemRemoved(mBooks.lastIndexOf(f))
                } else {
                    notifyItemChanged(mBooks.lastIndexOf(f))
                }
                break
            }
        }
    }

    fun setBookReaded(book: FoundedBook) {
        book.read = true
        if (mBooks.contains(book)) {
            val bookIndex = mBooks.lastIndexOf(book)
            // если выбрано скрытие прочитанных книг- удалю её
            if (PreferencesHandler.instance.isHideRead) {
                mBooks.removeAt(bookIndex)
                notifyItemRemoved(bookIndex)
            } else {
                notifyItemChanged(bookIndex)
            }
        }
    }

    fun hideReaded() {
        val newList: ArrayList<FoundedBook> = ArrayList<FoundedBook>()
        // пройдусь по списку и удалю все прочитанные книги
        if (mBooks.size > 0) {
            for (book in mBooks) {
                if (!book.read) {
                    newList.add(book)
                }
            }
            mBooks = newList
            notifyItemRangeChanged(0, mBooks.size)
        }
    }

    fun hideDownloaded() {
        val newList: ArrayList<FoundedBook> = ArrayList<FoundedBook>()
        // пройдусь по списку и удалю все прочитанные книги
        if (mBooks.size > 0) {
            for (book in mBooks) {
                if (!book.downloaded) {
                    newList.add(book)
                }
            }
            mBooks = newList
            notifyItemRangeChanged(0, mBooks.size)
        }
    }

    val items: ArrayList<FoundedBook>
        get() = mBooks

    override fun clearList() {
        // удалю книги, если не было подгрузки результатов
        if (PreferencesHandler.instance.isDownloadAll) {
            mBooks = ArrayList<FoundedBook>()
            notifyItemRangeChanged(0, mBooks.size)
        }
    }

    fun showReaded(booksList: ArrayList<FoundedBook>) {
        mBooks = booksList
        notifyItemRangeChanged(0, mBooks.size)
    }

    fun showDownloaded(booksList: ArrayList<FoundedBook>) {
        mBooks = booksList
        notifyItemRangeChanged(0, mBooks.size)
    }

    inner class ViewHolder(binding: SearchedBookWithPreviewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val mBinding: SearchedBookWithPreviewItemBinding = binding
        private lateinit var mBook: FoundedBook
        fun bind(foundedBook: FoundedBook) {
            mBook = foundedBook
            mBinding.setVariable(BR.book, mBook)
            mBinding.executePendingBindings()
            mBinding.contentGroup.visibility = View.VISIBLE
            mBinding.downloadBookBtn.text =
                App.instance.applicationContext.getString(R.string.download_title)
            mBinding.downloadBookBtn.setTextColor(
                ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.book_name_color,
                    null
                )
            )
            mBinding.downloadBookBtn.setOnClickListener {
                if (mBook.downloadLinks.size > 1) {
                    val savedMime: String? = PreferencesHandler.instance.favoriteMime
                    if (savedMime != null && savedMime.isNotEmpty()) {
                        // проверю, нет ли в списке выбранного формата
                        for (dl in mBook.downloadLinks) {
                            mCurrentLink = dl
                            if (dl.mime!!.contains(savedMime)) {
                                val result: ArrayList<DownloadLink> =
                                    ArrayList<DownloadLink>()
                                result.add(mCurrentLink!!)
                                App.instance.mDownloadLinksList.postValue(result)
                                return@setOnClickListener
                            }
                        }
                    }
                }
                App.instance.mDownloadLinksList.postValue(mBook.downloadLinks)
            }
            mBinding.downloadBookBtn.setOnLongClickListener {
                // если ссылка на скачивание одна- скачаю книгу, если несколько- выдам диалоговое окно со списком форматов для скачивания
                App.instance.mDownloadLinksList.postValue(mBook.downloadLinks)
                true
            }
            // если включено отображение превью книг и превью существует
            if (PreferencesHandler.instance.isPreviews && foundedBook.previewUrl != null) {
                mBinding.previewImage.visibility = View.VISIBLE
                // загружу изображение с помощью GLIDE
                Glide
                    .with(mBinding.previewImage)
                    .load(PreferencesHandler.instance.picMirror + foundedBook.previewUrl)
                    .into(mBinding.previewImage)
            } else {
                mBinding.previewImage.visibility = View.GONE
            }
            Handler().post {
                // проверю, если книга прочитана- покажу это
                if (mReadDao.getBookById(foundedBook.id) != null) {
                    mBinding.bookRead.visibility = View.VISIBLE
                } else {
                    mBinding.bookRead.visibility = View.GONE
                }
                // проверю, если книга прочитана- покажу это
                if (mDao.getBookById(foundedBook.id) != null) {
                    mBinding.bookDownloaded.visibility = View.VISIBLE
                } else {
                    mBinding.bookDownloaded.visibility = View.GONE
                }
            }
        }

        fun cancelImageLoad() {
            if (PreferencesHandler.instance.isPreviews && mBook.previewUrl != null) {
                Glide.with(mBinding.previewImage)
                    .clear(mBinding.previewImage)
            }
        }

        fun bindLoadMoreBtn() {

            mBinding.contentGroup.visibility = View.GONE
            mBinding.bookRead.visibility = View.GONE
            mBinding.bookDownloaded.visibility = View.GONE
            mBinding.previewImage.visibility = View.GONE


            mBinding.downloadBookBtn.setTextColor(
                ResourcesCompat.getColor(App.instance.resources, R.color.genre_text_color, null)
            )
            mBinding.downloadBookBtn.text = "Загрузить следующую страницу"
            mBinding.downloadBookBtn.setOnClickListener {
                OPDSActivity.sLoadNextPage.postValue(
                    true
                )
            }
        }

        init {
            mBinding.bookRead.setOnClickListener {
                Toast.makeText(
                    App.instance.applicationContext,
                    "Книга отмечена как прочитанная",
                    Toast.LENGTH_LONG
                ).show()
            }
            mBinding.bookDownloaded.setOnClickListener {
                Toast.makeText(
                    App.instance.applicationContext,
                    "Книга уже скачивалась",
                    Toast.LENGTH_LONG
                ).show()
            }

            // добавлю отображение информации о книге при клике на название книги

            mBinding.bookName.setOnClickListener {
                App.instance.mSelectedBook.postValue(
                    mBook
                )
            }

            mBinding.menuButton.setOnClickListener {
                // отправлю событие контекстного меню для книги
                App.instance.mContextBook.postValue(mBook)
            }
            // добавлю действие на нажатие на автора

            mBinding.authorName.setOnClickListener {
                // если автор один- вывожу диалог выбора отображения, если несколько- вывожу диалог выбора автора
                if (mBook.authors.size > 1) {
                    App.instance.mSelectedAuthors.postValue(mBook.authors)
                } else if (mBook.authors.size == 1) {
                    App.instance.mSelectedAuthor.postValue(mBook.authors[0])
                }
            }
            // добавлю действие поиска по серии

            mBinding.sequence.setOnClickListener {
                if (mBook.sequences.size > 0) {
                    if (mBook.sequences.size > 1) {
                        App.instance.mSelectedSequences.postValue(mBook.sequences)
                    } else {
                        App.instance.mSelectedSequence.postValue(mBook.sequences[0])
                    }
                }
            }
            mBinding.previewImage.setOnClickListener {
                App.instance.mShowCover.postValue(
                    mBook
                )
            }

            mBinding.bookDownloaded.visibility = View.INVISIBLE
            mBinding.bookRead.visibility = View.INVISIBLE
        }
    }

    init {
        if (arrayList.size > 0) {
            mBooks = arrayList
            OPDSActivity.sBooksForDownload = mBooks
        }
        mDao = App.instance.mDatabase.downloadedBooksDao()
        mReadDao = App.instance.mDatabase.readBooksDao()
    }
}