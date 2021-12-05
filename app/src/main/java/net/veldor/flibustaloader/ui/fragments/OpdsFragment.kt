package net.veldor.flibustaloader.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.LayoutInflater
import android.view.ViewGroup
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
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.FoundedItemAdapter
import net.veldor.flibustaloader.databinding.FragmentOpdsBinding
import net.veldor.flibustaloader.delegates.BooksAddedToQueueDelegate
import net.veldor.flibustaloader.delegates.FoundedItemActionDelegate
import net.veldor.flibustaloader.delegates.ResultsReceivedDelegate
import net.veldor.flibustaloader.handlers.PicHandler
import net.veldor.flibustaloader.parsers.TestParser
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.selections.HistoryItem
import net.veldor.flibustaloader.selections.SearchResult
import net.veldor.flibustaloader.ui.BaseActivity
import net.veldor.flibustaloader.ui.BlacklistActivity
import net.veldor.flibustaloader.ui.BrowserActivity
import net.veldor.flibustaloader.ui.DownloadScheduleActivity
import net.veldor.flibustaloader.utils.*
import net.veldor.flibustaloader.view_models.DownloadScheduleViewModel
import net.veldor.flibustaloader.view_models.OPDSViewModel
import net.veldor.flibustaloader.workers.DownloadBooksWorker
import net.veldor.flibustaloader.workers.SendLogWorker
import java.lang.reflect.Field
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList


class OpdsFragment : Fragment(), SearchView.OnQueryTextListener, FoundedItemActionDelegate,
    BooksAddedToQueueDelegate,
    View.OnCreateContextMenuListener,
    ResultsReceivedDelegate {

    private var showDownloadSelectedMenu: Boolean = false
    private var downloadSelectedSnackbar: Snackbar? = null
    private var filteredItems: ArrayList<FoundedEntity>? = arrayListOf()
    private lateinit var binding: FragmentOpdsBinding
    lateinit var viewModel: OPDSViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
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
            load("received link", link, append = false, addToHistory = true, -1)
        }
        /*else if (viewModel.getCurrentPage() != null) {
            load(
                "get current page",
                viewModel.getCurrentPage()!!,
                append = false,
                addToHistory = false,
                clickedElementIndex = -1
            )
        }*/
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

        App.instance.liveDownloadState.observe(viewLifecycleOwner, {
            if (it == DownloadBooksWorker.DOWNLOAD_FINISHED) {
                binding.downloadInProgressIndicator.visibility = View.GONE
            } else if (it == DownloadBooksWorker.DOWNLOAD_IN_PROGRESS) {
                binding.downloadInProgressIndicator.visibility = View.VISIBLE
            }
        })

        OPDSViewModel.currentRequestState.observe(viewLifecycleOwner, {
            binding.statusWrapper.setText(it)
        })

        // получена прямая ссылка для поиска
        sLiveSearchLink.observe(viewLifecycleOwner, { s: String? ->
            if (s != null && s.isNotEmpty()) {
                load("1", s, append = false, addToHistory = true, -1)
            }
        })

        viewModel.isLoadError.observe(viewLifecycleOwner, { hasError: Boolean ->
            if (hasError) {
                binding.progressBar.visibility = View.INVISIBLE
                binding.statusWrapper.visibility = View.GONE
                binding.fab.visibility = View.GONE
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

        binding.downloadInProgressIndicator.setOnClickListener {
            val state = DownloadScheduleViewModel.liveFullBookDownloadProgress.value
            if (state != null) {
                Toast.makeText(
                    requireContext(),
                    String.format(
                        Locale.ENGLISH,
                        "Book load in progress.\nLoaded %d from %d, failed %d",
                        state.loaded, state.total, state.failed
                    ),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

        binding.statusWrapper.setInAnimation(requireContext(), android.R.anim.slide_in_left)
        binding.statusWrapper.setOutAnimation(requireContext(), android.R.anim.slide_out_right)

        binding.fab.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.load_cancelled_message),
                Toast.LENGTH_SHORT
            ).show()
            viewModel.cancelLoad()
            binding.fab.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            binding.statusWrapper.visibility = View.GONE
        }

        binding.filteredCount.setOnClickListener {
            showFilteredList()
        }

        binding.mirrorUsed.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.used_mirror_message),
                Toast.LENGTH_LONG
            ).show()
        }
        // обработаю клик на кнопку отображения списка авторов

        binding.showAuthorsListButton.setOnClickListener {
            load("5", "/opds/authorsindex", append = false, addToHistory = true, -1)
            showLoadWaiter()
        }

        // навешу обработчик на переключатель типа поиска
        binding.searchType.setOnCheckedChangeListener { _, which ->
            binding.showAllSwitcher.visibility = View.GONE
            binding.showAuthorsListButton.visibility = View.GONE
            binding.showGenresListButton.visibility = View.GONE
            binding.showSequencesListButton.visibility = View.GONE
            when (which) {
                R.id.searchBook -> {
                    binding.showAllSwitcher.visibility = View.VISIBLE
                    binding.showAuthorsListButton.visibility = View.GONE
                    binding.showGenresListButton.visibility = View.GONE
                    binding.showSequencesListButton.visibility = View.GONE
                    if (PreferencesHandler.instance.isAutofocusSearch()) {
                        mSearchView?.isIconified = false
                        mSearchView?.requestFocus()
                    }
                }
                R.id.searchAuthor -> {
                    binding.showAllSwitcher.visibility = View.GONE
                    binding.showAuthorsListButton.visibility = View.VISIBLE
                    binding.showGenresListButton.visibility = View.GONE
                    binding.showSequencesListButton.visibility = View.GONE
                    if (PreferencesHandler.instance.isAutofocusSearch()) {
                        mSearchView?.isIconified = false
                        mSearchView?.requestFocus()
                    }
                }
                R.id.searchGenre -> {
                    binding.showAllSwitcher.visibility = View.GONE
                    binding.showAuthorsListButton.visibility = View.GONE
                    binding.showGenresListButton.visibility = View.VISIBLE
                    binding.showSequencesListButton.visibility = View.GONE
                    if (PreferencesHandler.instance.isAutofocusSearch()) {
                        mSearchView?.isIconified = false
                        mSearchView?.requestFocus()
                    }
                }
                R.id.searchSequence -> {
                    binding.showAllSwitcher.visibility = View.GONE
                    binding.showAuthorsListButton.visibility = View.GONE
                    binding.showGenresListButton.visibility = View.GONE
                    binding.showSequencesListButton.visibility = View.VISIBLE
                    if (PreferencesHandler.instance.isAutofocusSearch()) {
                        mSearchView?.isIconified = false
                        mSearchView?.requestFocus()
                    }
                }
            }
        }
        binding.showGenresListButton.setOnClickListener {
            mSearchView?.isIconified = true
            (binding.resultsList.adapter as FoundedItemAdapter).setHasNext(false)
            load("2", "/opds/genres", false, addToHistory = true, -1)
            showLoadWaiter()
        }
        binding.showSequencesListButton.setOnClickListener {
            mSearchView?.isIconified = true
            (binding.resultsList.adapter as FoundedItemAdapter).setHasNext(false)
            load("3", "/opds/sequencesindex", false, addToHistory = true, -1)
            showLoadWaiter()
        }
        val a = FoundedItemAdapter(arrayListOf(), this)
        a.setHasStableIds(true)
        binding.resultsList.adapter = a
//        binding.resultsList.recycledViewPool.setMaxRecycledViews(0, 0);
//        binding.resultsList.setItemViewCacheSize(50);
//        binding.resultsList.setDrawingCacheEnabled(true);
        if (PreferencesHandler.instance.isLinearLayout) {
            binding.resultsList.layoutManager = LinearLayoutManager(requireContext())
        } else {
            binding.resultsList.layoutManager = GridLayoutManager(requireContext(), 2)
        }
        val previousResults = viewModel.getPreviouslyLoaded()
        if (previousResults != null) {
            loadFromHistory(previousResults)
        }
        binding.resultsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // проверю последний видимый элемент
                binding.resultsList.layoutManager!!.height
                if (PreferencesHandler.instance.isLinearLayout) {
                    val manager = binding.resultsList.layoutManager as LinearLayoutManager?
                    if (manager != null) {
                        val adapter = binding.resultsList.adapter
                        if (adapter != null) {
                            val position = manager.findLastCompletelyVisibleItemPosition()
                            viewModel.saveScrolledPosition(position)
                            if (!showDownloadSelectedMenu) {
                                if (
                                    !viewModel.loadInProgress() &&
                                    position == adapter.itemCount - 1 &&
                                    position > lastScrolled &&
                                    !PreferencesHandler.instance.isShowLoadMoreBtn() &&
                                    PreferencesHandler.instance.opdsPagedResultsLoad && sNextPage != null
                                    && (binding.resultsList.adapter as FoundedItemAdapter).hasBooks()
                                ) {
                                    load("4", sNextPage!!, append = true, addToHistory = false, -1)
                                }
                                lastScrolled = position
                            }
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
                    PreferencesHandler.instance.isReDownload,
                    strictFormat = PreferencesHandler.instance.isStrictDownloadFormat(),
                    delegate = this,
                    userSequenceName = null
                )
            } else {
                selectBookTypeDialog(
                    (binding.resultsList.adapter as FoundedItemAdapter).getList(),
                    false,
                    null
                )
            }
            binding.floatingMenu.close(true)
        }
        binding.fabDownloadAsSequence.setOnClickListener {
            // покажу диалог с предложением выбрать название серии
            showSaveAsSequenceDialog()
            binding.floatingMenu.close(true)
        }
        binding.fabDownloadUnloaded.setOnClickListener {
            val favoriteFormat = PreferencesHandler.instance.favoriteMime
            if (favoriteFormat != null && favoriteFormat.isNotEmpty()) {
                viewModel.downloadAll(
                    (binding.resultsList.adapter as FoundedItemAdapter).getList(),
                    favoriteFormat,
                    true,
                    strictFormat = PreferencesHandler.instance.isStrictDownloadFormat(),
                    delegate = this,
                    userSequenceName = null
                )
            } else {
                // покажу диалог выбора предпочтительнго типа скачивания
                selectBookTypeDialog(
                    (binding.resultsList.adapter as FoundedItemAdapter).getList(),
                    true,
                    null
                )
            }
            binding.floatingMenu.close(true)
        }
        binding.fabDownloadSelected.setOnClickListener {
            binding.floatingMenu.close(true)
            (binding.resultsList.adapter as FoundedItemAdapter).showCheckboxes()
            downloadSelectedSnackbar =
                Snackbar.make(binding.rootView, "Выбрано книг: 0", Snackbar.LENGTH_INDEFINITE)
            downloadSelectedSnackbar?.setAction("Скачать") {
                val booksForDownload =
                    (binding.resultsList.adapter as FoundedItemAdapter).getSelectedForDownload()
                val favoriteFormat = PreferencesHandler.instance.favoriteMime
                if (favoriteFormat != null && favoriteFormat.isNotEmpty()) {
                    viewModel.downloadAll(
                        booksForDownload,
                        favoriteFormat,
                        PreferencesHandler.instance.isReDownload,
                        strictFormat = PreferencesHandler.instance.isStrictDownloadFormat(),
                        delegate = this,
                        userSequenceName = null
                    )
                } else {
                    selectBookTypeDialog(booksForDownload, false, null)
                }
                (binding.resultsList.adapter as FoundedItemAdapter).cancelDownloadSelection()
                showDownloadSelectedMenu = false
                requireActivity().invalidateOptionsMenu()
            }
            downloadSelectedSnackbar?.show()
            showDownloadSelectedMenu = true
            requireActivity().invalidateOptionsMenu()
        }
    }

    private fun showSaveAsSequenceDialog() {
        val builder = AlertDialog.Builder(
            requireContext()
        )
        builder.setTitle(getString(R.string.sequence_name_select_title))
        val viewInflated: View = LayoutInflater.from(context)
            .inflate(R.layout.sequence_name_dialog, view as ViewGroup?, false)
        val input = viewInflated.findViewById(R.id.input) as EditText
        builder.setView(viewInflated)

// Set up the buttons

// Set up the buttons
        builder.setPositiveButton(
            android.R.string.ok
        ) { dialog, _ ->
            dialog.dismiss()
            val text = input.text.toString()
            if (text.isNotEmpty()) {
                val favoriteFormat = PreferencesHandler.instance.favoriteMime
                if (favoriteFormat != null && favoriteFormat.isNotEmpty()) {
                    viewModel.downloadAll(
                        (binding.resultsList.adapter as FoundedItemAdapter).getList(),
                        favoriteFormat,
                        PreferencesHandler.instance.isReDownload,
                        strictFormat = PreferencesHandler.instance.isStrictDownloadFormat(),
                        this,
                        text
                    )
                } else {
                    selectBookTypeDialog(
                        (binding.resultsList.adapter as FoundedItemAdapter).getList(),
                        false,
                        text
                    )
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.sequence_name_required_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        builder.setNegativeButton(
            android.R.string.cancel
        ) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun showFilteredList() {
        if (filteredItems?.isNotEmpty() == true) {
            val dialogBuilder = AlertDialog.Builder(requireContext())
            val items: ArrayList<String> = arrayListOf()
            filteredItems!!.forEach {
                items.add("${it.name}\n==> ${it.filterResult.toString()}\n")
            }
            val cs: Array<CharSequence> = items.toArray(arrayOfNulls<CharSequence>(items.size))
            dialogBuilder.setItems(cs, null)
            dialogBuilder.show()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.no_filtered_items_message),
                Toast.LENGTH_SHORT
            ).show()
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
    }

    @SuppressLint("RestrictedApi", "DiscouragedPrivateApi")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        if (showDownloadSelectedMenu) {
            requireActivity().menuInflater.inflate(R.menu.download_selected_menu, menu)
        } else {
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
            myItem.isChecked = PreferencesHandler.instance.nightMode


            // обработаю переключатель быстрой загрузки
            myItem = menu.findItem(R.id.discardFavoriteType)
            myItem.isEnabled = PreferencesHandler.instance.favoriteMime != null

            // обработаю переключатель загрузки всех результатов поиска книг
            myItem = menu.findItem(R.id.menuLoadAllBooks)
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
            myItem = menu.findItem(R.id.menuCreateAdditionalDirs)
            myItem.isChecked = PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()
            myItem = menu.findItem(R.id.loadSequencesToAuthorDir)
            myItem.isChecked = PreferencesHandler.instance.isLoadSequencesInAuthorDir()
        }
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
            PreferencesHandler.instance.isPreviews = !PreferencesHandler.instance.isPreviews
            requireActivity().invalidateOptionsMenu()
            return false
        }
        if (id == R.id.menuCreateAdditionalDirs) {
            PreferencesHandler.instance.setDifferentDirForAuthorAndSequence(!PreferencesHandler.instance.isDifferentDirForAuthorAndSequence())
            requireActivity().invalidateOptionsMenu()
            return false
        }
        if (id == R.id.loadSequencesToAuthorDir) {
            PreferencesHandler.instance.setLoadSequencesInAuthorDir(!PreferencesHandler.instance.isLoadSequencesInAuthorDir())
            requireActivity().invalidateOptionsMenu()
            return false
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
        if (id == R.id.actionSelectAll) {
            (binding.resultsList.adapter as FoundedItemAdapter).selectAllForDownload()
            return true
        }
        if (id == R.id.actionSelectNothing) {
            (binding.resultsList.adapter as FoundedItemAdapter).selectNothingForDownload()
            return true
        }
        if (id == R.id.actionInvert) {
            (binding.resultsList.adapter as FoundedItemAdapter).invertSelectionForDownload()
            return true
        }
        if (id == R.id.actionCancel) {
            (binding.resultsList.adapter as FoundedItemAdapter).cancelDownloadSelection()
            downloadSelectedSnackbar?.dismiss()
            showDownloadSelectedMenu = false
            requireActivity().invalidateOptionsMenu()
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


    private fun selectBookTypeDialog(
        booksList: ArrayList<FoundedEntity>,
        onlyNotLoaded: Boolean,
        userSequenceName: String?
    ) {
        val inflate = layoutInflater
        @SuppressLint("InflateParams") val view =
            inflate.inflate(R.layout.confirm_book_type_select, null)
        var checker: SwitchCompat = view.findViewById(R.id.reDownload)
        checker.isChecked = PreferencesHandler.instance.isReDownload
        checker = view.findViewById(R.id.onlyThisType)
        checker.isChecked = PreferencesHandler.instance.isStrictDownloadFormat()
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
                switcher = dialog.findViewById(R.id.onlyThisType)
                PreferencesHandler.instance.setStrictDownloadFormat(switcher.isChecked)
                viewModel.downloadAll(
                    booksList,
                    MimeTypes.getFullMime(MimeTypes.MIMES_LIST[i])!!,
                    onlyNotLoaded,
                    strictFormat = PreferencesHandler.instance.isStrictDownloadFormat(),
                    this,
                    userSequenceName
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
                (binding.resultsList.adapter as FoundedItemAdapter).setHasNext(false)
                when (which) {
                    0 -> {
                        load(
                            "7",
                            "/opds/new/0/new",
                            append = false,
                            addToHistory = true,
                            -1
                        )
                    }
                    1 -> {
                        load(
                            "8",
                            "/opds/newgenres",
                            append = false,
                            addToHistory = true,
                            -1
                        )
                    }
                    2 -> {
                        load(
                            "9",
                            "/opds/newauthors",
                            append = false,
                            addToHistory = true,
                            -1
                        )
                    }
                    3 -> {
                        load(
                            "10",
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
        hideKeyboard(binding.rootView)
        if (!TextUtils.isEmpty(s.trim { it <= ' ' })) {
            binding.floatingMenu.visibility = View.GONE
            mSearchView?.onActionViewCollapsed()
            addValueToAutocompleteList(s)
            // ищу введённое значение
            scrollToTop()
            val searchString = URLEncoder.encode(s, "utf-8").replace("+", "%20")
            load(
                "13",
                URLHelper.getSearchRequest(
                    binding.searchType.checkedRadioButtonId,
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
        binding.statusWrapper.visibility = View.VISIBLE
        binding.fab.visibility = View.VISIBLE
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
            Log.d("surprise", "showSelectSequenceFromList: load selected author")
            load(
                "14",
                sequences[i].link!!,
                append = false,
                addToHistory = true,
                clickedElementIndex = (binding.resultsList.adapter as FoundedItemAdapter).getClickedItemId()
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
            Log.d("surprise", "loadAuthor: load some of author")
            load(
                "15", url,
                append = false,
                addToHistory = true,
                clickedElementIndex = (binding.resultsList.adapter as FoundedItemAdapter).getClickedItemId()
            )
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
                    viewModel.addToDownloadQueue(item, this)
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
        var sNextPage: String? = null
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
            "Прочитано",
            "По формату",
            "По году публикации",
            "По добавлению на сайт",
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
    }

    override fun buttonPressed(item: FoundedEntity) {
        when (item.type) {
            TestParser.TYPE_BOOK -> {
                // обработаю загрузку
                when {
                    item.downloadLinks.size > 1 -> {
                        val savedMime: String? = PreferencesHandler.instance.favoriteMime
                        if (!savedMime.isNullOrEmpty()) {
                            // проверю, нет ли в списке выбранного формата
                            item.downloadLinks.forEach {
                                if (it.mime!!.contains(savedMime)) {
                                    viewModel.addToDownloadQueue(it, this)
                                    return
                                }
                            }
                        }
                        showDownloadsDialog(item.downloadLinks)

                    }
                    item.downloadLinks.size == 1 -> {
                        viewModel.addToDownloadQueue(item.downloadLinks[0], this)
                    }
                    else -> {
                        Toast.makeText(
                            requireContext(),
                            "Не удалось найти ссылки для скачивания книги. Отправьте мне отчёт, сообщив подробности действия",
                            Toast.LENGTH_LONG
                        ).show()
                        val work = OneTimeWorkRequest.Builder(SendLogWorker::class.java).build()
                        WorkManager.getInstance(App.instance).enqueue(work)
                    }
                }
            }
            TestParser.TYPE_AUTHOR -> {
                // выдам список действий по автору
                showAuthorViewSelect(item)
            }
            else -> {
                // перейду по ссылке
                load(
                    "17",
                    item.link!!,
                    append = false,
                    addToHistory = true,
                    (binding.resultsList.adapter as FoundedItemAdapter).getClickedItemId()
                )
                showLoadWaiter()
            }
        }
    }

    override fun imageClicked(item: FoundedEntity) {
        if (item.coverUrl != null) {
            // покажу картинку
            val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.MyDialogStyle)
            val inflater = layoutInflater
            @SuppressLint("InflateParams") val dialogLayout =
                inflater.inflate(R.layout.book_cover, null)
            val imageContainer = dialogLayout.findViewById<ImageView>(R.id.cover_view)
            if (item.cover != null) {
                imageContainer.setImageBitmap(BitmapFactory.decodeFile(item.cover!!.path))
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
        } else {
            Toast.makeText(requireContext(), "У этой книги нет обложки", Toast.LENGTH_SHORT).show()
        }
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
                    entity.read = viewModel.setBookRead(entity)
                    (binding.resultsList.adapter as FoundedItemAdapter).bookRead(entity)
                }
            }
            R.id.markDownloaded -> {
                val entity = (binding.resultsList.adapter as FoundedItemAdapter).getContentItem()
                if (entity != null) {

                    entity.downloaded = viewModel.setBookDownloaded(entity)
                    (binding.resultsList.adapter as FoundedItemAdapter).bookDownloaded(entity)
                }
            }
            R.id.actionShowDetails -> {
                val entity = (binding.resultsList.adapter as FoundedItemAdapter).getContentItem()
                if (entity != null && entity.downloadLinks.isNotEmpty()) {
                    // открою страницу в браузере
                    val link = Regex("[^0-9]").replace(
                        entity.downloadLinks[0].url!!.replace("fb2", ""),
                        ""
                    )
                    PreferencesHandler.instance.openedFromOpds = true
                    (binding.resultsList.adapter as FoundedItemAdapter).dropSelected()
                    entity.selected = true
                    PreferencesHandler.instance.lastLoadedUrl = "/b/$link"
                    (requireActivity() as BrowserActivity).mBottomNavView.selectedItemId =
                        R.id.navigation_web_view
                }
            }
        }
        return super.onContextItemSelected(item)
    }

    override fun itemPressed(item: FoundedEntity) {
        when (item.type) {
            TestParser.TYPE_AUTHOR -> {
                // выдам список действий по автору
                showAuthorViewSelect(item)
            }
            else -> {
                Log.d("surprise", "itemPressed: load on ite pressed")
                // перейду по ссылке
                load(
                    "16",
                    item.link!!,
                    append = false,
                    addToHistory = true,
                    (binding.resultsList.adapter as FoundedItemAdapter).getClickedItemId()
                )
                showLoadWaiter()
            }
        }
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
            Log.d("surprise", "loadMoreBtnClicked: load on more button clicked")
            load("21", sNextPage!!, append = true, addToHistory = false, -1)
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
            Log.d("surprise", "sequenceClicked: load on sequence clicked")
            load(
                "18",
                item.sequences[0].link!!,
                append = false,
                addToHistory = true,
                clickedElementIndex = (binding.resultsList.adapter as FoundedItemAdapter).getClickedItemId()
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

    override fun itemSelectedForDownload() {
        val readyForDownload =
            (binding.resultsList.adapter as FoundedItemAdapter).getSelectedForDownload()
        downloadSelectedSnackbar?.setText("Выбрано для загрузки: ${readyForDownload.size}")
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager =
            requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun load(
        reason: String,
        link: String,
        append: Boolean,
        addToHistory: Boolean,
        clickedElementIndex: Long
    ) {
        if (showDownloadSelectedMenu) {
            (binding.resultsList.adapter as FoundedItemAdapter).cancelDownloadSelection()
            downloadSelectedSnackbar?.dismiss()
            showDownloadSelectedMenu = false
            requireActivity().invalidateOptionsMenu()
        }
        if (!append) {
            // save results to history
            val historyItem = HistoryItem(
                nextPageLink = sNextPage,
                clickedElementIndex,
                binding.resultsCount.text.toString().toInt(),
                binding.filteredCount.text.toString().toInt(),
                (binding.resultsList.adapter as FoundedItemAdapter).getList()
            )
            History.instance!!.addToHistory(historyItem)
            PicHandler.dropPreviousLoading()
            binding.resultsCount.visibility = View.GONE
            binding.resultsCount.text = "0"
            binding.filteredCount.visibility = View.GONE
            binding.filteredCount.text = "0"
            (binding.resultsList.adapter as FoundedItemAdapter).isScrolledToLast = false
            (binding.resultsList.adapter as FoundedItemAdapter).clearList()
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.statusWrapper.visibility = View.VISIBLE
        binding.fab.visibility = View.VISIBLE
        viewModel.request(
            link,
            append,
            addToHistory,
            clickedElementIndex = clickedElementIndex,
            this
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // save loaded data from recycler
        if (::viewModel.isInitialized) {
            viewModel.cancelLoad()
        }
    }

    override fun booksAdded(count: Int) {
        Handler(Looper.getMainLooper()).post {
            if (count > 0) {
                val snackbar = Snackbar.make(
                    binding.root,
                    "Скачиваю книг $count",
                    Snackbar.LENGTH_LONG
                )
                snackbar.setAction(getString(R.string.title_activity_download_schedule)) {
                    val intent = Intent(requireContext(), DownloadScheduleActivity::class.java)
                    requireActivity().startActivity(intent)
                }
                snackbar.setActionTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        android.R.color.white,
                        null
                    )
                )
                snackbar.show()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.no_books_for_load),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun resultsReceived(results: SearchResult) {
        Log.d("surprise", "resultsReceived: received results of request")
        activity?.runOnUiThread {
            // set next page
            sNextPage = if (results.nextPageLink.isNullOrEmpty()) {
                (binding.resultsList.adapter as FoundedItemAdapter).setHasNext(false)
                null
            } else {
                (binding.resultsList.adapter as FoundedItemAdapter).setHasNext(true)
                results.nextPageLink!!
            }
            lastScrolled = 0
            if (results.nextPageLink == null || (PreferencesHandler.instance.opdsPagedResultsLoad && results.type == TestParser.TYPE_BOOK)) {
                binding.progressBar.visibility = View.INVISIBLE
                binding.statusWrapper.visibility = View.GONE
                binding.fab.visibility = View.GONE
            }
            if (results.appended) {
                filteredItems?.addAll(results.filteredList)
                if (binding.filteredCount.text.isDigitsOnly()) {
                    if (results.filtered > 0) {
                        binding.filteredCount.text =
                            (binding.filteredCount.text.toString()
                                .toInt() + results.filtered).toString()
                    }
                }
                if (binding.resultsCount.text.isDigitsOnly()) {
                    if (results.size > 0) {
                        binding.resultsCount.text =
                            (binding.resultsCount.text.toString().toInt() + results.size).toString()
                    }
                }
                (binding.resultsList.adapter as FoundedItemAdapter).appendContent(results.results)
                viewModel.saveLoaded(
                    HistoryItem(
                        sNextPage,
                        (binding.resultsList.adapter as FoundedItemAdapter).getClickedItemId(),
                        binding.resultsCount.text.toString().toInt(),
                        binding.filteredCount.text.toString().toInt(),
                        (binding.resultsList.adapter as FoundedItemAdapter).getList()
                    )
                )
            } else {
                binding.filteredCount.visibility = View.VISIBLE
                binding.resultsCount.visibility = View.VISIBLE
                binding.filteredCount.text = "0"
                binding.resultsCount.text = "0"
                filteredItems = results.filteredList
                if (results.filtered > 0) {
                    binding.filteredCount.text = results.filtered.toString()
                }
                if (results.size > 0) {
                    binding.resultsCount.text = results.size.toString()
                }
                (binding.resultsList.adapter as FoundedItemAdapter).setContent(results.results)
                viewModel.saveLoaded(
                    HistoryItem(
                        sNextPage,
                        (binding.resultsList.adapter as FoundedItemAdapter).getClickedItemId(),
                        binding.resultsCount.text.toString().toInt(),
                        binding.filteredCount.text.toString().toInt(),
                        (binding.resultsList.adapter as FoundedItemAdapter).getList()
                    )
                )
                binding.resultsList.scrollToPosition(0)
            }
            if ((binding.resultsList.adapter as FoundedItemAdapter).hasBooks()) {
                binding.floatingMenu.visibility = View.VISIBLE
            } else {
                binding.floatingMenu.visibility = View.GONE
            }
        }
    }

    fun loadFromHistory(lastPage: HistoryItem) {
        if (showDownloadSelectedMenu) {
            (binding.resultsList.adapter as FoundedItemAdapter).cancelDownloadSelection()
            downloadSelectedSnackbar?.dismiss()
            showDownloadSelectedMenu = false
            requireActivity().invalidateOptionsMenu()
        }
        binding.resultsCount.visibility = View.VISIBLE
        binding.filteredCount.visibility = View.VISIBLE
        PicHandler.dropPreviousLoading()
        sNextPage = lastPage.nextPageLink
        // set next page
        if (lastPage.nextPageLink.isNullOrEmpty()) {
            (binding.resultsList.adapter as FoundedItemAdapter).setHasNext(false)
        } else {
            (binding.resultsList.adapter as FoundedItemAdapter).setHasNext(true)
        }
        (binding.resultsList.adapter as FoundedItemAdapter).setContent(lastPage.results)
        val appendResult = (binding.resultsList.adapter as FoundedItemAdapter).applyFilters()
        if (lastPage.clickedItem >= 0) {
            Log.d("surprise", "loadFromHistory: clicked item id is ${lastPage.clickedItem}")
            val position =
                (binding.resultsList.adapter as FoundedItemAdapter).getItemPositionById(lastPage.clickedItem)
            if (position >= 0) {
                Log.d("surprise", "loadFromHistory: founded position is $position")
                binding.resultsList.scrollToPosition(position)
                (binding.resultsList.adapter as FoundedItemAdapter).markClickedElement(lastPage.clickedItem)
            } else {
                binding.resultsList.layoutManager!!.scrollToPosition(viewModel.getScrolledPosition())
            }
        } else {
            binding.resultsList.layoutManager!!.scrollToPosition(viewModel.getScrolledPosition())
        }
        binding.resultsCount.text = appendResult[0].toString()
        binding.filteredCount.text = (lastPage.filteredValues + appendResult[1]).toString()
        if ((binding.resultsList.adapter as FoundedItemAdapter).hasBooks()) {
            binding.floatingMenu.visibility = View.VISIBLE
        } else {
            binding.floatingMenu.visibility = View.GONE
        }
        viewModel.saveLoaded(lastPage)
    }
}