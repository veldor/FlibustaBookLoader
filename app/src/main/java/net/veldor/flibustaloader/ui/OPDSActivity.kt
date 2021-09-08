package net.veldor.flibustaloader.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.SearchAutoComplete
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.github.clans.fab.FloatingActionMenu
import com.google.android.material.snackbar.Snackbar
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.MyWebViewClient
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.FoundedAuthorsAdapter
import net.veldor.flibustaloader.adapters.FoundedBooksAdapter
import net.veldor.flibustaloader.adapters.FoundedGenresAdapter
import net.veldor.flibustaloader.adapters.FoundedSequencesAdapter
import net.veldor.flibustaloader.database.entity.Bookmark
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.databinding.NewOpdsActivityBinding
import net.veldor.flibustaloader.dialogs.ChangelogDialog
import net.veldor.flibustaloader.dialogs.GifDialog
import net.veldor.flibustaloader.dialogs.GifDialogListener
import net.veldor.flibustaloader.http.GlobalWebClient
import net.veldor.flibustaloader.http.TorWebClient
import net.veldor.flibustaloader.interfaces.MyAdapterInterface
import net.veldor.flibustaloader.selections.*
import net.veldor.flibustaloader.utils.Grammar.textFromHtml
import net.veldor.flibustaloader.utils.History.Companion.instance
import net.veldor.flibustaloader.utils.MimeTypes
import net.veldor.flibustaloader.utils.MimeTypes.getFullMime
import net.veldor.flibustaloader.utils.MimeTypes.getMime
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper.getBaseOPDSUrl
import net.veldor.flibustaloader.utils.URLHelper.getBaseUrl
import net.veldor.flibustaloader.utils.URLHelper.getSearchRequest
import net.veldor.flibustaloader.utils.XMLHandler.putSearchValue
import net.veldor.flibustaloader.view_models.MainViewModel
import net.veldor.flibustaloader.workers.DownloadBooksWorker.Companion.noActiveDownloadProcess
import net.veldor.flibustaloader.workers.SearchWorker
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

class OPDSActivity : BaseActivity(), SearchView.OnQueryTextListener {
    private lateinit var mBinding: NewOpdsActivityBinding
    private var mTorRestartDialog: AlertDialog? = null
    private var mViewModel: MainViewModel? = null
    private var mSearchView: SearchView? = null
    private lateinit var autocompleteStrings: ArrayList<String>
    private var mSearchAdapter: ArrayAdapter<String>? = null
    private var mSearchRadioContainer: RadioGroup? = null
    private var mShowLoadDialog: Dialog? = null
    private var mLoadMoreBtn: Button? = null
    private var mTorConnectErrorReceiver: TorConnectErrorReceiver? = null
    private val mAuthorViewTypes = arrayOf(
        "Книги по сериям",
        "Книги вне серий",
        "Книги по алфавиту",
        "Книги по дате поступления"
    )
    private var mSelectAuthorViewDialog: AlertDialog? = null
    private var mSelectAuthorsDialog: AlertDialog.Builder? = null
    private var mResultsRecycler: RecyclerView? = null
    private var mSearchTitle: TextView? = null
    private var mSelectSequencesDialog: AlertDialog.Builder? = null
    private var mLastLoadedPageUrl: String? = null
    private var mSelectedAuthor: Author? = null
    private var mConfirmExit: Long = 0
    private var mRootView: View? = null
    private var mMultiplyDownloadDialog: Dialog? = null
    private var mSelectBookTypeDialog: AlertDialog? = null
    private lateinit var mSearchAutoComplete: SearchAutoComplete
    private var mForwardBtn: ImageButton? = null
    private var mBackwardBtn: ImageButton? = null
    private var mBookTypeDialog: AlertDialog? = null
    private var mCoverPreviewDialog: Dialog? = null
    private var bookInfoDialog: AlertDialog? = null
    private var mFab: FloatingActionMenu? = null
    private var mDownloadSelectedDialog: AlertDialog? = null
    private var mSuccessSelect: LiveData<Boolean>? = null
    private var mAddToLoaded = false
    private var mScrollView: ScrollView? = null
    private var mSearchBooksButton: RadioButton? = null
    private var mSearchAuthorsButton: RadioButton? = null
    private var mMassLoadSwitcher: SwitchCompat? = null
    private var mActivityVisible = true
    private var mShowAuthorsListActivator: Button? = null
    private var mConnectionTypeView: TextView? = null
    private var mPageNotLoadedDialog: AlertDialog? = null
    private var mDisconnectedSnackbar: Snackbar? = null
    private var mCurrentPage = 0
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sLoadNextPage.value = false
        if (PreferencesHandler.instance.isEInk) {
            setContentView(R.layout.new_eink_opds_activity)
        } else {
            mBinding = NewOpdsActivityBinding.inflate(layoutInflater, null, false)
            setContentView(mBinding.root)

        }
        setupInterface()
        setupObservers()

        // добавлю viewModel
        mViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        checkSearchType()

        // проверю очередь загрузки
        checkDownloadQueue()
        mShowAuthorsListActivator = findViewById(R.id.showAuthorsListButton)
        if (mShowAuthorsListActivator != null) {
            mShowAuthorsListActivator!!.setOnClickListener {
                doSearch(
                    getBaseOPDSUrl() + "/opds/authorsindex",
                    false
                )
            }
        }

        // определю кнопки прыжков по результатам
        mLoadMoreBtn = findViewById(R.id.load_more_button)

        // кнопка перехода вперёд
        mForwardBtn = findViewById(R.id.forward_btn)
        mBackwardBtn = findViewById(R.id.backward_btn)
        if (mBackwardBtn != null) {
            mBackwardBtn!!.setOnClickListener {
                if (!instance!!.isEmpty) {
                    val lastPage = instance!!.lastPage
                    if (lastPage != null) {
                        doSearch(lastPage, false)
                        return@setOnClickListener
                    }
                }
                if (mConfirmExit != 0L) {
                    if (mConfirmExit > System.currentTimeMillis() - 3000) {
                        // выйду из приложения
                        Log.d("surprise", "OPDSActivity onKeyDown exit")
                        finishAffinity()
                    } else {
                        Toast.makeText(
                            this@OPDSActivity,
                            "Нечего загружать. Нажмите ещё раз для выхода",
                            Toast.LENGTH_SHORT
                        ).show()
                        mConfirmExit = System.currentTimeMillis()
                    }
                } else {
                    Toast.makeText(
                        this@OPDSActivity,
                        "Нечего загружать. Нажмите ещё раз для выхода",
                        Toast.LENGTH_SHORT
                    ).show()
                    mConfirmExit = System.currentTimeMillis()
                }
            }
        }
        if (mForwardBtn != null) {
            mForwardBtn!!.setOnClickListener {
                scrollToTop()
                if (sNextPage != null && sNextPage!!.isNotEmpty()) {
                    doSearch(sNextPage, false)
                } else {
                    Toast.makeText(this@OPDSActivity, "Результаты закончились", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        // зарегистрирую получатель ошибки подключения к TOR
        val filter = IntentFilter()
        filter.addAction(MyWebViewClient.TOR_CONNECT_ERROR_ACTION)
        mTorConnectErrorReceiver = TorConnectErrorReceiver()
        registerReceiver(mTorConnectErrorReceiver, filter)
        handleLoading()

        // создам тестовый массив строк для автозаполнения
        autocompleteStrings = mViewModel!!.searchAutocomplete
        if (mLoadMoreBtn != null) {
            // при нажатии кнопки назад- убираем последний элемент истории и переходим на предпоследний
            mLoadMoreBtn!!.setOnClickListener {
                // загружаю следующую страницу
                mAddToLoaded = true
                doSearch(sNextPage, false)
            }
        }
        // добавлю отслеживание изменения ваианта поиска
        mSearchBooksButton = findViewById(R.id.searchBook)
        if (mSearchBooksButton != null) {
            mSearchBooksButton!!.setOnClickListener {
                mShowAuthorsListActivator!!.visibility = View.GONE
                mMassLoadSwitcher!!.visibility = View.VISIBLE
                Toast.makeText(this@OPDSActivity, "Ищем книги", Toast.LENGTH_SHORT).show()
                sSearchType = SEARCH_TYPE_BOOKS
                if (mSearchView != null) {
                    Log.d("surprise", "OPDSActivity onCreate 242: show search icon")
                    mSearchView!!.visibility = View.VISIBLE
                    if (PreferencesHandler.instance.isAutofocusSearch()) {
                        mSearchView!!.isIconified = false
                        mSearchView!!.requestFocusFromTouch()
                    }
                }
            }
        }
        mSearchAuthorsButton = findViewById(R.id.searchAuthor)
        mSearchAuthorsButton!!.setOnClickListener {
            mMassLoadSwitcher!!.visibility = View.GONE
            mShowAuthorsListActivator!!.visibility = View.VISIBLE
            Toast.makeText(this@OPDSActivity, "Ищем авторов", Toast.LENGTH_SHORT).show()
            sSearchType = SEARCH_TYPE_AUTHORS
            if (mSearchView != null) {
                Log.d("surprise", "OPDSActivity onCreate 242: show search icon")
                mSearchView!!.visibility = View.VISIBLE
                if (PreferencesHandler.instance.isAutofocusSearch()) {
                    mSearchView!!.isIconified = false
                    mSearchView!!.requestFocusFromTouch()
                }
            }
        }
        val searchGenresButton = findViewById<RadioButton>(R.id.searchGenre)
        searchGenresButton.setOnClickListener {
            mShowAuthorsListActivator!!.visibility = View.GONE
            mMassLoadSwitcher!!.visibility = View.GONE
            Toast.makeText(this@OPDSActivity, "Ищу жанры", Toast.LENGTH_SHORT).show()
            sSearchType = SEARCH_TYPE_GENRE
            sBookmarkName = "Все жанры"
            if (mSearchView != null) {
                mSearchView!!.visibility = View.INVISIBLE
            }
            showLoadWaitingDialog()
            doSearch(getSearchRequest(SEARCH_TYPE_GENRE, "genres"), false)
            scrollToTop()
            mFab!!.visibility = View.GONE
        }
        val searchSequencesButton = findViewById<RadioButton>(R.id.searchSequence)
        searchSequencesButton.setOnClickListener {
            mMassLoadSwitcher!!.visibility = View.GONE
            Toast.makeText(this@OPDSActivity, "Ищу серии", Toast.LENGTH_SHORT).show()
            sSearchType = SEARCH_TYPE_SEQUENCES
            sBookmarkName = "Все серии"
            if (mSearchView != null) {
                mSearchView!!.visibility = View.INVISIBLE
            }
            showLoadWaitingDialog()
            doSearch(getSearchRequest(SEARCH_TYPE_SEQUENCES, "sequencesindex"), false)
            scrollToTop()
            mFab!!.visibility = View.GONE
        }

        // заголовок поиска
        val searchTitle: LiveData<String> = App.instance.mSearchTitle
        searchTitle.observe(this, { s: String? ->
            if (s != null) {
                if (mSearchTitle != null) {
                    mSearchTitle!!.visibility = View.VISIBLE
                    mSearchTitle!!.text = s
                    changeTitle(s)
                }
            } else if (mSearchTitle != null) {
                mSearchTitle!!.visibility = View.GONE
            }
        })

        // добавлю отслеживание получения списка ссылок на скачивание
        val downloadLinks: LiveData<ArrayList<DownloadLink>> = App.instance.mDownloadLinksList
        downloadLinks.observe(this, { downloadLinks1: ArrayList<DownloadLink>? ->
            if (downloadLinks1 != null && downloadLinks1.size > 0 && mActivityVisible) {
                if (downloadLinks1.size == 1) {
                    // добавлю книгу в очередь скачивания
                    mViewModel!!.addToDownloadQueue(downloadLinks1[0])
                    Toast.makeText(
                        this@OPDSActivity,
                        R.string.book_added_to_schedule_message,
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // покажу диалог для выбора ссылки для скачивания
                    showDownloadsDialog(downloadLinks1)
                }
            }
        })
        // добавлю отслеживание получения списка ссылок на скачивание
        val foundedSequences: LiveData<ArrayList<FoundedSequence>> =
            App.instance.mSelectedSequences
        foundedSequences.observe(
            this,
            { sequences: ArrayList<FoundedSequence>? -> sequences?.let { showSequenceSelectDialog(it) } })

        // добавлю отслеживание выбора типа отображения автора
        val selectedAuthor: LiveData<Author> = App.instance.mSelectedAuthor
        selectedAuthor.observe(this, { author: Author? ->
            if (author != null) {
                // если выбран конкретный автор- отображу выбор показа его книг, если выбрана группа авторов- загружу следующую страницу выбора
                if (author.id != null && author.id!!.startsWith("tag:authors")) {
                    doSearch(getBaseOPDSUrl() + author.uri, false)
                } else {
                    mSelectedAuthor = author
                    showAuthorViewSelect()
                }
            }
        })

        // добавлю отслеживание выбора автора
        val authorsList: LiveData<ArrayList<Author>> = App.instance.mSelectedAuthors
        authorsList.observe(
            this,
            { authors: ArrayList<Author>? -> authors?.let { showAuthorsSelect(it) } })

        // добавлю отслеживание выбора серии
        val selectedSequence: LiveData<FoundedSequence> = App.instance.mSelectedSequence
        selectedSequence.observe(this, { foundedSequence: FoundedSequence? ->
            if (foundedSequence != null) {
                scrollToTop()
                SearchWorker.sSequenceName = foundedSequence.title
                doSearch(getBaseOPDSUrl() + foundedSequence.link, false)
            }
        })


        // добавлю обсерверы
        addObservers()
        checkUpdates()

        // если передана ссылка- перейду по ней
        Log.d("surprise", "check link")
        val intent = intent
        val link = intent.getStringExtra(TARGET_LINK)
        if (link != null) {
            Log.d("surprise", "have link")
            doSearch(link, false)
        }
    }

    override fun setupObservers() {
        super.setupObservers()
        // Отслежу команду на загрузку страницы из адаптера
        sLoadNextPage.observe(this, { doLoad ->
            if (doLoad && sNextPage != null) {
                mAddToLoaded = true
                doSearch(sNextPage, false)
            } else {
                Toast.makeText(this@OPDSActivity, "Все книги загружены", Toast.LENGTH_SHORT).show()
                val adapter = mResultsRecycler!!.adapter
                adapter?.notifyDataSetChanged()
            }
        })
        isLoadError.observe(this, { hasError: Boolean ->
            if (hasError) {
                // покажу окошко и сообщу, что загрузка не удалась
                showPageNotLoadedDialog()
            }
        })

        // добавлю отслеживание статуса сети
        val connectionState: LiveData<Int?> = GlobalWebClient.mConnectionState
        connectionState.observe(this, { state: Int? ->
            if (state == GlobalWebClient.CONNECTED) {
                // соединение установлено.
                // провожу действие только если до этого было заявлено о потере соединения
                if (mDisconnectedSnackbar != null && mDisconnectedSnackbar!!.isShown) {
                    Toast.makeText(this@OPDSActivity, "Connected", Toast.LENGTH_LONG).show()
                    mDisconnectedSnackbar!!.dismiss()
                }
            } else if (state == GlobalWebClient.DISCONNECTED) {
                // скрою диалоговое окно загрузки
                hideWaitingDialog()
                showDisconnectedStateSnackbar()
            }
        })
    }

    private fun showDisconnectedStateSnackbar() {
        if (mDisconnectedSnackbar == null) {
            mDisconnectedSnackbar =
                Snackbar.make(mRootView!!, "Connection lost", Snackbar.LENGTH_INDEFINITE)
        }
        mDisconnectedSnackbar!!.show()
    }

    private fun showPageNotLoadedDialog() {
        if (mPageNotLoadedDialog == null) {
            mPageNotLoadedDialog = AlertDialog.Builder(this, R.style.MyDialogStyle)
                .setTitle(getString(R.string.error_load_page_title))
                .setMessage(getString(R.string.error_load_page_message))
                .setPositiveButton(android.R.string.ok, null)
                .create()
        }
        mPageNotLoadedDialog!!.show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val link = intent.getStringExtra(TARGET_LINK)
        if (link != null) {
            Log.d("surprise", "have link")
            doSearch(link, false)
        }
    }

    private fun checkSearchType() {
        // получу выбранный тип поиска
        when (mSearchRadioContainer!!.checkedRadioButtonId) {
            R.id.searchBook -> {
                sSearchType = SEARCH_TYPE_BOOKS
            }
            R.id.searchAuthor -> {
                sSearchType = SEARCH_TYPE_AUTHORS
            }
            R.id.searchSequence -> {
                sSearchType = SEARCH_TYPE_SEQUENCES
            }
            R.id.searchGenre -> {
                sSearchType = SEARCH_TYPE_GENRE
            }
        }
    }

    private fun checkDownloadQueue() {
        val queue = mViewModel!!.checkDownloadQueue()
        if (queue) {
            // продолжу загрузку книг
            val schedule = App.instance.mDatabase.booksDownloadScheduleDao().allBooksLive
            schedule!!.observe(this, { booksDownloadSchedule: List<BooksDownloadSchedule?>? ->
                if (booksDownloadSchedule != null) {
                    if (booksDownloadSchedule.isNotEmpty()) {
                        // покажу диалог с предложением докачать файлы
                        schedule.removeObservers(this@OPDSActivity)
                        continueDownload()
                    }
                }
            })
        } else {
            Log.d("surprise", "OPDSActivity checkDownloadQueue: download queue is empty")
        }
    }

    private fun continueDownload() {
        // проверю, нет ли активных процессов скачивания
        if (noActiveDownloadProcess()) {
            showReDownloadDialog()
        } else {
            Log.d("surprise", "OPDSActivity continueDownload: download still in progress")
        }
    }

    private fun showReDownloadDialog() {
        if (!this@OPDSActivity.isFinishing) {
            AlertDialog.Builder(this@OPDSActivity, R.style.MyDialogStyle)
                .setTitle(R.string.re_download_dialog_header_message)
                .setMessage(R.string.re_download_dialog_body_message)
                .setCancelable(true)
                .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                    mViewModel!!.initiateMassDownload()
                    Toast.makeText(
                        this@OPDSActivity,
                        R.string.download_continued_message,
                        Toast.LENGTH_LONG
                    ).show()
                }
                .setNegativeButton(R.string.no, null)
                .create().show()
        }
    }

    override fun setupInterface() {
        super.setupInterface()

        // определю тип соединения
        mConnectionTypeView = findViewById(R.id.connectionType)

        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToOPDS)
        item.isEnabled = false
        item.isChecked = true

        // Обёртка просмотра
        mScrollView = findViewById(R.id.subscriptions_layout)
        // рециклеры
        mResultsRecycler = findViewById(R.id.resultsList)
        mResultsRecycler!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // проверю последний видимый элемент
                if (PreferencesHandler.instance.isLinearLayout) {
                    val manager = mResultsRecycler!!.layoutManager as LinearLayoutManager?
                    if (manager != null) {
                        val adapter = mResultsRecycler!!.adapter
                        if (adapter != null) {
                            val position = manager.findLastCompletelyVisibleItemPosition()
                            if (position == adapter.itemCount - 1 && PreferencesHandler.instance.isShowLoadMoreBtn() &&
                                !PreferencesHandler.instance.isDownloadAll && sNextPage != null
                            ) {
                                Log.d(
                                    "surprise",
                                    "OPDSActivity onScrollStateChanged 562: load next page which is $sNextPage"
                                )
                                // подгружу результаты
                                mAddToLoaded = true
                                doSearch(sNextPage, false)
                            }
                        }
                    }
                }
            }
        })
        if (PreferencesHandler.instance.isLinearLayout) {
            mResultsRecycler!!.layoutManager = LinearLayoutManager(this@OPDSActivity)
        } else {
            mResultsRecycler!!.layoutManager = GridLayoutManager(this@OPDSActivity, 2)
        }

        // варианты поиска
        mSearchRadioContainer = findViewById(R.id.subscribe_type)
        mSearchTitle = findViewById(R.id.search_title)
        mRootView = findViewById(R.id.rootView)
        if (mRootView != null) {
            if (PreferencesHandler.instance.isEInk) {
                //Toast.makeText(this, "Читалка", Toast.LENGTH_SHORT).show();
                Log.d("surprise", "OPDSActivity setupInterface 529: use reader")
            } else {
                if (PreferencesHandler.instance.isPicHide()) {
                    // назначу фон
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mRootView!!.background = ContextCompat.getDrawable(this, R.drawable.back_2)
                    } else {
                        mRootView!!.background =
                            ResourcesCompat.getDrawable(resources, R.drawable.back_2, null)
                    }
                }
            }
        }
        mMassLoadSwitcher = findViewById(R.id.showAllSwitcher)
        if (mMassLoadSwitcher != null) {
            mMassLoadSwitcher!!.isChecked = PreferencesHandler.instance.isDownloadAll
            mMassLoadSwitcher!!.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean ->
                PreferencesHandler.instance.isDownloadAll =
                    !PreferencesHandler.instance.isDownloadAll
                invalidateMenu()
            }
        }
        mFab = findViewById(R.id.floatingMenu)
        mFab!!.visibility = View.GONE
        val downloadAllButton = findViewById<View>(R.id.fabDownloadAll)
        downloadAllButton?.setOnClickListener {
            val favoriteFormat = PreferencesHandler.instance.favoriteMime
            if (favoriteFormat != null && favoriteFormat.isNotEmpty()) {
                downloadAllBooks()
            } else {
                // покажу диалог выбора предпочтительнго типа скачивания
                // сброшу отслеживание удачного выбора типа книги
                App.instance.mTypeSelected.value = false
                selectBookTypeDialog()
                val successSelect: LiveData<Boolean> = App.instance.mTypeSelected
                successSelect.observe(this@OPDSActivity, { selected: Boolean ->
                    if (selected) {
                        downloadAllBooks()
                    }
                })
            }
            mFab!!.close(true)
        }
        val downloadUnloadedButton = findViewById<View>(R.id.fabDOwnloadUnloaded)
        downloadUnloadedButton?.setOnClickListener {
            val favoriteFormat = PreferencesHandler.instance.favoriteMime
            if (favoriteFormat != null && favoriteFormat.isNotEmpty()) {
                downloadUnloaded()
            } else {
                // покажу диалог выбора предпочтительнго типа скачивания
                // сброшу отслеживание удачного выбора типа книги
                App.instance.mTypeSelected.value = false
                selectBookTypeDialog()
                val successSelect: LiveData<Boolean> = App.instance.mTypeSelected
                successSelect.observe(this@OPDSActivity, { selected: Boolean ->
                    if (selected) {
                        downloadUnloaded()
                    }
                })
            }
            mFab!!.close(true)
        }
        val downloadSelectButton = findViewById<View>(R.id.fabDOwnloadSelected)
        downloadSelectButton?.setOnClickListener {
            val favoriteFormat = PreferencesHandler.instance.favoriteMime
            if (favoriteFormat != null && favoriteFormat.isNotEmpty()) {
                showDownloadSelectedDialog()
            } else {
                // покажу диалог выбора предпочтительнго типа скачивания
                // сброшу отслеживание удачного выбора типа книги
                App.instance.mTypeSelected.value = false
                selectBookTypeDialog()
                mSuccessSelect = App.instance.mTypeSelected
                mSuccessSelect!!.observe(this@OPDSActivity, { selected: Boolean ->
                    if (selected) {
                        showDownloadSelectedDialog()
                        mSuccessSelect!!.removeObservers(this@OPDSActivity)
                    }
                })
            }
            mFab!!.close(true)
        }
        showChangesList()
    }

    private fun scrollUp() {
        if (PreferencesHandler.instance.isLinearLayout) {
            val manager = mResultsRecycler!!.layoutManager as LinearLayoutManager?
            if (manager != null) {
                val position = manager.findFirstCompletelyVisibleItemPosition()
                if (position > 0) {
                    manager.scrollToPositionWithOffset(position - 1, 10)
                }
            }
        }
    }

    private fun scrollDown() {
        if (PreferencesHandler.instance.isLinearLayout) {
            val manager = mResultsRecycler!!.layoutManager as LinearLayoutManager?
            if (manager != null) {
                var position = manager.findFirstCompletelyVisibleItemPosition()
                val adapter = mResultsRecycler!!.adapter
                if (adapter != null) {
                    if (position < adapter.itemCount - 1) {
                        manager.scrollToPositionWithOffset(position + 1, 10)
                        position = manager.findLastCompletelyVisibleItemPosition()
                        if (position == adapter.itemCount - 1 && !PreferencesHandler.instance.isDownloadAll && sNextPage != null) {
                            // подгружу результаты
                            mAddToLoaded = true
                            mCurrentPage += 1
                            doSearch(sNextPage, false)
                        }
                    }
                }
            }
        }
    }

    private fun showChangesList() {
        // покажу список изменений, если он ещё не показан для этой версии
        if (PreferencesHandler.instance.isShowChanges()) {
            ChangelogDialog.Builder(this).build().show()
            PreferencesHandler.instance.setChangesViewed()
        }
    }

    private fun downloadUnloaded() {
        val status = mViewModel!!.downloadAll(true)
        observeBookScheduleAdd(status)
    }

    private fun showDownloadSelectedDialog() {
        // добавлю книги в список для загрузки
        if (mResultsRecycler!!.adapter is FoundedBooksAdapter) {
            // получу названия книг
            val books: ArrayList<FoundedBook> =
                (mResultsRecycler!!.adapter as FoundedBooksAdapter?)!!.items
            if (books.size > 0) {
                val variants = arrayOfNulls<String>(books.size)
                for ((counter, fb) in books.withIndex()) {
                    variants[counter] = String.format(
                        Locale.ENGLISH,
                        "%s \n %s \n %s \n",
                        fb.name,
                        fb.format,
                        if (fb.translate!!.isEmpty()) "" else fb.translate
                    )
                }
                mDownloadSelectedDialog = AlertDialog.Builder(this, R.style.MyDialogStyle)
                    .setTitle(R.string.select_books_for_download_message)
                    .setMultiChoiceItems(
                        variants,
                        null
                    ) { _: DialogInterface?, _: Int, _: Boolean -> }
                    .setPositiveButton(R.string.download_selected_message) { _: DialogInterface?, _: Int ->
                        mDownloadSelectedDialog!!.listView.checkedItemCount
                        val ids = mDownloadSelectedDialog!!.listView.checkedItemPositions
                        if (ids.size() > 0) {
                            val status = mViewModel!!.downloadSelected(ids)
                            observeBookScheduleAdd(status)
                        } else {
                            Toast.makeText(
                                this@OPDSActivity,
                                R.string.books_not_selected_message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    .create()
                if (!this@OPDSActivity.isFinishing) {
                    mDownloadSelectedDialog!!.show()
                }
            }
        } else {
            Toast.makeText(this, R.string.books_not_found_message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeBookScheduleAdd(status: LiveData<WorkInfo>) {
        status.observe(this, { workInfo: WorkInfo? ->
            if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                Toast.makeText(
                    this@OPDSActivity,
                    "Книги добавлены в очередь скачивания",
                    Toast.LENGTH_LONG
                ).show()
                // запущу скачивание
                if (PreferencesHandler.instance.isDownloadAutostart) {
                    mViewModel!!.initiateMassDownload()
                }
                startActivity(Intent(baseContext, ActivityBookDownloadSchedule::class.java))
                status.removeObservers(this@OPDSActivity)
            }
        })
    }

    private fun checkUpdates() {
        if (PreferencesHandler.instance.isCheckUpdate) {
            // проверю обновления
            val version = mViewModel!!.startCheckUpdate()
            version.observe(this, { aBoolean: Boolean? ->
                if (aBoolean != null && aBoolean) {
                    // показываю Snackbar с уведомлением
                    makeUpdateSnackbar()
                }
                version.removeObservers(this@OPDSActivity)
            })
        }
    }

    private fun makeUpdateSnackbar() {
        val updateSnackbar = Snackbar.make(
            mRootView!!,
            getString(R.string.snackbar_found_update_message),
            Snackbar.LENGTH_INDEFINITE
        )
        updateSnackbar.setAction(getString(R.string.snackbar_update_action_message)) { mViewModel!!.initializeUpdate() }
        updateSnackbar.setActionTextColor(ResourcesCompat.getColor(resources, android.R.color.white, null))
        updateSnackbar.show()
    }

    private fun addObservers() {
        // отслеживание нового поиска
        sNewSearch.observe(this, { aBoolean: Boolean ->
            // если есть адаптер и это не адаптер с книгами- сброшу его значение
            if (aBoolean && mResultsRecycler!!.adapter is MyAdapterInterface) {
                (mResultsRecycler!!.adapter as MyAdapterInterface?)!!.clearList()
            }
            if (!aBoolean) {
                Handler().postDelayed({
                    if (sElementForSelectionIndex >= 0 && mResultsRecycler != null && mResultsRecycler!!.adapter != null && mResultsRecycler!!.adapter!!
                            .itemCount > sElementForSelectionIndex && mResultsRecycler!!.layoutManager != null
                    ) {
                        if (PreferencesHandler.instance.isLinearLayout) {
                            val manager = mResultsRecycler!!.layoutManager as LinearLayoutManager?
                            manager!!.scrollToPositionWithOffset(sElementForSelectionIndex - 1, 10)
                        }
                    }
                }, 500)
            }
        })

        // отслеживание отсутствия результатов
        sNothingFound.observe(this, { result: Boolean ->
            if (result) {
                Toast.makeText(this@OPDSActivity, "Ничего не найдено", Toast.LENGTH_SHORT).show()
                if (mResultsRecycler!!.adapter != null) {
                    if (mResultsRecycler!!.adapter is FoundedBooksAdapter) {
                        if (mAddToLoaded) {
                            mFab!!.visibility = View.VISIBLE
                        } else {
                            (mResultsRecycler!!.adapter as FoundedBooksAdapter?)!!.setContent(
                                null,
                                false
                            )
                        }
                    } else if (mResultsRecycler!!.adapter is FoundedAuthorsAdapter) {
                        (mResultsRecycler!!.adapter as FoundedAuthorsAdapter?)!!.setContent(
                            arrayListOf()
                        )
                    }
                }
            }
        })

        // отслеживание загруженной книги
        val downloadedBook: LiveData<String> = App.instance.mLiveDownloadedBookId
        downloadedBook.observe(this, { downloadedBookId: String? ->
            if (downloadedBookId != null && mResultsRecycler!!.adapter is FoundedBooksAdapter) {
                (mResultsRecycler!!.adapter as FoundedBooksAdapter?)!!.bookDownloaded(
                    downloadedBookId
                )
            }
        })

        // отслеживание отображения обложки книги
        val bookWithCover: LiveData<FoundedBook> = App.instance.mShowCover
        bookWithCover.observe(this@OPDSActivity, { foundedBook: FoundedBook? ->
            if (foundedBook?.previewUrl != null) {
                Log.d("surprise", "onChanged: show again")
                showPreview(foundedBook)
            }
        })

        // получена прямая ссылка для поиска
        sLiveSearchLink.observe(this, { s: String? ->
            if (s != null && s.isNotEmpty()) {
                doSearch(getBaseOPDSUrl() + s, false)
            }
        })

        // отслежу получение списка жанров
        sLiveGenresFound.observe(this, { genres: ArrayList<Genre> ->
            hideLoadingButtons()
            sSearchType = SEARCH_TYPE_GENRE
            if (mResultsRecycler!!.adapter != null) {
                if (mResultsRecycler!!.adapter is FoundedGenresAdapter) {
                    (mResultsRecycler!!.adapter as FoundedGenresAdapter?)!!.setContent(genres)
                } else {
                    mResultsRecycler!!.adapter = FoundedGenresAdapter(genres)
                }
            } else {
                mResultsRecycler!!.adapter = FoundedGenresAdapter(genres)
            }
        })
        // отслежу получение списка серий
        sLiveSequencesFound.observe(this, { sequences: ArrayList<FoundedSequence> ->
            hideLoadingButtons()
            sSearchType = SEARCH_TYPE_SEQUENCES
            if (mResultsRecycler!!.adapter != null) {
                if (mResultsRecycler!!.adapter is FoundedSequencesAdapter) {
                    (mResultsRecycler!!.adapter as FoundedSequencesAdapter?)!!.setContent(
                        sequences
                    )
                } else {
                    mResultsRecycler!!.adapter = FoundedSequencesAdapter(sequences)
                }
            } else {
                mResultsRecycler!!.adapter = FoundedSequencesAdapter(sequences)
            }
        })
        sLiveAuthorsFound.observe(this, { authors: ArrayList<Author> ->
            if (!mSearchAuthorsButton!!.isChecked) {
                mSearchAuthorsButton!!.isChecked = true
            }
            App.sSearchType = SEARCH_AUTHORS
            hideLoadingButtons()
            // найденные авторы сброшены
            if (mResultsRecycler!!.adapter != null) {
                if (mResultsRecycler!!.adapter is FoundedAuthorsAdapter) {
                    (mResultsRecycler!!.adapter as FoundedAuthorsAdapter?)!!.setContent(authors)
                } else {
                    mResultsRecycler!!.adapter = FoundedAuthorsAdapter(authors)
                }
            } else {
                mResultsRecycler!!.adapter = FoundedAuthorsAdapter(authors)
            }
        })
        sLiveBooksFound.observe(this, { books: ArrayList<FoundedBook> ->
            if (!mSearchBooksButton!!.isChecked) {
                mSearchBooksButton!!.isChecked = true
            }
            App.sSearchType = SEARCH_BOOKS
            // найденные книги сброшены
            if (mResultsRecycler!!.adapter != null) {
                if (mResultsRecycler!!.adapter is FoundedBooksAdapter) {
                    (mResultsRecycler!!.adapter as FoundedBooksAdapter?)!!.setContent(
                        books,
                        mAddToLoaded
                    )
                    mAddToLoaded = false
                } else {
                    mResultsRecycler!!.adapter = FoundedBooksAdapter(books)
                }
            } else {
                mResultsRecycler!!.adapter = FoundedBooksAdapter(books)
            }
            if (books.size > 0) {
                mFab!!.visibility = View.VISIBLE
                if (!PreferencesHandler.instance.isDownloadAll && sNextPage != null) {
                    if (mLoadMoreBtn != null) {
                        mLoadMoreBtn!!.visibility = View.VISIBLE
                    }
                    if (mForwardBtn != null) {
                        mForwardBtn!!.visibility = View.VISIBLE
                    }
                    if (mBackwardBtn != null) {
                        mBackwardBtn!!.visibility = View.VISIBLE
                    }
                } else {
                    hideLoadingButtons()
                }
            } else {
                hideLoadingButtons()
            }
        })

        // добавлю отслеживание показа информации о книге
        val selectedBook: LiveData<FoundedBook> = App.instance.mSelectedBook
        selectedBook.observe(this, { book: FoundedBook? ->
            if (book != null) {
                Log.d("surprise", "OPDSActivity onChanged show book info")
                bookInfoDialog = AlertDialog.Builder(this@OPDSActivity, R.style.MyDialogStyle)
                    .setTitle(book.name)
                    .setMessage(textFromHtml(book.bookInfo))
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                if (!this@OPDSActivity.isFinishing) {
                    bookInfoDialog!!.show()
                }
            }
        })

        // добавлю отслеживание контекстного меню для книги
        val contextBook: LiveData<FoundedBook> = App.instance.mContextBook
        contextBook.observe(this, { foundedBook: FoundedBook? ->
            // добавлю хранилище для книги
            if (foundedBook != null) {
                // покажу контекстное меню
                val dialogBuilder = AlertDialog.Builder(this@OPDSActivity, R.style.MyDialogStyle)
                val items = arrayOf("Пометить книгу как прочитанную", "Показать страницу книги")
                dialogBuilder.setItems(items) { _: DialogInterface?, which: Int ->
                    when (which) {
                        0 -> {
                            // отмечу книгу как прочитанную
                            mViewModel!!.setBookRead(foundedBook)
                            Toast.makeText(
                                this@OPDSActivity,
                                "Книга отмечена как прочитанная",
                                Toast.LENGTH_LONG
                            ).show()
                            // оповещу адаптер об этом
                            if (mResultsRecycler!!.adapter is FoundedBooksAdapter) {
                                (mResultsRecycler!!.adapter as FoundedBooksAdapter?)!!.setBookReaded(
                                    foundedBook
                                )
                            }
                        }
                        1 -> showPage(foundedBook)
                    }
                }
                mBookTypeDialog = dialogBuilder.create()
                if (!this@OPDSActivity.isFinishing) {
                    mBookTypeDialog!!.show()
                }
            }
        })
        val authorNewBooks: LiveData<Author> = App.instance.mAuthorNewBooks
        authorNewBooks.observe(
            this,
            { author: Author? ->
                if (author != null) doSearch(
                    getBaseOPDSUrl() + "/opds/new/0/newauthors/" + author.id!!.substring(22), false
                )
            })

        // отслеживание статуса загрузки книг
        val multiplyDownloadStatus: LiveData<String> = App.instance.mMultiplyDownloadStatus
        multiplyDownloadStatus.observe(this, { s: String? ->
            if (s != null && s.isNotEmpty() && mMultiplyDownloadDialog != null) {
                val window = mMultiplyDownloadDialog!!.window
                if (window != null) {
                    val dialogText = window.findViewById<TextView>(R.id.title)
                    dialogText.text = s
                }
            }
        })

        // отслеживание незавершённого поиска
        var workStatus = App.instance.mSearchWork.value
        if (workStatus != null) {
            if (workStatus.state == WorkInfo.State.RUNNING || workStatus.state == WorkInfo.State.ENQUEUED) {
                showLoadWaitingDialog()
            }
        }
        // отслеживание незавершённого скачивания
        if (App.instance.mDownloadAllWork != null) {
            workStatus = App.instance.mDownloadAllWork?.value
            if (workStatus != null) {
                if (workStatus.state == WorkInfo.State.RUNNING || workStatus.state == WorkInfo.State.ENQUEUED) {
                    showMultiplyDownloadDialog()
                    val status = App.instance.mMultiplyDownloadStatus.value
                    if (status != null && status.isNotEmpty()) {
                        val window = mMultiplyDownloadDialog!!.window
                        if (window != null) {
                            val dialogText = window.findViewById<TextView>(R.id.title)
                            dialogText.text = status
                        }
                    }
                }
            }
            observeBooksDownload()
        }
        val loadStatus: LiveData<String> = App.instance.mLoadAllStatus
        loadStatus.observe(this, { s: String? ->
            if (s != null && s.isNotEmpty() && mShowLoadDialog != null) {
                // изменю сообщение
                val window = mShowLoadDialog!!.window
                if (window != null) {
                    val dialogText = window.findViewById<TextView>(R.id.title)
                    if (dialogText != null) {
                        dialogText.text = s
                    }
                }
            }
            if (s != null && s.isNotEmpty() && mMultiplyDownloadDialog != null) {
                // изменю сообщение
                val window = mMultiplyDownloadDialog!!.window
                if (window != null) {
                    val dialogText = window.findViewById<TextView>(R.id.title)
                    if (dialogText != null) {
                        dialogText.text = s
                    }
                }
            }
        })
    }

    private fun hideLoadingButtons() {
        if (mLoadMoreBtn != null) {
            mLoadMoreBtn!!.visibility = View.GONE
        }
        if (mForwardBtn != null) {
            mForwardBtn!!.visibility = View.GONE
        }
        if (mBackwardBtn != null) {
            mBackwardBtn!!.visibility = View.GONE
        }
    }

    private fun showPreview(foundedBook: FoundedBook) {
        if (!this@OPDSActivity.isFinishing) {
            Log.d("surprise", "OPDSActivity showPreview: preview url is " + foundedBook.previewUrl)
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            val inflater = layoutInflater
            @SuppressLint("InflateParams") val dialogLayout =
                inflater.inflate(R.layout.book_cover, null)
            val imageContainer = dialogLayout.findViewById<ImageView>(R.id.cover_view)
            Glide
                .with(imageContainer)
                .load(PreferencesHandler.instance.picMirror + foundedBook.previewUrl)
                .into(imageContainer)
            dialogBuilder
                .setView(dialogLayout)
                .setCancelable(true)
                .setPositiveButton("Ок", null)
                .create()
                .show()
        }
    }

    private fun showPage(book: FoundedBook?) {
        val intent = Intent(this, WebViewActivity::class.java)
        intent.data = Uri.parse(getBaseUrl() + book!!.bookLink)
        intent.putExtra(WebViewActivity.CALLED, true)
        startActivity(intent)
    }

    private fun observeBooksDownload() {
        // отслежу загрузку книг
        val booksDownloadStatus = App.instance.mDownloadAllWork
        booksDownloadStatus?.observe(this, { workInfo: WorkInfo? ->
            if (workInfo != null) {
                if (workInfo.state == WorkInfo.State.SUCCEEDED && !App.instance.mDownloadsInProgress) {
                    Toast.makeText(
                        this@OPDSActivity,
                        "Все книги загружены (кажется)",
                        Toast.LENGTH_LONG
                    ).show()
                    // работа закончена, закрою диалог и выведу тост
                    if (mMultiplyDownloadDialog != null) {
                        mMultiplyDownloadDialog!!.dismiss()
                        booksDownloadStatus.removeObservers(this@OPDSActivity)
                    }
                }
                // если есть недогруженные книги- выведу Snackbar, где уведомплю об этом
                if (App.instance.mBooksDownloadFailed.size > 0) {
                    val updateSnackbar = Snackbar.make(
                        mRootView!!,
                        "Есть недокачанные книги",
                        Snackbar.LENGTH_INDEFINITE
                    )
                    updateSnackbar.setAction("Попробовать ещё раз") { }
                    updateSnackbar.setActionTextColor(ResourcesCompat.getColor(resources, android.R.color.white, null))
                    updateSnackbar.show()
                }
            }
        })
    }

    private fun showSequenceSelectDialog(sequences: ArrayList<FoundedSequence>) {
        if (mSelectSequencesDialog == null) {
            // создам диалоговое окно
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle(R.string.select_sequences_choose_message)
            mSelectSequencesDialog = dialogBuilder
        }
        // получу сисок серий
        val iterator: Iterator<FoundedSequence> = sequences.iterator()
        val sequencesList = ArrayList<String?>()
        while (iterator.hasNext()) {
            val s = iterator.next()
            sequencesList.add(s.title)
        }
        // покажу список выбора серии
        mSelectSequencesDialog!!.setItems(sequencesList.toTypedArray()) { _: DialogInterface?, i: Int ->
            App.instance.mSelectedSequence.postValue(
                sequences[i]
            )
        }
        if (!this@OPDSActivity.isFinishing) {
            mSelectSequencesDialog!!.show()
        }
    }

    override fun onPause() {
        super.onPause()
        App.instance.mContextBook.value = null
        App.instance.mSelectedBook.value = null
        App.instance.mSelectedAuthor.value = null
        App.instance.mSelectedAuthors.value = null
        App.instance.mDownloadLinksList.value = null
        App.instance.mSelectedSequence.value = null
        App.instance.mSelectedSequences.value = null
        if (bookInfoDialog != null) {
            bookInfoDialog!!.dismiss()
            bookInfoDialog = null
        }
        if (mShowLoadDialog != null) {
            mShowLoadDialog!!.dismiss()
            mShowLoadDialog = null
        }
        if (mMultiplyDownloadDialog != null) {
            mMultiplyDownloadDialog!!.dismiss()
            mMultiplyDownloadDialog = null
        }
        if (mSelectBookTypeDialog != null) {
            mSelectBookTypeDialog!!.dismiss()
            mSelectBookTypeDialog = null
        }
        if (mBookTypeDialog != null) {
            mBookTypeDialog!!.dismiss()
            mBookTypeDialog = null
        }
        if (mCoverPreviewDialog != null) {
            mCoverPreviewDialog!!.dismiss()
            mCoverPreviewDialog = null
        }
        mActivityVisible = false
    }

    override fun onResume() {
        super.onResume()
        if (mConnectionTypeView != null) {
            if (PreferencesHandler.instance.isExternalVpn) {
                mConnectionTypeView!!.text = getString(R.string.vpn_title)
                mConnectionTypeView!!.setBackgroundColor(Color.parseColor("#03A9F4"))
            } else {
                mConnectionTypeView!!.text = getString(R.string.tor_title)
                mConnectionTypeView!!.setBackgroundColor(Color.parseColor("#4CAF50"))
            }
        }
        if (mResultsRecycler != null) {
            mResultsRecycler!!.performClick()
            if (mResultsRecycler!!.adapter is MyAdapterInterface) {
                mResultsRecycler!!.adapter!!.notifyDataSetChanged()
            }
        }
        mActivityVisible = true
    }

    override fun onDestroy() {
        super.onDestroy()
        App.instance.mShowCover.value = null
        if (mTorConnectErrorReceiver != null) {
            unregisterReceiver(mTorConnectErrorReceiver)
        }
        App.instance.mSelectedAuthor.value = null
        App.instance.mSelectedAuthors.value = null
        App.instance.mDownloadLinksList.value = null
        App.instance.mSelectedSequence.value = null
        App.instance.mSelectedSequences.value = null
    }

    private fun hideWaitingDialog() {
        if (mShowLoadDialog != null) {
            mShowLoadDialog!!.dismiss()
            App.instance.mLoadAllStatus.value = "Ожидание"
        }
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        Handler().postDelayed({
            if (sSearchType == SEARCH_TYPE_BOOKS || sSearchType == SEARCH_TYPE_AUTHORS) {
                mSearchView!!.visibility = View.VISIBLE
            } else {
                mSearchView!!.visibility = View.INVISIBLE
            }
        }, 100)
        return super.onMenuOpened(featureId, menu)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.odps_menu, menu)

        // добавлю обработку поиска
        val searchMenuItem = menu.findItem(R.id.action_search)
        mSearchView = searchMenuItem.actionView as SearchView
        if (mSearchView != null) {
            if (PreferencesHandler.instance.isEInk) {
                mSearchView!!.queryHint = ""
            }
            mSearchView!!.inputType = InputType.TYPE_CLASS_TEXT
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
            if (PreferencesHandler.instance.isEInk) {
                mSearchAutoComplete.setTextColor(Color.WHITE)
            }
            mSearchAutoComplete.setDropDownBackgroundResource(R.color.background_color)
            mSearchAutoComplete.threshold = 0
            mSearchAutoComplete.dropDownHeight = ViewGroup.LayoutParams.WRAP_CONTENT
            mSearchAdapter =
                ArrayAdapter(this, android.R.layout.simple_list_item_1, autocompleteStrings)
            mSearchAutoComplete.setAdapter(mSearchAdapter)
        }
        var myItem = menu.findItem(R.id.menuUseDarkMode)
        myItem.isChecked = mViewModel!!.nightModeEnabled


        // обработаю переключатель быстрой загрузки
        myItem = menu.findItem(R.id.discardFavoriteType)
        myItem.isEnabled = PreferencesHandler.instance.favoriteMime != null

        // обработаю переключатель загрузки всех результатов поиска книг
        myItem = menu.findItem(R.id.menuLoadAllBooks)
        Log.d(
            "surprise",
            "onCreateOptionsMenu: is checked? " + PreferencesHandler.instance.isDownloadAll
        )
        myItem.isChecked = PreferencesHandler.instance.isDownloadAll

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
        return true
    }

    private fun selectSorting() {
        if (sSearchType != null) {
            val sortingOptions: Array<String> = when (sSearchType) {
                SEARCH_TYPE_BOOKS -> bookSortOptions
                SEARCH_TYPE_AUTHORS -> authorSortOptions
                else -> otherSortOptions
            }
            val dialog = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialog.setTitle("Выберите тип сортировки")
                .setItems(sortingOptions) { _: DialogInterface?, which: Int -> sortList(which) }
            // покажу список типов сортировки
            if (!this@OPDSActivity.isFinishing) {
                dialog.show()
            }
        }
    }

    private fun sortList(which: Int) {
        Log.d("surprise", "OPDSActivity sortList 1064: sort " + App.sSearchType)
        when (App.sSearchType) {
            SEARCH_BOOKS -> {
                App.instance.mBookSortOption = which
                // если подключен книжный адаптер- проведу сортировку
                if (mResultsRecycler!!.adapter is FoundedBooksAdapter) {
                    (mResultsRecycler!!.adapter as FoundedBooksAdapter?)!!.sort()
                }
            }
            SEARCH_AUTHORS -> {
                App.instance.mAuthorSortOptions = which
                Log.d("surprise", "OPDSActivity sortList 1075: sort authors")
                if (mResultsRecycler!!.adapter is FoundedAuthorsAdapter) {
                    (mResultsRecycler!!.adapter as FoundedAuthorsAdapter?)!!.sort()
                }
            }
            SEARCH_SEQUENCE -> {
                App.instance.mOtherSortOptions = which
                if (mResultsRecycler!!.adapter is FoundedSequencesAdapter) {
                    (mResultsRecycler!!.adapter as FoundedSequencesAdapter?)!!.sort()
                }
            }
            SEARCH_GENRE -> {
                App.instance.mOtherSortOptions = which
                if (mResultsRecycler!!.adapter is FoundedGenresAdapter) {
                    (mResultsRecycler!!.adapter as FoundedGenresAdapter?)!!.sort()
                }
            }
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
            mViewModel!!.switchNightMode()
            Handler().postDelayed(ResetApp(), 100)
            return true
        }
        if (id == R.id.hideReadSwitcher) {
            PreferencesHandler.instance.isHideRead = !PreferencesHandler.instance.isHideRead
            // скрою книги, если они есть
            if (PreferencesHandler.instance.isHideRead) {
                Toast.makeText(this@OPDSActivity, "Скрываю прочитанные книги", Toast.LENGTH_SHORT)
                    .show()
                hideReadedBooks()
            } else {
                Toast.makeText(this@OPDSActivity, "Отображаю прочитанные книги", Toast.LENGTH_SHORT)
                    .show()
                showReadedBooks()
            }
            invalidateMenu()
            return true
        }
        if (id == R.id.action_sort_by) {
            selectSorting()
            return true
        }
        if (id == R.id.menuLoadAllBooks) {
            mMassLoadSwitcher!!.toggle()
            invalidateMenu()
            return true
        }
        if (id == R.id.clearSearchHistory) {
            clearHistory()
            return true
        }
        if (id == R.id.discardFavoriteType) {
            PreferencesHandler.instance.favoriteMime = null
            Toast.makeText(this, "Выбранный тип загрузок сброшен", Toast.LENGTH_SHORT).show()
            return true
        }
        if (id == R.id.showPreviews) {
            PreferencesHandler.instance.isPreviews
            invalidateMenu()
            return true
        }
        if (id == R.id.switchLayout) {
            PreferencesHandler.instance.isLinearLayout = !PreferencesHandler.instance.isLinearLayout
            recreate()
        }
        if (id == R.id.hideDigests) {
            PreferencesHandler.instance.isHideDigests = !PreferencesHandler.instance.isHideDigests
            invalidateMenu()
            if (PreferencesHandler.instance.isHideDigests) {
                Toast.makeText(
                    this@OPDSActivity,
                    "Скрываю книги, у которых больше 3 авторов",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return true
        }
        if (id == R.id.menuCreateAuthorDir) {
            PreferencesHandler.instance.setCreateAuthorsDir(!item.isChecked)
            invalidateMenu()
            if (PreferencesHandler.instance.isCreateAuthorsDir()) {
                Toast.makeText(
                    this@OPDSActivity,
                    "Создаю папки для отдельных авторов",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@OPDSActivity,
                    "Не создаю папки для отдельных авторов",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return true
        }
        if (id == R.id.menuCreateSequenceDir) {
            PreferencesHandler.instance.setCreateSequencesDir(!item.isChecked)
            invalidateMenu()
            if (PreferencesHandler.instance.isCreateSequencesDir()) {
                Toast.makeText(
                    this@OPDSActivity,
                    "Создаю папки для отдельных серий",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@OPDSActivity,
                    "Не создаю папки для отдельных серий",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return true
        }
        if (id == R.id.hideDownloadedSwitcher) {
            PreferencesHandler.instance.isHideDownloaded =
                PreferencesHandler.instance.isHideDownloaded
            invalidateMenu()
            if (PreferencesHandler.instance.isHideDownloaded) {
                Toast.makeText(
                    this@OPDSActivity,
                    "Скрываю ранее скачанные книги",
                    Toast.LENGTH_SHORT
                ).show()
                hideDownloadedBooks()
            } else {
                Toast.makeText(
                    this@OPDSActivity,
                    "Показываю ранее скачанные книги",
                    Toast.LENGTH_SHORT
                ).show()
                showDownloadedBooks()
            }
            return true
        }
        if (id == R.id.useFilter) {
            PreferencesHandler.instance.isUseFilter = !item.isChecked
            invalidateMenu()
            if (PreferencesHandler.instance.isUseFilter) {
                startActivity(Intent(this, BlacklistActivity::class.java))
            } else {
                Toast.makeText(
                    this@OPDSActivity,
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
        val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
        val view = layoutInflater.inflate(R.layout.dialog_bookmark_name, null)
        val input = view.findViewById<EditText>(R.id.bookmarkNameInput)
        input.setText(sBookmarkName)
        dialogBuilder.setTitle("Название закладки")
            .setView(view)
            .setPositiveButton("Сохранить") { _: DialogInterface?, _: Int ->
                val bookmarkText = input.text.toString()
                val link = instance!!.showLastPage()
                if (bookmarkText.isNotEmpty()) {
                    // сохраню закладку
                    val bookmark = Bookmark()
                    bookmark.link = link
                    Log.d("surprise", "onClick: add link $link")
                    bookmark.name = bookmarkText
                    App.instance.mDatabase.bookmarksDao().insert(bookmark)
                    Toast.makeText(this@OPDSActivity, "Закладка добавлена", Toast.LENGTH_LONG)
                        .show()
                } else {
                    Toast.makeText(
                        this@OPDSActivity,
                        "Нужно заполнить имя закладки и произвести поиск",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton(android.R.string.no, null)
            .show()
    }

    private fun showReadedBooks() {
        if (mResultsRecycler!!.adapter is FoundedBooksAdapter) {
            (mResultsRecycler!!.adapter as FoundedBooksAdapter?)!!.showReaded(sBooksForDownload!!)
        }
    }

    private fun showDownloadedBooks() {
        if (mResultsRecycler!!.adapter is FoundedBooksAdapter) {
            (mResultsRecycler!!.adapter as FoundedBooksAdapter?)!!.showDownloaded(sBooksForDownload!!)
        }
    }

    private fun hideReadedBooks() {
        if (mResultsRecycler!!.adapter is FoundedBooksAdapter) {
            (mResultsRecycler!!.adapter as FoundedBooksAdapter?)!!.hideReaded()
        }
    }

    private fun hideDownloadedBooks() {
        if (mResultsRecycler!!.adapter is FoundedBooksAdapter) {
            (mResultsRecycler!!.adapter as FoundedBooksAdapter?)!!.hideDownloaded()
        }
    }

    private fun clearHistory() {
        mViewModel!!.clearHistory()
        autocompleteStrings = ArrayList()
        mSearchAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, autocompleteStrings)
        mSearchAutoComplete.setAdapter(mSearchAdapter)
        Toast.makeText(this, "Автозаполнение сброшено", Toast.LENGTH_SHORT).show()
    }

    private fun downloadAllBooks() {
        // добавлю книги в список для загрузки
        // если выбран тип загрузки книг и они существуют- предлагаю выбрать тип загрузки
        val status = mViewModel!!.downloadAll(false)
        observeBookScheduleAdd(status)
    }

    private fun selectBookTypeDialog() {
        val inflate = layoutInflater
        @SuppressLint("InflateParams") val view =
            inflate.inflate(R.layout.confirm_book_type_select, null)
        var checker: SwitchCompat = view.findViewById(R.id.save_only_selected)
        checker.isChecked = PreferencesHandler.instance.saveOnlySelected
        checker = view.findViewById(R.id.reDownload)
        checker.isChecked = PreferencesHandler.instance.isReDownload
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Выберите формат скачивания")
            .setItems(MimeTypes.MIMES_LIST) { dialogInterface: DialogInterface, i: Int ->
                val dialog = dialogInterface as Dialog
                var switcher: SwitchCompat = dialog.findViewById(R.id.save_type_selection)
                if (switcher.isChecked) {
                    // запомню выбор формата
                    Toast.makeText(
                        this@OPDSActivity,
                        "Предпочтительный формат для скачивания сохранён(" + getFullMime(
                            MimeTypes.MIMES_LIST[i]
                        ) + "). Вы можете сбросить его в настройки +> разное.",
                        Toast.LENGTH_LONG
                    ).show()
                    PreferencesHandler.instance.favoriteMime = getFullMime(MimeTypes.MIMES_LIST[i])
                }
                switcher = dialog.findViewById(R.id.save_only_selected)
                PreferencesHandler.instance.saveOnlySelected = switcher.isChecked
                switcher = dialog.findViewById(R.id.reDownload)
                PreferencesHandler.instance.isReDownload = switcher.isChecked
                invalidateMenu()
                // оповещу о завершении выбора
                App.instance.mSelectedFormat = getFullMime(MimeTypes.MIMES_LIST[i])
                App.instance.mTypeSelected.postValue(true)
            }
            .setView(view)
        mSelectBookTypeDialog = dialogBuilder.create()
        // покажу диалог с выбором предпочтительного формата
        if (!this@OPDSActivity.isFinishing) {
            mSelectBookTypeDialog!!.show()
        }
    }

    private fun showMultiplyDownloadDialog() {
        if (mMultiplyDownloadDialog == null) {
            mMultiplyDownloadDialog = if (PreferencesHandler.instance.isEInk) {
                AlertDialog.Builder(this, R.style.MyDialogStyle)
                    .setTitle(getString(R.string.download_books_title))
                    .setMessage(getString(R.string.download_books_msg))
                    .setPositiveButton("Отменить") { _: DialogInterface?, _: Int ->
                        if (App.instance.mProcess != null) {
                            WorkManager.getInstance(this@OPDSActivity)
                                .cancelWorkById(App.instance.mProcess!!.id)
                        }
                        mMultiplyDownloadDialog?.dismiss()
                        // отменю операцию
                        Toast.makeText(
                            this@OPDSActivity,
                            "Загрузка книг отменена",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .create()
            } else {
                GifDialog.Builder(this)
                    .setTitle(getString(R.string.download_books_title))
                    .setMessage(getString(R.string.download_books_msg))
                    .setGifResource(R.drawable.loading) //Pass your Gif here
                    .isCancellable(false)
                    .setPositiveBtnText("Отменить")
                    .onPositiveClicked(object : GifDialogListener {
                        override fun onClick() {
                            if (App.instance.mProcess != null) {
                                Log.d("surprise", "OPDSActivity OnClick kill process")
                                WorkManager.getInstance(this@OPDSActivity)
                                    .cancelWorkById(App.instance.mProcess!!.id)
                            }
                            if (mMultiplyDownloadDialog != null) {
                                mMultiplyDownloadDialog!!.dismiss()
                            }
                            // отменю операцию
                            Toast.makeText(
                                this@OPDSActivity,
                                "Загрузка книг отменена",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    })
                    .build()
            }
        }
        //mMultiplyDownloadDialog.setMessage("Считаю количество книг для скачивания");
        if (!this@OPDSActivity.isFinishing) {
            mMultiplyDownloadDialog!!.show()
        }
    }

    private fun showNewDialog() {
        if (!this@OPDSActivity.isFinishing) {
            val newCategory = arrayOf(
                "Все новинки",
                "Новые книги по жанрам",
                "Новые книги по авторам",
                "Новые книги по сериям"
            )
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle(R.string.showNewDialogTitle)
                .setItems(newCategory) { _: DialogInterface?, which: Int ->
                    // сброшу ранее загруженное
                    App.instance.mResultsEscalate = false
                    when (which) {
                        0 -> {
                            App.sSearchType = SEARCH_BOOKS
                            doSearch(getBaseOPDSUrl() + "/opds/new/0/new", false)
                        }
                        1 -> doSearch(getBaseOPDSUrl() + "/opds/newgenres", false)
                        2 -> doSearch(getBaseOPDSUrl() + "/opds/newauthors", false)
                        3 -> doSearch(getBaseOPDSUrl() + "/opds/newsequences", false)
                    }
                }
                .show()
        }
    }

    override fun onQueryTextSubmit(s: String): Boolean {
        if (!TextUtils.isEmpty(s.trim { it <= ' ' })) {
            // ищу введённое значение
            try {
                mAddToLoaded = false
                scrollToTop()
                makeSearch(s)
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }
        return true
    }

    @Throws(UnsupportedEncodingException::class)
    private fun makeSearch(s: String) {
        addValueToAutocompleteList(s)
        changeTitle("Поиск $s")
        sBookmarkName = when (sSearchType) {
            SEARCH_TYPE_BOOKS -> {
                "Книга: $s"
            }
            SEARCH_TYPE_AUTHORS -> {
                "Автор: $s"
            }
            else -> {
                s
            }
        }
        val searchString = URLEncoder.encode(s, "utf-8").replace("+", "%20")
        doSearch(getSearchRequest(sSearchType, searchString), false)
    }

    private fun addValueToAutocompleteList(s: String) {
        // занесу значение в список автозаполнения
        if (putSearchValue(s)) {
            // обновлю список поиска
            autocompleteStrings = mViewModel!!.searchAutocomplete
            mSearchAdapter!!.clear()
            mSearchAdapter!!.addAll(autocompleteStrings)
            mSearchAdapter!!.notifyDataSetChanged()
        }
    }

    private fun doSearch(s: String?, searchOnBackPressed: Boolean) {
        mFab!!.visibility = View.GONE
        mCurrentPage = 0
        if (s != null && s.isNotEmpty()) {
            if (mSearchView != null) {
                mSearchView!!.onActionViewCollapsed()
            }
            // сохраню последнюю загруженную страницу
            PreferencesHandler.instance.lastLoadedUrl = s
            if (!searchOnBackPressed) {
                if (mLastLoadedPageUrl != null) {
                    Log.d(
                        "surprise",
                        "OPDSActivity doSearch 1733: add to history: $mLastLoadedPageUrl"
                    )
                    instance!!.addToHistory(
                        mLastLoadedPageUrl!!
                    )
                }
                Log.d("surprise", "OPDSActivity doSearch 1736: save last loaded page $s")
                mLastLoadedPageUrl = s
            }
            // очищу историю поиска и положу туда начальное значение
//            if (addToHistory || History.getInstance().isOneElementInQueue()) {
//                Log.d("surprise", "OPDSActivity doSearch 1730: add to HISTORY");
//                History.getInstance().addToHistory(s);
//            }
            showLoadWaitingDialog()
            if (!searchOnBackPressed) {
                // сохраню порядковый номер элемента по которому был клик, если он был
                mViewModel!!.saveClickedIndex(sClickedItemIndex)
            }
            sClickedItemIndex = -1
            observeSearchStatus(mViewModel!!.request(s))
            // добавлю значение в историю
        }
    }

    private fun observeSearchStatus(requestId: UUID) {
        val workInfo = WorkManager.getInstance(this).getWorkInfoByIdLiveData(requestId)
        workInfo.observe(this, { workInfo1: WorkInfo? ->
            if (workInfo1 != null) {
                if (workInfo1.state.isFinished) {
                    hideWaitingDialog()
                }
            }
        })
    }

    private fun showLoadWaitingDialog() {
        if (mShowLoadDialog == null) {
            mShowLoadDialog = if (PreferencesHandler.instance.isEInk) {
                val view = layoutInflater.inflate(R.layout.dialog_waiting_e_ink, null, false)
                AlertDialog.Builder(this, R.style.MyDialogStyle)
                    .setTitle(getString(R.string.download_books_title))
                    .setView(view)
                    .setPositiveButton("Отменить") { _: DialogInterface?, _: Int ->
                        // отменю операцию поиска
                        WorkManager.getInstance(this@OPDSActivity)
                            .cancelUniqueWork(SearchWorker.WORK_TAG)
                        hideWaitingDialog()
                        Toast.makeText(this@OPDSActivity, "Загрузка отменена", Toast.LENGTH_LONG)
                            .show()
                    }
                    .create()
            } else {
                GifDialog.Builder(this)
                    .setTitle(getString(R.string.load_waiting_title))
                    .setMessage(getString(R.string.load_waiting_message))
                    .setGifResource(R.drawable.loading) //Pass your Gif here
                    .isCancellable(false)
                    .setPositiveBtnText("Отменить")
                    .onPositiveClicked(object : GifDialogListener {
                        override fun onClick() {
                            // отменю операцию поиска
                            WorkManager.getInstance(this@OPDSActivity)
                                .cancelUniqueWork(SearchWorker.WORK_TAG)
                            hideWaitingDialog()
                            Toast.makeText(
                                this@OPDSActivity,
                                "Загрузка отменена",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    })
                    .build()
            }
        }
        if (!this@OPDSActivity.isFinishing) {
            mShowLoadDialog!!.show()
        }
    }

    private fun showAuthorViewSelect() {
        // получу идентификатор автора
        if (mSelectAuthorViewDialog == null) {
            // создам диалоговое окно
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle(R.string.select_author_view_message)
                .setItems(mAuthorViewTypes) { _: DialogInterface?, which: Int ->
                    loadAuthor(
                        which,
                        mSelectedAuthor!!.uri,
                        mSelectedAuthor!!.name
                    )
                }
            mSelectAuthorViewDialog = dialogBuilder.create()
        }
        if (!this@OPDSActivity.isFinishing) {
            mSelectAuthorViewDialog!!.show()
        }
    }

    private fun showAuthorsSelect(authors: ArrayList<Author>) {
        if (mSelectAuthorsDialog == null) {
            // создам диалоговое окно
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setTitle(R.string.select_authors_choose_message)
            mSelectAuthorsDialog = dialogBuilder
        }
        // получу сисок имён авторов
        val iterator: Iterator<Author> = authors.iterator()
        val authorsList = ArrayList<String?>()
        while (iterator.hasNext()) {
            val a = iterator.next()
            authorsList.add(a.name)
        }
        // покажу список выбора автора
        mSelectAuthorsDialog!!.setItems(authorsList.toTypedArray()) { _: DialogInterface?, i: Int ->
            mSelectedAuthor = authors[i]
            showAuthorViewSelect()
        }
        if (!this@OPDSActivity.isFinishing) {
            mSelectAuthorsDialog!!.show()
        }
    }

    private fun loadAuthor(which: Int, authorId: String?, name: String?) {
        var url: String? = null
        when (which) {
            0 -> {
                url = "/opds/authorsequences/$authorId"
                sBookmarkName = "Автор $name по сериям"
            }
            1 -> {
                url = "/opds/author/$authorId/authorsequenceless"
                sBookmarkName = "Автор $name вне серий"
            }
            2 -> {
                url = "/opds/author/$authorId/alphabet"
                sBookmarkName = "Автор $name по алфавиту"
            }
            3 -> {
                url = "/opds/author/$authorId/time"
                sBookmarkName = "Автор $name по времени"
            }
        }
        if (url != null) {
            scrollToTop()
            doSearch(getBaseOPDSUrl() + url, false)
        }
    }

    override fun onQueryTextChange(s: String): Boolean {
        return false
    }

    inner class TorConnectErrorReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // покажу диалоговое окно с оповещением, что TOR остановлен и кнопкой повторного запуска
            val errorDetails = intent.getStringExtra(TorWebClient.ERROR_DETAILS)
            showTorRestartDialog(errorDetails)
        }
    }

    private fun showTorRestartDialog(incomingErrorDetails: String) {
        hideWaitingDialog()
        if (mTorRestartDialog == null) {
            // создам диалоговое окно
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle(R.string.tor_is_stopped)
                .setMessage(getString(R.string.tor_restart_dialog_message) + incomingErrorDetails)
                .setPositiveButton(R.string.restart_tor_message) { dialog: DialogInterface, _: Int ->
                    App.instance.startTor()
                    dialog.dismiss()
                    // вернусь в основное активити и подожду перезапуска
                    val intent = Intent(this@OPDSActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                }
                .setNegativeButton("Ok", null)
                .setCancelable(true)
            mTorRestartDialog = dialogBuilder.create()
        }
        if (!this@OPDSActivity.isFinishing) {
            mTorRestartDialog!!.show()
        }
    }

    private fun handleLoading() {
        // если приложение только запущено
        if (sFirstLoad) {
            sFirstLoad = false
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (PreferencesHandler.instance.isEInk) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_PAGE_UP) {
                scrollUp()
                return true
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
                scrollDown()
                return true
            }
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mCoverPreviewDialog != null && mCoverPreviewDialog!!.isShowing) {
                mBookTypeDialog!!.dismiss()
                return true
            }
            // если доступен возврат назад- возвращаюсь, если нет- закрываю приложение
            if (!instance!!.isEmpty) {
                val lastPage = instance!!.lastPage
                if (lastPage != null) {
                    // получу значение элемента, по которому кликнули в предыдущем поиске
                    sElementForSelectionIndex = mViewModel!!.lastClickedElement
                    doSearch(lastPage, true)
                    mLastLoadedPageUrl = lastPage
                    return true
                }
            }
            if (mConfirmExit != 0L) {
                if (mConfirmExit > System.currentTimeMillis() - 3000) {
                    // выйду из приложения
                    Log.d("surprise", "OPDSActivity onKeyDown exit")
                    // this.finishAffinity();
                    val startMain = Intent(Intent.ACTION_MAIN)
                    startMain.addCategory(Intent.CATEGORY_HOME)
                    startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(startMain)
                } else {
                    Toast.makeText(
                        this,
                        "Нечего загружать. Нажмите ещё раз для выхода",
                        Toast.LENGTH_SHORT
                    ).show()
                    mConfirmExit = System.currentTimeMillis()
                }
            } else {
                Toast.makeText(
                    this,
                    "Нечего загружать. Нажмите ещё раз для выхода",
                    Toast.LENGTH_SHORT
                ).show()
                mConfirmExit = System.currentTimeMillis()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showDownloadsDialog(downloadLinks: ArrayList<DownloadLink>) {
        val inflate = layoutInflater
        @SuppressLint("InflateParams") val view =
            inflate.inflate(R.layout.confirm_book_type_select, null)
        var checker: SwitchCompat = view.findViewById(R.id.save_only_selected)
        checker.isChecked = PreferencesHandler.instance.saveOnlySelected
        checker = view.findViewById(R.id.reDownload)
        checker.isChecked = PreferencesHandler.instance.isReDownload
        val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
        dialogBuilder.setTitle(R.string.downloads_dialog_header)
        // получу список типов данных
        val linksLength = downloadLinks.size
        val linksArray = arrayOfNulls<String>(linksLength)
        var counter = 0
        var mime: String?
        while (counter < linksLength) {
            mime = downloadLinks[counter].mime
            linksArray[counter] = getMime(mime!!)
            counter++
        }
        dialogBuilder.setItems(linksArray) { dialogInterface: DialogInterface, i: Int ->
            // проверю, выбрано ли сохранение формата загрузки
            val dialog = dialogInterface as Dialog
            var switcher: SwitchCompat = dialog.findViewById(R.id.save_type_selection)
            if (switcher.isChecked) {
                // запомню выбор формата
                Toast.makeText(
                    this@OPDSActivity,
                    "Предпочтительный формат для скачивания сохранён (" + getFullMime(
                        linksArray[i]
                    ) + "). Вы можете сбросить его в настройки +> разное.",
                    Toast.LENGTH_LONG
                ).show()
                PreferencesHandler.instance.favoriteMime = getFullMime(linksArray[i])
            }
            switcher = dialog.findViewById(R.id.save_only_selected)
            PreferencesHandler.instance.saveOnlySelected = switcher.isChecked
            switcher = dialog.findViewById(R.id.reDownload)
            PreferencesHandler.instance.isReDownload = switcher.isChecked
            invalidateMenu()
            // получу сокращённый MIME
            val shortMime = linksArray[i]
            Log.d("surprise", "OPDSActivity: 2056 short mime is $shortMime")
            val longMime = getFullMime(shortMime)
            Log.d("surprise", "OPDSActivity: 2058 long mime is $longMime")
            var counter1 = 0
            val linksLength1 = downloadLinks.size
            var item: DownloadLink
            while (counter1 < linksLength1) {
                item = downloadLinks[counter1]
                Log.d("surprise", "OPDSActivity: 2064 item mime is " + item.mime)
                if (item.mime == longMime) {
                    mViewModel!!.addToDownloadQueue(item)
                    Log.d("surprise", "OPDSActivity: 2064 add " + item.mime)
                    break
                }
                counter1++
            }
            Toast.makeText(
                this@OPDSActivity,
                "Книга добавлена в очередь загрузок",
                Toast.LENGTH_SHORT
            ).show()
        }
            .setView(view)
        val dialog = dialogBuilder.create()
        if (!this@OPDSActivity.isFinishing) {
            dialog.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MainActivity.START_TOR) {
            // перезагружу страницу
            val lastUrl = instance!!.showLastPage()
            if (lastUrl.isNotEmpty()) {
                showLoadWaitingDialog()
                mViewModel!!.request(lastUrl)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun scrollToTop() {
        if (mResultsRecycler != null) {
            mResultsRecycler!!.scrollToPosition(0)
        }
        if (mScrollView != null) {
            mScrollView!!.smoothScrollTo(0, 0)
        }
    }

    private fun invalidateMenu() {
        invalidateOptionsMenu()
        Handler().postDelayed({
            if (sSearchType == SEARCH_TYPE_BOOKS || sSearchType == SEARCH_TYPE_AUTHORS) {
                Log.d("surprise", "OPDSActivity onMenuOpened 1036: showing search")
                mSearchView!!.visibility = View.VISIBLE
            } else {
                Log.d("surprise", "OPDSActivity onMenuOpened 1040: hide search")
                mSearchView!!.visibility = View.INVISIBLE
            }
        }, 100)
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
            "По автору"
        )
        private val authorSortOptions = arrayOf(
            "По имени автора от А",
            "По имени автора от Я",
            "По количеству книг от большего",
            "По количеству книг от меньшего"
        )
        private val otherSortOptions = arrayOf("От А", "От Я")
        const val SEARCH_TYPE_BOOKS = "search books"
        const val SEARCH_TYPE_AUTHORS = "search authors"
        const val SEARCH_TYPE_SEQUENCES = "search sequences"
        const val SEARCH_TYPE_GENRE = "search genre"

        // список результатов поиска жанров
        val sLiveGenresFound = MutableLiveData<ArrayList<Genre>>()

        // список результатов поиска серий
        val sLiveSequencesFound = MutableLiveData<ArrayList<FoundedSequence>>()

        // список результатов поиска авторов
        val sLiveAuthorsFound = MutableLiveData<ArrayList<Author>>()
        val sLiveBooksFound = MutableLiveData<ArrayList<FoundedBook>>()
        var sBooksForDownload: ArrayList<FoundedBook>? = null

        // ссылка для поиска
        val sLiveSearchLink = MutableLiveData<String>()
        val sNothingFound = MutableLiveData<Boolean>()
        val sNewSearch = MutableLiveData<Boolean>()
        var sClickedItemIndex = -1
        val isLoadError = MutableLiveData<Boolean>()
        var sLoadNextPage = MutableLiveData<Boolean>()

        // ВИДЫ ПОИСКА
        const val SEARCH_BOOKS = 1
        const val SEARCH_AUTHORS = 2
        const val SEARCH_GENRE = 3
        const val SEARCH_SEQUENCE = 4
        const val SEARCH_NEW_AUTHORS = 5
        var sSearchType: String? = null
        var sNextPage: String? = null
        private var sFirstLoad = true

        // индекс последнего элемента, по которому кликал пользователь
        var sElementForSelectionIndex = -1
    }
}