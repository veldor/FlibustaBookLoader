package net.veldor.flibustaloader.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.FoundedItemAdapter
import net.veldor.flibustaloader.databinding.FragmentOpdsBinding
import net.veldor.flibustaloader.delegates.FoundedItemActionDelegate
import net.veldor.flibustaloader.parsers.TestParser
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.ui.BaseActivity
import net.veldor.flibustaloader.ui.BlacklistActivity
import net.veldor.flibustaloader.ui.BrowserActivity
import net.veldor.flibustaloader.utils.*
import net.veldor.flibustaloader.view_models.OPDSViewModel
import java.lang.reflect.Field
import java.net.URLEncoder
import java.util.*


class OpdsFragment : Fragment(), SearchView.OnQueryTextListener, FoundedItemActionDelegate,
    View.OnCreateContextMenuListener {

    private lateinit var binding: FragmentOpdsBinding
    lateinit var viewModel: OPDSViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity?.invalidateOptionsMenu()
        viewModel = ViewModelProvider(this).get(OPDSViewModel::class.java)
        binding = FragmentOpdsBinding.inflate(inflater, container, false)
        val root = binding.root
        autocompleteStrings = viewModel.searchAutocomplete
        setHasOptionsMenu(true)
        setupInterface()
        setupObservers()

        // если передана ссылка- перейду по ней
        val intent = requireActivity().intent
        val link = intent.getStringExtra(TARGET_LINK)
        if (link != null) {
            load(link, append = false, addToHistory = true, -1)
        } else if (viewModel.getCurrentPage() != null) {
            load(
                viewModel.getCurrentPage()!!,
                append = false,
                addToHistory = false,
                clickedElementIndex = -1
            )
        }
        return root
    }

    // EXPORTED

    private var lastScrolled: Int = 0
    private var mSearchView: SearchView? = null
    private lateinit var autocompleteStrings: ArrayList<String>
    private var mSearchAdapter: ArrayAdapter<String>? = null
    private val mAuthorViewTypes = arrayOf(
        "Книги по сериям",
        "Книги вне серий",
        "Книги по алфавиту",
        "Книги по дате поступления"
    )
    private lateinit var mSearchAutoComplete: SearchView.SearchAutoComplete
    var mBookTypeDialog: AlertDialog? = null
    var mCoverPreviewDialog: Dialog? = null
    private var mCurrentPage = 0


    private fun setupObservers() {
        // получена прямая ссылка для поиска
        sLiveSearchLink.observe(viewLifecycleOwner, { s: String? ->
            if (s != null && s.isNotEmpty()) {
                load(s, append = false, addToHistory = true, -1)
            }
        })
        viewModel.searchResults.observe(viewLifecycleOwner, {
            lastScrolled = 0
            if (it.type == TestParser.TYPE_BOOK && (it.results.size > 0 || it.appended)) {
                // покажу кнопку скачивания всех книг
                binding.floatingMenu.visibility = View.VISIBLE
            } else {
                binding.floatingMenu.visibility = View.GONE
            }
            if (it.nextPageLink == null || (PreferencesHandler.instance.opdsPagedResultsLoad && it.type == TestParser.TYPE_BOOK)) {
                binding.progressBar.visibility = View.INVISIBLE
            }

            if (it.appended) {
                if (binding.resultsCount.text.isDigitsOnly()) {
                    val previousSize = binding.resultsCount.text.toString().toInt()
                    binding.resultsCount.text = (it.size + previousSize).toString()
                } else {
                    binding.resultsCount.text = (it.size).toString()
                }
                val previousSize: Int = if (binding.filteredCount.text.isDigitsOnly()) {
                    binding.filteredCount.text.toString().toInt()
                } else {
                    0
                }
                if (previousSize > 0 || it.filtered > 0) {
                    binding.resultsCount.text = (it.filtered + previousSize).toString()
                    binding.filteredCount.visibility = View.VISIBLE
                }
                (binding.resultsList.adapter as FoundedItemAdapter).appendContent(it.results)
            } else {
                if (it.filtered > 0) {
                    binding.filteredCount.visibility = View.VISIBLE
                    binding.filteredCount.text = it.filtered.toString()
                }
                binding.resultsCount.visibility = View.VISIBLE
                binding.resultsCount.text = it.size.toString()
                (binding.resultsList.adapter as FoundedItemAdapter).setContent(it.results)
                binding.resultsList.scrollToPosition(0)
            }
            // set next page
            sNextPage = if (it.nextPageLink.isNullOrEmpty()) {
                (binding.resultsList.adapter as FoundedItemAdapter).setHasNext(false)
                null
            } else {
                (binding.resultsList.adapter as FoundedItemAdapter).setHasNext(true)
                it.nextPageLink!!
            }
            // если нет результатов- заявлю об этом
            if (it.size == 0) {
                Toast.makeText(requireContext(), "Поиск завершен", Toast.LENGTH_LONG).show()
            }
            if (it.isBackSearch && it.clickedElementIndex >= 0) {
                // проверю, что элемент входит в выборку
                if ((binding.resultsList.adapter as FoundedItemAdapter).getSize() >= it.clickedElementIndex &&
                    !(binding.resultsList.adapter as FoundedItemAdapter).isScrolledToLast
                ) {
                    (binding.resultsList.adapter as FoundedItemAdapter).isScrolledToLast = true
                    binding.resultsList.scrollToPosition(it.clickedElementIndex)
                    (binding.resultsList.adapter as FoundedItemAdapter).markClickedElement(it.clickedElementIndex)
                }
            }
        })

        viewModel.isLoadError.observe(viewLifecycleOwner, { hasError: Boolean ->
            if (hasError) {
                binding.progressBar.visibility = View.INVISIBLE
                // покажу окошко и сообщу, что загрузка не удалась
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_load_page_message),
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        })

        App.instance.liveBookJustLoaded.observe(viewLifecycleOwner, {
            (binding.resultsList.adapter as FoundedItemAdapter).bookLoaded(it)
        })
    }


    fun setupInterface() {
        binding.mirrorUsed.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.used_mirror_message),
                Toast.LENGTH_LONG
            ).show()
        }
        // обработаю клик на кнопку отображения списка авторов

        binding.showAuthorsListButton.setOnClickListener {
            load("/opds/authorsindex", append = false, addToHistory = true, -1)
            showLoadWaiter()
        }

        // навешу обработчик на переключатель типа поиска
        binding.searchType.setOnCheckedChangeListener { _, which ->
            binding.showAllSwitcher.visibility = View.GONE
            binding.showAuthorsListButton.visibility = View.GONE
            when (which) {
                R.id.searchBook -> {
                    binding.showAllSwitcher.visibility = View.VISIBLE
                    if (PreferencesHandler.instance.isAutofocusSearch()) {
                        mSearchView?.isIconified = false
                        mSearchView?.requestFocus()
                    }
                }
                R.id.searchAuthor -> {
                    binding.showAuthorsListButton.visibility = View.VISIBLE
                    if (PreferencesHandler.instance.isAutofocusSearch()) {
                        mSearchView?.isIconified = false
                        mSearchView?.requestFocus()
                    }
                }
                R.id.searchGenre -> {
                    mSearchView?.isIconified = true
                    load("/opds/genres", false, addToHistory = true, -1)
                    showLoadWaiter()
                }
                R.id.searchSequence -> {
                    mSearchView?.isIconified = true
                    load("/opds/sequencesindex", false, addToHistory = true, -1)
                    showLoadWaiter()
                }
            }
        }
        binding.resultsList.adapter = FoundedItemAdapter(arrayListOf(), this)
//        binding.resultsList.recycledViewPool.setMaxRecycledViews(0, 0);
//        binding.resultsList.setItemViewCacheSize(50);
//        binding.resultsList.setDrawingCacheEnabled(true);
        if (PreferencesHandler.instance.isLinearLayout) {
            binding.resultsList.layoutManager = LinearLayoutManager(requireContext())
        } else {
            binding.resultsList.layoutManager = GridLayoutManager(requireContext(), 2)
        }

        binding.resultsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // проверю последний видимый элемент
                if (PreferencesHandler.instance.isLinearLayout) {
                    val manager = binding.resultsList.layoutManager as LinearLayoutManager?
                    if (manager != null) {
                        val adapter = binding.resultsList.adapter
                        if (adapter != null) {
                            val position = manager.findLastCompletelyVisibleItemPosition()
                            if (
                                position == adapter.itemCount - 1 &&
                                position > lastScrolled &&
                                !PreferencesHandler.instance.isShowLoadMoreBtn() &&
                                PreferencesHandler.instance.opdsPagedResultsLoad && sNextPage != null
                            ) {
                                // подгружу результаты
                                load(sNextPage!!, append = true, addToHistory = false, -1)
                            }
                            lastScrolled = position
                        }
                    }
                }
            }
        })

        // варианты поиска
        if (PreferencesHandler.instance.isEInk) {
            binding.searchGenre.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.searchBook.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.searchAuthor.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.searchSequence.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.connectionType.setBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.preferencesGroup.visibility = View.GONE
        } else {
            if (!PreferencesHandler.instance.isPicHide()) {
                // назначу фон
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    binding.rootView.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.back_2)
                } else {
                    binding.rootView.background =
                        ResourcesCompat.getDrawable(resources, R.drawable.back_2, null)
                }
            }
        }
        binding.showAllSwitcher.isChecked = PreferencesHandler.instance.opdsPagedResultsLoad
        binding.showAllSwitcher.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean ->
            PreferencesHandler.instance.opdsPagedResultsLoad =
                !PreferencesHandler.instance.opdsPagedResultsLoad
            requireActivity().invalidateOptionsMenu()
        }
        binding.floatingMenu.visibility = View.GONE
        binding.fabDownloadAll.setOnClickListener {
            val favoriteFormat = PreferencesHandler.instance.favoriteMime
            if (favoriteFormat != null && favoriteFormat.isNotEmpty()) {
                viewModel.downloadAll(
                    (binding.resultsList.adapter as FoundedItemAdapter).getList(),
                    favoriteFormat,
                    false,
                    strictFormat = true
                )
            } else {
                // покажу диалог выбора предпочтительнго типа скачивания
                // сброшу отслеживание удачного выбора типа книги
                selectBookTypeDialog(false)
            }
            binding.floatingMenu.close(true)
        }
        binding.fabDownloadUnloaded.setOnClickListener {
            val favoriteFormat = PreferencesHandler.instance.favoriteMime
            if (favoriteFormat != null && favoriteFormat.isNotEmpty()) {
                viewModel.downloadAll(
                    (binding.resultsList.adapter as FoundedItemAdapter).getList(),
                    favoriteFormat,
                    true,
                    strictFormat = true
                )
            } else {
                // покажу диалог выбора предпочтительнго типа скачивания
                selectBookTypeDialog(true)
            }
            binding.floatingMenu.close(true)
        }
    }

    fun scrollUp() {
        if (PreferencesHandler.instance.isLinearLayout) {
            val manager = binding.resultsList.layoutManager as LinearLayoutManager?
            if (manager != null) {
                val position = manager.findFirstCompletelyVisibleItemPosition()
                if (position > 0) {
                    manager.scrollToPositionWithOffset(position - 1, 10)
                }
            }
        }
    }

    fun scrollDown() {
        if (PreferencesHandler.instance.isLinearLayout) {
            val manager = binding.resultsList.layoutManager as LinearLayoutManager?
            if (manager != null) {
                var position = manager.findFirstCompletelyVisibleItemPosition()
                val adapter = binding.resultsList.adapter
                if (adapter != null) {
                    if (position < adapter.itemCount - 1) {
                        manager.scrollToPositionWithOffset(position + 1, 10)
                        position = manager.findLastCompletelyVisibleItemPosition()
                        if (
                            position == adapter.itemCount - 1 &&
                            PreferencesHandler.instance.opdsPagedResultsLoad && sNextPage != null &&
                            !PreferencesHandler.instance.isShowLoadMoreBtn()
                        ) {
                            mCurrentPage += 1
                            Log.d("surprise", "scrollDown: GO TO NEXT PAGE")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (PreferencesHandler.instance.isExternalVpn) {
            binding.connectionType.text = getString(R.string.vpn_title)
            binding.connectionType.setBackgroundColor(Color.parseColor("#03A9F4"))
        } else {
            binding.connectionType.text = getString(R.string.tor_title)
            binding.connectionType.setBackgroundColor(Color.parseColor("#4CAF50"))
        }

        binding.mirrorUsed.visibility = if (App.instance.useMirror) View.VISIBLE else View.GONE
        if (App.instance.useMirror) {
            Log.d("surprise", "onResume: using mirror")
        } else {
            Log.d("surprise", "onResume: no using mirror")
        }
    }

    @SuppressLint("RestrictedApi", "DiscouragedPrivateApi")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        requireActivity().menuInflater.inflate(R.menu.odps_menu, menu)

        // добавлю обработку поиска
        val searchMenuItem = menu.findItem(R.id.action_search)
        mSearchView = searchMenuItem.actionView as SearchView
        if (mSearchView != null) {
            if (PreferencesHandler.instance.isEInk) {
                val colorFilter = PorterDuffColorFilter(
                    ResourcesCompat.getColor(resources, R.color.black, null),
                    PorterDuff.Mode.MULTIPLY
                )
                mSearchView!!.queryHint = ""
                mSearchView!!.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
                    ?.setTextColor(Color.BLACK)
                mSearchView!!.findViewById<ImageView>(androidx.appcompat.R.id.search_button)
                    ?.colorFilter = colorFilter
                mSearchView!!.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
                    ?.colorFilter = colorFilter

                val mCursorDrawableRes: Field =
                    TextView::class.java.getDeclaredField("mCursorDrawableRes")
                mCursorDrawableRes.isAccessible = true
                mCursorDrawableRes.set(
                    mSearchView!!.findViewById<TextView>(androidx.appcompat.R.id.search_src_text),
                    R.drawable.cursor
                )

                val myItem = menu.findItem(R.id.action_sort_by)
                myItem?.icon?.colorFilter = colorFilter

                mSearchView?.setOnQueryTextFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        Log.d("surprise", "onCreateOptionsMenu: have focus")
                        binding.preferencesGroup.visibility = View.VISIBLE
                    } else {
                        binding.preferencesGroup.visibility = View.GONE
                    }
                }
            } else {
                mSearchView!!.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
                    ?.setTextColor(Color.WHITE)
            }
            mSearchView!!.inputType = InputType.TYPE_CLASS_TEXT
            val size = Point()
            requireActivity().windowManager.defaultDisplay.getSize(size)
            mSearchView!!.maxWidth = size.x - 340
            mSearchView!!.setOnQueryTextListener(this)
            mSearchView!!.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
                override fun onSuggestionSelect(i: Int): Boolean {
                    return true
                }

                override fun onSuggestionClick(i: Int): Boolean {
                    val value = autocompleteStrings[i]
                    mSearchView!!.setQuery(value, false)
                    return true
                }
            })
            mSearchAutoComplete = mSearchView!!.findViewById(R.id.search_src_text)!!
            mSearchAutoComplete.setDropDownBackgroundResource(R.color.background_color)
            mSearchAutoComplete.threshold = 0
            mSearchAutoComplete.dropDownHeight = ViewGroup.LayoutParams.WRAP_CONTENT
            mSearchAdapter =
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    autocompleteStrings
                )
            mSearchAutoComplete.setAdapter(mSearchAdapter)
        }
        var myItem = menu.findItem(R.id.menuUseDarkMode)
        myItem.isChecked = viewModel.nightModeEnabled


        // обработаю переключатель быстрой загрузки
        myItem = menu.findItem(R.id.discardFavoriteType)
        myItem.isEnabled = PreferencesHandler.instance.favoriteMime != null

        // обработаю переключатель загрузки всех результатов поиска книг
        myItem = menu.findItem(R.id.menuLoadAllBooks)
        Log.d(
            "surprise",
            "onCreateOptionsMenu: is checked? " + PreferencesHandler.instance.opdsPagedResultsLoad
        )
        myItem.isChecked = PreferencesHandler.instance.opdsPagedResultsLoad

        // переключатель превью обложек
        myItem = menu.findItem(R.id.showPreviews)
        myItem.isChecked = PreferencesHandler.instance.isPreviews

        // обработаю переключатель скрытия прочтённых книг
        val hideReadSwitcher = menu.findItem(R.id.hideReadSwitcher)
        hideReadSwitcher.isChecked = PreferencesHandler.instance.isHideRead
        myItem = menu.findItem(R.id.hideDigests)
        myItem.isChecked = PreferencesHandler.instance.isHideDigests
        myItem = menu.findItem(R.id.hideDownloadedSwitcher)
        myItem.isChecked = PreferencesHandler.instance.isHideDownloaded
        myItem = menu.findItem(R.id.menuCreateAuthorDir)
        myItem.isChecked = PreferencesHandler.instance.isCreateAuthorsDir()
        myItem = menu.findItem(R.id.menuCreateSequenceDir)
        myItem.isChecked = PreferencesHandler.instance.isCreateSequencesDir()
        myItem = menu.findItem(R.id.useFilter)
        myItem.isChecked = PreferencesHandler.instance.isUseFilter
    }

    private fun selectSorting() {
        // проверю, загружены ли данные
        val data = (binding.resultsList.adapter as FoundedItemAdapter).getList()
        if (data.isEmpty()) {
            Toast.makeText(requireContext(), "Нет результатов для сортировки", Toast.LENGTH_LONG)
                .show()
        } else {
            // проверю тип загруженных данных
            val type = data[0].type
            val sortingOptions: Array<String> = when (type!!) {
                TestParser.TYPE_BOOK -> bookSortOptions
                TestParser.TYPE_AUTHOR -> authorSortOptions
                else -> otherSortOptions
            }
            val dialog = AlertDialog.Builder(requireContext(), R.style.MyDialogStyle)
            dialog.setTitle("Выберите тип сортировки")
                .setItems(sortingOptions) { _: DialogInterface?, which: Int ->
                    (binding.resultsList.adapter as FoundedItemAdapter).sortList(
                        which
                    )
                }
            dialog.show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_new) {
            showNewDialog()
            return true
        }
        if (id == R.id.action_bookmark) {
            addBookmark()
            return true
        }
        if (id == R.id.menuUseDarkMode) {
            viewModel.switchNightMode()
            Handler().postDelayed(BaseActivity.ResetApp(), 100)
            return true
        }

        if (id == R.id.hideDigests) {
            PreferencesHandler.instance.isHideDigests = !PreferencesHandler.instance.isHideDigests
            requireActivity().invalidateOptionsMenu()
            if (PreferencesHandler.instance.isHideDigests) {
                Toast.makeText(
                    requireContext(),
                    "Скрываю книги, у которых больше 3 авторов",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return true
        }
        if (id == R.id.hideDownloadedSwitcher) {
            PreferencesHandler.instance.isHideDownloaded =
                !PreferencesHandler.instance.isHideDownloaded
            requireActivity().invalidateOptionsMenu()
            if (PreferencesHandler.instance.isHideDownloaded) {
                Toast.makeText(
                    requireContext(),
                    "Скрываю ранее скачанные книги",
                    Toast.LENGTH_SHORT
                ).show()
                hideDownloadedBooks()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Показываю ранее скачанные книги",
                    Toast.LENGTH_SHORT
                ).show()
                showDownloadedBooks()
            }
            return true
        }
        if (id == R.id.hideReadSwitcher) {
            PreferencesHandler.instance.isHideRead = !PreferencesHandler.instance.isHideRead
            // скрою книги, если они есть
            if (PreferencesHandler.instance.isHideRead) {
                Toast.makeText(requireContext(), "Скрываю прочитанные книги", Toast.LENGTH_SHORT)
                    .show()
                hideReadedBooks()
            } else {
                Toast.makeText(requireContext(), "Отображаю прочитанные книги", Toast.LENGTH_SHORT)
                    .show()
                showReadedBooks()
            }
            requireActivity().invalidateOptionsMenu()
            return true
        }
        if (id == R.id.action_sort_by) {
            selectSorting()
            return true
        }
        if (id == R.id.menuLoadAllBooks) {
            binding.showAllSwitcher.toggle()
            requireActivity().invalidateOptionsMenu()
            return true
        }
        if (id == R.id.clearSearchHistory) {
            clearHistory()
            return true
        }
        if (id == R.id.discardFavoriteType) {
            PreferencesHandler.instance.favoriteMime = null
            Toast.makeText(requireContext(), "Выбранный тип загрузок сброшен", Toast.LENGTH_SHORT)
                .show()
            return true
        }
        if (id == R.id.showPreviews) {
            PreferencesHandler.instance.isPreviews
            requireActivity().invalidateOptionsMenu()
            return true
        }
        if (id == R.id.switchLayout) {
            PreferencesHandler.instance.isLinearLayout = !PreferencesHandler.instance.isLinearLayout
            requireActivity().recreate()
        }
        if (id == R.id.menuCreateAuthorDir) {
            PreferencesHandler.instance.setCreateAuthorsDir(!item.isChecked)
            requireActivity().invalidateOptionsMenu()
            if (PreferencesHandler.instance.isCreateAuthorsDir()) {
                Toast.makeText(
                    requireContext(),
                    "Создаю папки для отдельных авторов",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Не создаю папки для отдельных авторов",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return true
        }
        if (id == R.id.menuCreateSequenceDir) {
            PreferencesHandler.instance.setCreateSequencesDir(!item.isChecked)
            requireActivity().invalidateOptionsMenu()
            if (PreferencesHandler.instance.isCreateSequencesDir()) {
                Toast.makeText(
                    requireContext(),
                    "Создаю папки для отдельных серий",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Не создаю папки для отдельных серий",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return true
        }
        if (id == R.id.useFilter) {
            PreferencesHandler.instance.isUseFilter = !item.isChecked
            requireActivity().invalidateOptionsMenu()
            if (PreferencesHandler.instance.isUseFilter) {
                startActivity(Intent(requireContext(), BlacklistActivity::class.java))
            } else {
                Toast.makeText(
                    requireContext(),
                    "Фильтрация результатов не применяется",
                    Toast.LENGTH_SHORT
                ).show()
                showDownloadedBooks()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addBookmark() {
        // покажу диалоговое окно с предложением подтвердить название закладки
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.MyDialogStyle)
        val view = layoutInflater.inflate(R.layout.dialog_bookmark_name, null)
        val input = view.findViewById<EditText>(R.id.bookmarkNameInput)
        input.setText(sBookmarkName)
        dialogBuilder.setTitle("Название закладки")
            .setView(view)
            .setPositiveButton("Сохранить") { _: DialogInterface?, _: Int ->
                val bookmarkText = input.text.toString()
                viewModel.saveBookmark(bookmarkText)
            }
            .setNegativeButton(android.R.string.no, null)
            .show()
    }

    private fun showReadedBooks() {

    }

    private fun showDownloadedBooks() {

    }

    private fun hideReadedBooks() {

    }

    private fun hideDownloadedBooks() {

    }

    private fun clearHistory() {
        viewModel.clearHistory()
        autocompleteStrings = ArrayList()
        mSearchAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, autocompleteStrings)
        mSearchAutoComplete.setAdapter(mSearchAdapter)
        Toast.makeText(requireContext(), "Автозаполнение сброшено", Toast.LENGTH_SHORT).show()
    }


    private fun selectBookTypeDialog(onlyNotLoaded: Boolean) {
        val inflate = layoutInflater
        @SuppressLint("InflateParams") val view =
            inflate.inflate(R.layout.confirm_book_type_select, null)
        val checker: SwitchCompat = view.findViewById(R.id.reDownload)
        checker.isChecked = PreferencesHandler.instance.isReDownload
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Выберите формат скачивания")
            .setItems(MimeTypes.MIMES_LIST) { dialogInterface: DialogInterface, i: Int ->
                val dialog = dialogInterface as Dialog
                var switcher: SwitchCompat = dialog.findViewById(R.id.save_type_selection)
                if (switcher.isChecked) {
                    // запомню выбор формата
                    Toast.makeText(
                        requireContext(),
                        "Предпочтительный формат для скачивания сохранён(" + MimeTypes.getFullMime(
                            MimeTypes.MIMES_LIST[i]
                        ) + "). Вы можете сбросить его в настройки +> разное.",
                        Toast.LENGTH_LONG
                    ).show()
                    PreferencesHandler.instance.favoriteMime =
                        MimeTypes.getFullMime(MimeTypes.MIMES_LIST[i])
                }
                switcher = dialog.findViewById(R.id.reDownload)
                PreferencesHandler.instance.isReDownload = switcher.isChecked
                viewModel.downloadAll(
                    (binding.resultsList.adapter as FoundedItemAdapter).getList(),
                    MimeTypes.getFullMime(MimeTypes.MIMES_LIST[i])!!,
                    onlyNotLoaded,
                    dialog.findViewById<SwitchCompat>(R.id.onlyThisType).isChecked
                )
                requireActivity().invalidateOptionsMenu()
            }
            .setView(view)
        dialogBuilder.show()
    }

    private fun showNewDialog() {
        val newCategory = arrayOf(
            "Все новинки",
            "Новые книги по жанрам",
            "Новые книги по авторам",
            "Новые книги по сериям"
        )
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.MyDialogStyle)
        dialogBuilder.setTitle(R.string.showNewDialogTitle)
            .setItems(newCategory) { _: DialogInterface?, which: Int ->
                when (which) {
                    0 -> {
                        load(
                            "/opds/new/0/new",
                            append = false,
                            addToHistory = true,
                            -1
                        )
                    }
                    1 -> {
                        load(
                            "/opds/newgenres",
                            append = false,
                            addToHistory = true,
                            -1
                        )
                    }
                    2 -> {
                        load(
                            "/opds/newauthors",
                            append = false,
                            addToHistory = true,
                            -1
                        )
                    }
                    3 -> {
                        load(
                            "/opds/newsequences",
                            append = false,
                            addToHistory = true,
                            -1
                        )
                    }
                }
                showLoadWaiter()
            }
            .show()
    }

    override fun onQueryTextSubmit(s: String): Boolean {
        if (binding.searchType.checkedRadioButtonId == R.id.searchSequence || binding.searchType.checkedRadioButtonId == R.id.searchSequence) {
            Toast.makeText(
                requireContext(),
                getString(R.string.this_search_not_accepted_message),
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        hideKeyboard(binding.rootView)
        if (!TextUtils.isEmpty(s.trim { it <= ' ' })) {
            binding.floatingMenu.visibility = View.GONE
            mSearchView?.onActionViewCollapsed()
            addValueToAutocompleteList(s)
            // ищу введённое значение
            scrollToTop()
            val searchString = URLEncoder.encode(s, "utf-8").replace("+", "%20")
            load(
                URLHelper.getSearchRequest(
                    binding.searchType.checkedRadioButtonId == R.id.searchBook,
                    searchString
                ), false, addToHistory = true, -1
            )
            showLoadWaiter()
        }
        return true
    }


    private fun addValueToAutocompleteList(s: String) {
        // занесу значение в список автозаполнения
        if (XMLHandler.putSearchValue(s)) {
            // обновлю список поиска
            autocompleteStrings = viewModel.searchAutocomplete
            mSearchAdapter!!.clear()
            mSearchAdapter!!.addAll(autocompleteStrings)
            mSearchAdapter!!.notifyDataSetChanged()
        }
    }

    private fun showLoadWaiter() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun showAuthorViewSelect(author: FoundedEntity) {
        // создам диалоговое окно
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.MyDialogStyle)
        dialogBuilder.setTitle(R.string.select_author_view_message)
            .setItems(mAuthorViewTypes) { _: DialogInterface?, which: Int ->
                loadAuthor(
                    which,
                    author
                )
            }
        dialogBuilder.create().show()
    }

    private fun showSelectAuthorFromList(authors: ArrayList<FoundedEntity>) {
        // создам диалоговое окно
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle(R.string.select_authors_choose_message)
        // получу сисок имён авторов
        val iterator: Iterator<FoundedEntity> = authors.iterator()
        val authorsList = ArrayList<String?>()
        while (iterator.hasNext()) {
            val a = iterator.next()
            authorsList.add(a.name)
        }
        // покажу список выбора автора
        dialogBuilder.setItems(authorsList.toTypedArray()) { _: DialogInterface?, i: Int ->
            showAuthorViewSelect(authors[i])
        }
        dialogBuilder.show()
    }

    private fun showSelectSequenceFromList(sequences: ArrayList<FoundedEntity>) {
        // создам диалоговое окно
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle(R.string.select_sequences_choose_message)
        // получу сисок имён авторов
        val iterator: Iterator<FoundedEntity> = sequences.iterator()
        val list = ArrayList<String?>()
        while (iterator.hasNext()) {
            val a = iterator.next()
            list.add(a.name)
        }
        // покажу список выбора автора
        dialogBuilder.setItems(list.toTypedArray()) { _: DialogInterface?, i: Int ->
            load(
                sequences[i].link!!,
                append = false,
                addToHistory = true,
                clickedElementIndex = (binding.resultsList.adapter as FoundedItemAdapter).getClickedItem()
            )
        }
        dialogBuilder.show()
    }

    private fun loadAuthor(which: Int, author: FoundedEntity) {
        var url: String? = null
        val link = Regex("[^0-9]").replace(author.id!!, "")
        Log.d("surprise", "loadAuthor: link is $link")
        when (which) {
            0 -> {
                url = "/opds/authorsequences/$link"
                sBookmarkName = "Автор ${author.name} по сериям"
            }
            1 -> {
                url = "/opds/author/$link/authorsequenceless"
                sBookmarkName = "Автор ${author.name} вне серий"
            }
            2 -> {
                url = "/opds/author/$link/alphabet"
                sBookmarkName = "Автор ${author.name} по алфавиту"
            }
            3 -> {
                url = "/opds/author/$link/time"
                sBookmarkName = "Автор ${author.name} по времени"
            }
        }
        if (url != null) {
            scrollToTop()
            History.instance!!.addToClickHistory((binding.resultsList.adapter as FoundedItemAdapter).getClickedItem())
            load(url, append = false, addToHistory = true, -1)
            showLoadWaiter()
        }
    }

    override fun onQueryTextChange(s: String): Boolean {
        return false
    }


    private fun showDownloadsDialog(downloadLinks: ArrayList<DownloadLink>) {
        val inflate = layoutInflater
        @SuppressLint("InflateParams") val view =
            inflate.inflate(R.layout.confirm_book_type_select, null)
        val checker: SwitchCompat = view.findViewById(R.id.reDownload)
        checker.isChecked = PreferencesHandler.instance.isReDownload
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.MyDialogStyle)
        dialogBuilder.setTitle(R.string.downloads_dialog_header)
        // получу список типов данных
        val linksLength = downloadLinks.size
        val linksArray = arrayOfNulls<String>(linksLength)
        var counter = 0
        var mime: String?
        while (counter < linksLength) {
            mime = downloadLinks[counter].mime
            linksArray[counter] = MimeTypes.getMime(mime!!)
            counter++
        }
        dialogBuilder.setItems(linksArray) { dialogInterface: DialogInterface, i: Int ->
            // проверю, выбрано ли сохранение формата загрузки
            val dialog = dialogInterface as Dialog
            var switcher: SwitchCompat = dialog.findViewById(R.id.save_type_selection)
            if (switcher.isChecked) {
                // запомню выбор формата
                Toast.makeText(
                    requireContext(),
                    "Предпочтительный формат для скачивания сохранён (" + MimeTypes.getFullMime(
                        linksArray[i]
                    ) + "). Вы можете сбросить его в настройки +> разное.",
                    Toast.LENGTH_LONG
                ).show()
                PreferencesHandler.instance.favoriteMime = MimeTypes.getFullMime(linksArray[i])
            }
            switcher = dialog.findViewById(R.id.reDownload)
            PreferencesHandler.instance.isReDownload = switcher.isChecked
            requireActivity().invalidateOptionsMenu()
            // получу сокращённый MIME
            val shortMime = linksArray[i]
            Log.d("surprise", "OPDSActivity: 2056 short mime is $shortMime")
            val longMime = MimeTypes.getFullMime(shortMime)
            Log.d("surprise", "OPDSActivity: 2058 long mime is $longMime")
            var counter1 = 0
            val linksLength1 = downloadLinks.size
            var item: DownloadLink
            while (counter1 < linksLength1) {
                item = downloadLinks[counter1]
                if (item.mime == longMime) {
                    viewModel.addToDownloadQueue(item)
                    break
                }
                counter1++
            }
            Toast.makeText(
                requireContext(),
                "Книга добавлена в очередь загрузок",
                Toast.LENGTH_SHORT
            ).show()
        }
            .setView(view)
        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun scrollToTop() {
        binding.resultsList.scrollToPosition(0)
    }

    companion object {
        var sBookmarkName: String? = null
        const val TARGET_LINK = "target link"
        private val bookSortOptions = arrayOf(
            "По названию книги",
            "По размеру",
            "По количеству скачиваний",
            "По серии",
            "По жанру",
            "По автору",
            "Скачано",
            "Прочитано"
        )
        private val authorSortOptions = arrayOf(
            "По имени автора от А",
            "По имени автора от Я",
            "По количеству книг от большего",
            "По количеству книг от меньшего"
        )
        private val otherSortOptions = arrayOf("От А", "От Я")

        // ссылка для поиска
        val sLiveSearchLink = MutableLiveData<String>()

        var sNextPage: String? = null
    }

    override fun buttonPressed(item: FoundedEntity) {
        when (item.type) {
            TestParser.TYPE_BOOK -> {
                // обработаю загрузку
                if (item.downloadLinks.size > 1) {
                    val savedMime: String? = PreferencesHandler.instance.favoriteMime
                    if (!savedMime.isNullOrEmpty()) {
                        // проверю, нет ли в списке выбранного формата
                        item.downloadLinks.forEach {
                            if (it.mime!!.contains(savedMime)) {
                                viewModel.addToDownloadQueue(it)
                                return
                            }
                        }
                    }
                    showDownloadsDialog(item.downloadLinks)

                } else {
                    viewModel.addToDownloadQueue(item.downloadLinks[0])
                }
            }
            TestParser.TYPE_AUTHOR -> {
                // выдам список действий по автору
                showAuthorViewSelect(item)
            }
            else -> {
                // перейду по ссылке
                load(
                    item.link!!,
                    append = false,
                    addToHistory = true,
                    (binding.resultsList.adapter as FoundedItemAdapter).getClickedItem()
                )
                showLoadWaiter()
            }
        }
    }

    override fun imageClicked(item: FoundedEntity) {
        // покажу картинку
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.MyDialogStyle)
        val inflater = layoutInflater
        @SuppressLint("InflateParams") val dialogLayout =
            inflater.inflate(R.layout.book_cover, null)
        val imageContainer = dialogLayout.findViewById<ImageView>(R.id.cover_view)
        if (item.cover != null) {
            imageContainer.setImageBitmap(item.cover)
        } else {
            imageContainer.setImageDrawable(
                ResourcesCompat.getDrawable(
                    App.instance.resources,
                    R.drawable.image_wait_load,
                    null
                )
            )
            viewModel.loadImage(imageContainer, item)
        }
        dialogBuilder
            .setView(dialogLayout)
            .setCancelable(true)
            .setPositiveButton("Ок", null)
            .create()
            .show()
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        if (v.id == R.id.menuButton) {
            Log.d("surprise", "onCreateContextMenu: create menu")
            requireActivity().menuInflater.inflate(R.menu.book_menu, menu)
        } else {
            super.onCreateContextMenu(menu, v, menuInfo)
        }
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.markRead -> {
                val entity = (binding.resultsList.adapter as FoundedItemAdapter).getContentItem()
                if (entity != null) {
                    viewModel.setBookRead(entity)
                    entity.read = true
                    (binding.resultsList.adapter as FoundedItemAdapter).bookRead(entity)
                }
            }
            R.id.markDownloaded -> {
                val entity = (binding.resultsList.adapter as FoundedItemAdapter).getContentItem()
                if (entity != null) {
                    viewModel.setBookDownloaded(entity)
                    entity.downloaded = true
                    (binding.resultsList.adapter as FoundedItemAdapter).bookDownloaded(entity)
                }
            }
            R.id.actionShowDetails -> {
                val entity = (binding.resultsList.adapter as FoundedItemAdapter).getContentItem()
                if (entity != null) {
                    // открою страницу в браузере
                    val link = Regex("[^0-9]").replace(
                        entity.downloadLinks[0].url!!.replace("fb2", ""),
                        ""
                    )
                    PreferencesHandler.instance.lastLoadedUrl = "/b/$link"
                    (requireActivity() as BrowserActivity).mBottomNavView.selectedItemId =
                        R.id.navigation_web_view
                }
            }
        }
        return super.onContextItemSelected(item)
    }

    override fun itemPressed(item: FoundedEntity) {

    }

    override fun buttonLongPressed(item: FoundedEntity) {

    }

    override fun itemLongPressed(item: FoundedEntity) {

    }

    override fun menuItemPressed(item: FoundedEntity, button: View) {
        // покажу контекстное меню элемента
        registerForContextMenu(button)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            button.showContextMenu(button.x, button.y)
        } else {
            button.showContextMenu()
        }
    }

    override fun loadMoreBtnClicked() {
        if (sNextPage != null) {
            load(sNextPage!!, append = true, addToHistory = false, -1)
        }
    }

    override fun authorClicked(item: FoundedEntity) {
        if (item.authors.size == 1) {
            showAuthorViewSelect(item.authors[0])
        } else {
            showSelectAuthorFromList(item.authors)
        }
    }

    override fun sequenceClicked(item: FoundedEntity) {
        Log.d("surprise", "sequenceClicked: sequences len is ${item.sequences.size}")
        if (item.sequences.size == 1) {
            load(
                item.sequences[0].link!!,
                append = false,
                addToHistory = true,
                clickedElementIndex = (binding.resultsList.adapter as FoundedItemAdapter).getClickedItem()
            )
        } else {
            showSelectSequenceFromList(item.sequences)
        }
    }

    override fun nameClicked(item: FoundedEntity) {
        if (item.type == TestParser.TYPE_BOOK) {
            // show dialog window with content
            if (item.content.isNotEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    AlertDialog.Builder(requireContext())
                        .setTitle(item.name)
                        .setMessage(Html.fromHtml(item.content, Html.FROM_HTML_MODE_COMPACT))
                        .setPositiveButton(R.string.ok, null)
                        .show()
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle(item.name)
                        .setMessage(Html.fromHtml(item.content))
                        .setPositiveButton(R.string.ok, null)
                        .show()
                }
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager =
            requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun load(link: String, append: Boolean, addToHistory: Boolean, clickedElementIndex: Int) {
        if (!append) {
            binding.resultsCount.visibility = View.GONE
            binding.resultsCount.text = "0"
            binding.filteredCount.visibility = View.GONE
            binding.filteredCount.text = "0"
            (binding.resultsList.adapter as FoundedItemAdapter).isScrolledToLast = false
        }
        binding.progressBar.visibility = View.VISIBLE
        viewModel.request(link, append, addToHistory, clickedElementIndex = clickedElementIndex)
    }

}