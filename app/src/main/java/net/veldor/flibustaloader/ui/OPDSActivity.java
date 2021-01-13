package net.veldor.flibustaloader.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.material.snackbar.Snackbar;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebViewClient;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.adapters.FoundedAuthorsAdapter;
import net.veldor.flibustaloader.adapters.FoundedBooksAdapter;
import net.veldor.flibustaloader.adapters.FoundedGenresAdapter;
import net.veldor.flibustaloader.adapters.FoundedSequencesAdapter;
import net.veldor.flibustaloader.database.entity.Bookmark;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.dialogs.ChangelogDialog;
import net.veldor.flibustaloader.dialogs.GifDialog;
import net.veldor.flibustaloader.http.TorWebClient;
import net.veldor.flibustaloader.interfaces.MyAdapterInterface;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.utils.Grammar;
import net.veldor.flibustaloader.utils.History;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.utils.MyPreferences;
import net.veldor.flibustaloader.utils.URLHelper;
import net.veldor.flibustaloader.utils.XMLHandler;
import net.veldor.flibustaloader.view_models.MainViewModel;
import net.veldor.flibustaloader.workers.DownloadBooksWorker;
import net.veldor.flibustaloader.workers.SearchWorker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;
import static net.veldor.flibustaloader.ui.MainActivity.START_TOR;

public class OPDSActivity extends BaseActivity implements SearchView.OnQueryTextListener {

    public static String sBookmarkName;
    public static final String TARGET_LINK = "target link";
    private static final String[] bookSortOptions = new String[]{"По названию книги", "По размеру", "По количеству скачиваний", "По серии", "По жанру", "По автору"};
    private static final String[] authorSortOptions = new String[]{"По имени автора от А", "По имени автора от Я", "По количеству книг от большего", "По количеству книг от меньшего"};
    private static final String[] otherSortOptions = new String[]{"От А", "От Я"};
    public static final String SEARCH_TYPE_BOOKS = "search books";
    public static final String SEARCH_TYPE_AUTHORS = "search authors";
    public static final String SEARCH_TYPE_SEQUENCES = "search sequences";
    public static final String SEARCH_TYPE_GENRE = "search genre";

    // список результатов поиска жанров
    public static final MutableLiveData<ArrayList<Genre>> sLiveGenresFound = new MutableLiveData<>();

    // список результатов поиска серий
    public static final MutableLiveData<ArrayList<FoundedSequence>> sLiveSequencesFound = new MutableLiveData<>();

    // список результатов поиска авторов
    public static final MutableLiveData<ArrayList<Author>> sLiveAuthorsFound = new MutableLiveData<>();

    public static final MutableLiveData<ArrayList<FoundedBook>> sLiveBooksFound = new MutableLiveData<>();

    public static ArrayList<FoundedBook> sBooksForDownload;

    // ссылка для поиска
    public static final MutableLiveData<String> sLiveSearchLink = new MutableLiveData<>();
    public static final MutableLiveData<Boolean> sNothingFound = new MutableLiveData<>();
    public static final MutableLiveData<Boolean> sNewSearch = new MutableLiveData<>();
    public static int sClickedItemIndex = -1;
    public static final MutableLiveData<Boolean> isLoadError = new MutableLiveData<>();


    private AlertDialog mTorRestartDialog;
    private MainViewModel mViewModel;
    private SearchView mSearchView;
    private ArrayList<String> autocompleteStrings;
    private ArrayAdapter<String> mSearchAdapter;
    private RadioGroup mSearchRadioContainer;
    private Dialog mShowLoadDialog;
    private Button mLoadMoreBtn;
    private TorConnectErrorReceiver mTorConnectErrorReceiver;
    private final String[] mAuthorViewTypes = new String[]{"Книги по сериям", "Книги вне серий", "Книги по алфавиту", "Книги по дате поступления"};
    private AlertDialog mSelectAuthorViewDialog;
    private AlertDialog.Builder mSelectAuthorsDialog;
    private RecyclerView mResultsRecycler;
    private TextView mSearchTitle;
    private AlertDialog.Builder mSelectSequencesDialog;

    // ВИДЫ ПОИСКА
    public static final int SEARCH_BOOKS = 1;
    public static final int SEARCH_AUTHORS = 2;
    public static final int SEARCH_GENRE = 3;
    public static final int SEARCH_SEQUENCE = 4;
    public static final int SEARCH_NEW_AUTHORS = 5;
    private Author mSelectedAuthor;
    private long mConfirmExit;
    private View mRootView;
    private Dialog mMultiplyDownloadDialog;
    private AlertDialog mSelectBookTypeDialog;
    private SearchView.SearchAutoComplete mSearchAutoComplete;
    private ImageButton mForwardBtn, mBackwardBtn;
    private AlertDialog mBookTypeDialog;
    private Dialog mCoverPreviewDialog;
    private AlertDialog BookInfoDialog;
    private FloatingActionMenu mFab;
    private AlertDialog mDownloadSelectedDialog;
    private LiveData<Boolean> mSuccessSelect;
    public static String sSearchType;
    public static String sNextPage;
    private boolean mAddToLoaded;
    private static boolean sFirstLoad = true;
    private NestedScrollView mScrollView;
    private RadioButton mSearchBooksButton;
    private RadioButton mSearchAuthorsButton;
    private SwitchCompat mMassLoadSwitcher;
    private boolean mActivityVisible = true;
    // индекс последнего элемента, по которому кликал пользователь
    public static Integer sElementForSelectionIndex = -1;
    private Button mShowAuthorsListActivator;
    private TextView mConnectionTypeView;
    private AlertDialog mPageNotLoadedDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_opds_activity);
        setupInterface();
        setupObservers();

        // добавлю viewModel
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        checkSearchType();

        // проверю очередь загрузки
        checkDownloadQueue();

        mShowAuthorsListActivator = findViewById(R.id.showAuthorsListButton);

        if (mShowAuthorsListActivator != null) {
            mShowAuthorsListActivator.setOnClickListener(v -> doSearch(URLHelper.getBaseOPDSUrl() + "/opds/authorsindex", false));
        }

        // определю кнопки прыжков по результатам
        mLoadMoreBtn = findViewById(R.id.load_more_button);

        // кнопка перехода вперёд
        mForwardBtn = findViewById(R.id.forward_btn);
        mBackwardBtn = findViewById(R.id.backward_btn);

        mBackwardBtn.setOnClickListener(v -> {
            if (History.getInstance().isEmpty()) {
                String lastPage = History.getInstance().getLastPage();
                if (lastPage != null) {
                    doSearch(lastPage, false);
                    return;
                }
            }
            if (mConfirmExit != 0) {
                if (mConfirmExit > System.currentTimeMillis() - 3000) {
                    // выйду из приложения
                    Log.d("surprise", "OPDSActivity onKeyDown exit");
                    OPDSActivity.this.finishAffinity();
                } else {
                    Toast.makeText(OPDSActivity.this, "Нечего загружать. Нажмите ещё раз для выхода", Toast.LENGTH_SHORT).show();
                    mConfirmExit = System.currentTimeMillis();
                }
            } else {
                Toast.makeText(OPDSActivity.this, "Нечего загружать. Нажмите ещё раз для выхода", Toast.LENGTH_SHORT).show();
                mConfirmExit = System.currentTimeMillis();
            }
        });

        mForwardBtn.setOnClickListener(v -> {
            scrollToTop();
            if (sNextPage != null && !sNextPage.isEmpty()) {
                doSearch(sNextPage, false);
            } else {
                Toast.makeText(OPDSActivity.this, "Результаты закончились", Toast.LENGTH_SHORT).show();
            }
        });

        // зарегистрирую получатель ошибки подключения к TOR
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyWebViewClient.TOR_CONNECT_ERROR_ACTION);
        mTorConnectErrorReceiver = new TorConnectErrorReceiver();
        registerReceiver(mTorConnectErrorReceiver, filter);
        handleLoading();

        // создам тестовый массив строк для автозаполнения
        autocompleteStrings = mViewModel.getSearchAutocomplete();

        // при нажатии кнопки назад- убираем последний элемент истории и переходим на предпоследний
        mLoadMoreBtn.setOnClickListener(view -> {
            // загружаю следующую страницу
            mAddToLoaded = true;
            doSearch(sNextPage, false);
        });
        // добавлю отслеживание изменения ваианта поиска

        mSearchBooksButton = findViewById(R.id.searchBook);
        if (mSearchBooksButton != null) {
            mSearchBooksButton.setOnClickListener(v -> {
                mShowAuthorsListActivator.setVisibility(View.GONE);
                mMassLoadSwitcher.setVisibility(View.VISIBLE);
                Toast.makeText(OPDSActivity.this, "Ищем книги", Toast.LENGTH_SHORT).show();
                sSearchType = SEARCH_TYPE_BOOKS;
                if (mSearchView != null) {
                    Log.d("surprise", "OPDSActivity onCreate 242: show search icon");
                    mSearchView.setVisibility(View.VISIBLE);
                    if (MyPreferences.getInstance().isAutofocusSearch()) {
                        mSearchView.setIconified(false);
                        mSearchView.requestFocusFromTouch();
                    }
                }
            });
        }

        mSearchAuthorsButton = findViewById(R.id.searchAuthor);
        mSearchAuthorsButton.setOnClickListener(v -> {
            mMassLoadSwitcher.setVisibility(View.GONE);
            mShowAuthorsListActivator.setVisibility(View.VISIBLE);
            Toast.makeText(OPDSActivity.this, "Ищем авторов", Toast.LENGTH_SHORT).show();
            sSearchType = SEARCH_TYPE_AUTHORS;
            if (mSearchView != null) {
                Log.d("surprise", "OPDSActivity onCreate 242: show search icon");
                mSearchView.setVisibility(View.VISIBLE);
                if (MyPreferences.getInstance().isAutofocusSearch()) {
                    mSearchView.setIconified(false);
                    mSearchView.requestFocusFromTouch();
                }
            }
        });

        RadioButton searchGenresButton = findViewById(R.id.searchGenre);
        searchGenresButton.setOnClickListener(v -> {
            mShowAuthorsListActivator.setVisibility(View.GONE);
            mMassLoadSwitcher.setVisibility(View.GONE);
            Toast.makeText(OPDSActivity.this, "Ищу жанры", Toast.LENGTH_SHORT).show();
            sSearchType = SEARCH_TYPE_GENRE;
            sBookmarkName = "Все жанры";
            if (mSearchView != null) {
                mSearchView.setVisibility(View.INVISIBLE);
            }
            showLoadWaitingDialog();
            doSearch(URLHelper.getSearchRequest(SEARCH_TYPE_GENRE, "genres"), false);
            scrollToTop();
            mFab.setVisibility(View.GONE);
        });

        RadioButton searchSequencesButton = findViewById(R.id.searchSequence);
        searchSequencesButton.setOnClickListener(v -> {
            mMassLoadSwitcher.setVisibility(View.GONE);
            Toast.makeText(OPDSActivity.this, "Ищу серии", Toast.LENGTH_SHORT).show();
            sSearchType = SEARCH_TYPE_SEQUENCES;
            sBookmarkName = "Все серии";
            if (mSearchView != null) {
                mSearchView.setVisibility(View.INVISIBLE);
            }
            showLoadWaitingDialog();
            doSearch(URLHelper.getSearchRequest(SEARCH_TYPE_SEQUENCES, "sequencesindex"), false);
            scrollToTop();
            mFab.setVisibility(View.GONE);
        });

        // заголовок поиска
        LiveData<String> searchTitle = App.getInstance().mSearchTitle;
        searchTitle.observe(this, s -> {
            if (s != null) {
                mSearchTitle.setVisibility(View.VISIBLE);
                mSearchTitle.setText(s);
                changeTitle(s);
            } else {
                mSearchTitle.setVisibility(View.GONE);
            }
        });

        // добавлю отслеживание получения списка ссылок на скачивание
        LiveData<ArrayList<DownloadLink>> downloadLinks = App.getInstance().mDownloadLinksList;
        downloadLinks.observe(this, downloadLinks1 -> {
            if (downloadLinks1 != null && downloadLinks1.size() > 0 && mActivityVisible) {
                if (downloadLinks1.size() == 1) {
                    // добавлю книгу в очередь скачивания
                    mViewModel.addToDownloadQueue(downloadLinks1.get(0));
                    Toast.makeText(OPDSActivity.this, R.string.book_added_to_schedule_message, Toast.LENGTH_LONG).show();
                } else {
                    // покажу диалог для выбора ссылки для скачивания
                    showDownloadsDialog(downloadLinks1);
                }
            }
        });
        // добавлю отслеживание получения списка ссылок на скачивание
        LiveData<ArrayList<FoundedSequence>> foundedSequences = App.getInstance().mSelectedSequences;
        foundedSequences.observe(this, sequences -> {
            if (sequences != null) {
                // покажу диалог для выбора ссылки для скачивания
                showSequenceSelectDialog(sequences);
            }
        });

        // добавлю отслеживание выбора типа отображения автора
        LiveData<Author> selectedAuthor = App.getInstance().mSelectedAuthor;
        selectedAuthor.observe(this, author -> {
            if (author != null) {
                // если выбран конкретный автор- отображу выбор показа его книг, если выбрана группа авторов- загружу следующую страницу выбора
                if (author.id != null && author.id.startsWith("tag:authors")) {
                    doSearch(URLHelper.getBaseOPDSUrl() + author.uri, false);
                } else {
                    mSelectedAuthor = author;
                    showAuthorViewSelect();
                }
            }
        });

        // добавлю отслеживание выбора автора
        LiveData<ArrayList<Author>> authorsList = App.getInstance().mSelectedAuthors;
        authorsList.observe(this, authors -> {
            if (authors != null) {
                showAuthorsSelect(authors);
            }
        });

        // добавлю отслеживание выбора серии
        LiveData<FoundedSequence> selectedSequence = App.getInstance().mSelectedSequence;
        selectedSequence.observe(this, foundedSequence -> {
            if (foundedSequence != null) {
                scrollToTop();
                SearchWorker.sSequenceName = foundedSequence.title;
                doSearch(URLHelper.getBaseOPDSUrl() + foundedSequence.link, false);
            }
        });


        // добавлю обсерверы
        addObservers();
        checkUpdates();

        // если передана ссылка- перейду по ней
        Log.d("surprise", "check link");
        Intent intent = getIntent();
        String link = intent.getStringExtra(TARGET_LINK);
        if (link != null) {
            Log.d("surprise", "have link");
            doSearch(link, false);
        }
    }

    @Override
    protected void setupObservers() {
        super.setupObservers();
        isLoadError.observe(this, hasError -> {
            if (hasError) {
                // покажу окошко и сообщу, что загрузка не удалась
                showPageNotLoadedDialog();
            }
        });
    }

    private void showPageNotLoadedDialog() {
        if (mPageNotLoadedDialog == null) {
            mPageNotLoadedDialog = new AlertDialog.Builder(this, R.style.MyDialogStyle)
                    .setTitle(getString(R.string.error_load_page_title))
                    .setMessage(getString(R.string.error_load_page_message))
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }
        mPageNotLoadedDialog.show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String link = intent.getStringExtra(TARGET_LINK);
        if (link != null) {
            Log.d("surprise", "have link");
            doSearch(link, false);
        }
    }

    private void checkSearchType() {
        // получу выбранный тип поиска
        if (mSearchRadioContainer.getCheckedRadioButtonId() == R.id.searchBook) {
            sSearchType = SEARCH_TYPE_BOOKS;
        } else if (mSearchRadioContainer.getCheckedRadioButtonId() == R.id.searchAuthor) {
            sSearchType = SEARCH_TYPE_AUTHORS;
        } else if (mSearchRadioContainer.getCheckedRadioButtonId() == R.id.searchSequence) {
            sSearchType = SEARCH_TYPE_SEQUENCES;
        } else if (mSearchRadioContainer.getCheckedRadioButtonId() == R.id.searchGenre) {
            sSearchType = SEARCH_TYPE_GENRE;
        }
    }

    private void checkDownloadQueue() {
        Boolean queue = mViewModel.checkDownloadQueue();
        if (queue) {
            // продолжу загрузку книг
            final LiveData<List<BooksDownloadSchedule>> schedule = App.getInstance().mDatabase.booksDownloadScheduleDao().getAllBooksLive();
            schedule.observe(this, booksDownloadSchedule -> {
                if (booksDownloadSchedule != null) {
                    if (booksDownloadSchedule.size() > 0) {
                        // покажу диалог с предложением докачать файлы
                        schedule.removeObservers(OPDSActivity.this);
                        continueDownload();
                    }
                }
            });
        } else {
            Log.d("surprise", "OPDSActivity checkDownloadQueue: download queue is empty");
        }
    }

    private void continueDownload() {
        // проверю, нет ли активных процессов скачивания
        if (DownloadBooksWorker.noActiveDownloadProcess()) {
            showReDownloadDialog();
        } else {
            Log.d("surprise", "OPDSActivity continueDownload: download still in progress");
        }
    }

    private void showReDownloadDialog() {
        if (!OPDSActivity.this.isFinishing()) {
            new AlertDialog.Builder(OPDSActivity.this, R.style.MyDialogStyle)
                    .setTitle(R.string.re_download_dialog_header_message)
                    .setMessage(R.string.re_download_dialog_body_message)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                        mViewModel.initiateMassDownload();
                        Toast.makeText(OPDSActivity.this, R.string.download_continued_message, Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton(R.string.no, null)
                    .create().show();
        }
    }


    @Override
    protected void setupInterface() {

        super.setupInterface();

        // определю тип соединения
        mConnectionTypeView = findViewById(R.id.connectionType);

        // скрою переход на данное активити
        Menu menuNav = mNavigationView.getMenu();
        MenuItem item = menuNav.findItem(R.id.goToOPDS);
        item.setEnabled(false);
        item.setChecked(true);

        // Обёртка просмотра
        mScrollView = findViewById(R.id.subscriptions_layout);
        // рециклеры
        mResultsRecycler = findViewById(R.id.resultsList);
        if (App.getInstance().isLinearLayout()) {
            mResultsRecycler.setLayoutManager(new LinearLayoutManager(OPDSActivity.this));
        } else {
            mResultsRecycler.setLayoutManager(new GridLayoutManager(OPDSActivity.this, 2));
        }

        // варианты поиска
        mSearchRadioContainer = findViewById(R.id.subscribe_type);

        mSearchTitle = findViewById(R.id.search_title);

        mRootView = findViewById(R.id.rootView);

        if (mRootView != null) {
            if (MyPreferences.getInstance().isEInk()) {
                //Toast.makeText(this, "Читалка", Toast.LENGTH_SHORT).show();
                Log.d("surprise", "OPDSActivity setupInterface 529: use reader");
            } else {
                // назначу фон
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mRootView.setBackground(ContextCompat.getDrawable(this, R.drawable.back_2));
                } else {
                    mRootView.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.back_2, null));
                }
            }
        }

        mMassLoadSwitcher = findViewById(R.id.showAllSwitcher);
        if (mMassLoadSwitcher != null) {
            mMassLoadSwitcher.setChecked(App.getInstance().isDownloadAll());
            mMassLoadSwitcher.setOnCheckedChangeListener((compoundButton, b) -> App.getInstance().switchDownloadAll());
        }
        mFab = findViewById(R.id.floatingMenu);
        mFab.setVisibility(View.GONE);

        View downloadAllButton = findViewById(R.id.fabDownloadAll);
        if (downloadAllButton != null) {
            downloadAllButton.setOnClickListener(view -> {
                String favoriteFormat = App.getInstance().getFavoriteMime();
                if (favoriteFormat != null && !favoriteFormat.isEmpty()) {
                    downloadAllBooks();
                } else {
                    // покажу диалог выбора предпочтительнго типа скачивания
                    // сброшу отслеживание удачного выбора типа книги
                    App.getInstance().mTypeSelected.setValue(false);
                    selectBookTypeDialog();
                    LiveData<Boolean> successSelect = App.getInstance().mTypeSelected;
                    successSelect.observe(OPDSActivity.this, selected -> {
                        if (selected) {
                            downloadAllBooks();
                        }
                    });
                }
                mFab.close(true);
            });
        }
        View downloadUnloadedButton = findViewById(R.id.fabDOwnloadUnloaded);
        if (downloadUnloadedButton != null) {
            downloadUnloadedButton.setOnClickListener(view -> {
                String favoriteFormat = App.getInstance().getFavoriteMime();
                if (favoriteFormat != null && !favoriteFormat.isEmpty()) {
                    downloadUnloaded();
                } else {
                    // покажу диалог выбора предпочтительнго типа скачивания
                    // сброшу отслеживание удачного выбора типа книги
                    App.getInstance().mTypeSelected.setValue(false);
                    selectBookTypeDialog();
                    LiveData<Boolean> successSelect = App.getInstance().mTypeSelected;
                    successSelect.observe(OPDSActivity.this, selected -> {
                        if (selected) {
                            downloadUnloaded();
                        }
                    });
                }
                mFab.close(true);
            });
        }
        View downloadSelectButton = findViewById(R.id.fabDOwnloadSelected);
        if (downloadSelectButton != null) {
            downloadSelectButton.setOnClickListener(view -> {
                String favoriteFormat = App.getInstance().getFavoriteMime();
                if (favoriteFormat != null && !favoriteFormat.isEmpty()) {
                    showDownloadSelectedDialog();
                } else {
                    // покажу диалог выбора предпочтительнго типа скачивания
                    // сброшу отслеживание удачного выбора типа книги
                    App.getInstance().mTypeSelected.setValue(false);
                    selectBookTypeDialog();
                    mSuccessSelect = App.getInstance().mTypeSelected;
                    mSuccessSelect.observe(OPDSActivity.this, selected -> {
                        if (selected) {
                            showDownloadSelectedDialog();
                            mSuccessSelect.removeObservers(OPDSActivity.this);
                        }
                    });
                }
                mFab.close(true);
            });
        }


        showChangesList();
    }

    private void showChangesList() {
        // покажу список изменений, если он ещё не показан для этой версии
        if (MyPreferences.getInstance().isShowChanges()) {
            new ChangelogDialog.Builder(this).build().show();
            MyPreferences.getInstance().setChangesViewed();
        }
    }

    private void downloadUnloaded() {
        LiveData<WorkInfo> status = mViewModel.downloadAll(true);
        observeBookScheduleAdd(status);
    }

    private void showDownloadSelectedDialog() {
        // добавлю книги в список для загрузки
        if (mResultsRecycler.getAdapter() instanceof FoundedBooksAdapter) {
            // получу названия книг
            ArrayList<FoundedBook> books = ((FoundedBooksAdapter) mResultsRecycler.getAdapter()).getItems();
            if (books != null && books.size() > 0) {
                String[] variants = new String[books.size()];
                int counter = 0;
                for (FoundedBook fb : books) {
                    variants[counter] = String.format(Locale.ENGLISH, "%s \n %s \n %s \n", fb.name, fb.format, (fb.translate.isEmpty() ? "" : fb.translate));
                    counter++;
                }
                mDownloadSelectedDialog = new AlertDialog.Builder(this, R.style.MyDialogStyle)
                        .setTitle(R.string.select_books_for_download_message)
                        .setMultiChoiceItems(variants, null, (dialogInterface, i, b) -> {
                        })
                        .setPositiveButton(R.string.download_selected_message, (dialogInterface, i) -> {
                            mDownloadSelectedDialog.getListView().getCheckedItemCount();
                            SparseBooleanArray ids = mDownloadSelectedDialog.getListView().getCheckedItemPositions();
                            if (ids.size() > 0) {
                                LiveData<WorkInfo> status = mViewModel.downloadSelected(ids);
                                observeBookScheduleAdd(status);
                            } else {
                                Toast.makeText(OPDSActivity.this, R.string.books_not_selected_message, Toast.LENGTH_LONG).show();
                            }
                        })
                        .create();
                if (!OPDSActivity.this.isFinishing()) {
                    mDownloadSelectedDialog.show();
                }
            }
        } else {
            Toast.makeText(this, R.string.books_not_found_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void observeBookScheduleAdd(final LiveData<WorkInfo> status) {
        status.observe(this, workInfo -> {
            if (workInfo != null && workInfo.getState() == SUCCEEDED) {
                Toast.makeText(OPDSActivity.this, "Книги добавлены в очередь скачивания", Toast.LENGTH_LONG).show();
                // запущу скачивание
                if (MyPreferences.getInstance().isDownloadAutostart()) {
                    mViewModel.initiateMassDownload();
                }
                startActivity(new Intent(getBaseContext(), ActivityBookDownloadSchedule.class));
                status.removeObservers(OPDSActivity.this);
            }
        });
    }

    private void checkUpdates() {
        if (App.getInstance().isCheckUpdate()) {
            // проверю обновления
            final LiveData<Boolean> version = mViewModel.startCheckUpdate();
            version.observe(this, aBoolean -> {
                if (aBoolean != null && aBoolean) {
                    // показываю Snackbar с уведомлением
                    makeUpdateSnackbar();
                }
                version.removeObservers(OPDSActivity.this);
            });
        }
    }


    private void makeUpdateSnackbar() {
        Snackbar updateSnackbar = Snackbar.make(mRootView, getString(R.string.snackbar_found_update_message), Snackbar.LENGTH_INDEFINITE);
        updateSnackbar.setAction(getString(R.string.snackbar_update_action_message), v -> mViewModel.initializeUpdate());
        updateSnackbar.setActionTextColor(getResources().getColor(android.R.color.white));
        updateSnackbar.show();
    }

    private void addObservers() {
        // отслеживание нового поиска
        sNewSearch.observe(this, aBoolean -> {
            // если есть адаптер и это не адаптер с книгами- сброшу его значение
            if (aBoolean && mResultsRecycler.getAdapter() instanceof MyAdapterInterface) {
                ((MyAdapterInterface) mResultsRecycler.getAdapter()).clearList();
            }

            if (!aBoolean) {

                new Handler().postDelayed(() -> {
                    if (sElementForSelectionIndex >= 0 && mResultsRecycler.getAdapter() != null && mResultsRecycler.getAdapter().getItemCount() > sElementForSelectionIndex && mResultsRecycler.getLayoutManager() != null) {
                        float y = mResultsRecycler.getY() + mResultsRecycler.getChildAt(sElementForSelectionIndex).getY();
                        mScrollView.smoothScrollTo(0, (int) y);
                    }
                    // очищу переменную с элементом
                    sElementForSelectionIndex = -1;
                }, 500);
            }

        });

        // отслеживание отсутствия результатов
        sNothingFound.observe(this, result -> {
            if (result) {
                Toast.makeText(OPDSActivity.this, "Ничего не найдено", Toast.LENGTH_SHORT).show();
                if (mResultsRecycler.getAdapter() != null) {
                    if (mResultsRecycler.getAdapter() instanceof FoundedBooksAdapter) {
                        ((FoundedBooksAdapter) mResultsRecycler.getAdapter()).setContent(null, false);
                    } else if (mResultsRecycler.getAdapter() instanceof FoundedAuthorsAdapter) {
                        ((FoundedAuthorsAdapter) mResultsRecycler.getAdapter()).setContent(null);
                    }
                }

            }
        });

        // отслеживание загруженной книги
        LiveData<String> downloadedBook = App.getInstance().mLiveDownloadedBookId;
        downloadedBook.observe(this, downloadedBookId -> {
            if (downloadedBookId != null && mResultsRecycler.getAdapter() instanceof FoundedBooksAdapter) {
                ((FoundedBooksAdapter) mResultsRecycler.getAdapter()).bookDownloaded(downloadedBookId);
            }
            // отмечу в книгах для закачки, что книга закачана

        });

        // отслеживание отображения обложки книги
        LiveData<FoundedBook> bookWithCover = App.getInstance().mShowCover;
        bookWithCover.observe(OPDSActivity.this, foundedBook -> {
            if (foundedBook != null && foundedBook.previewUrl != null) {
                Log.d("surprise", "onChanged: show again");
                showPreview(foundedBook);
            }
        });

        // получена прямая ссылка для поиска
        sLiveSearchLink.observe(this, s -> {
            if (s != null && !s.isEmpty()) {
                doSearch(URLHelper.getBaseOPDSUrl() + s, false);
            }
        });

        // отслежу получение списка жанров
        sLiveGenresFound.observe(this, genres -> {
            hideLoadingButtons();
            sSearchType = SEARCH_TYPE_GENRE;
            if (genres != null) {
                // получен список жанров. Проверю, если адаптером является адаптер жанров-
                // заменю значения, иначе- заменю адаптер
                if (mResultsRecycler.getAdapter() != null) {
                    if (mResultsRecycler.getAdapter() instanceof FoundedGenresAdapter) {
                        ((FoundedGenresAdapter) mResultsRecycler.getAdapter()).setContent(genres);
                    } else {
                        mResultsRecycler.setAdapter(new FoundedGenresAdapter(genres));
                    }
                } else {
                    mResultsRecycler.setAdapter(new FoundedGenresAdapter(genres));
                }
            }
        });
        // отслежу получение списка серий
        sLiveSequencesFound.observe(this, sequences -> {
            hideLoadingButtons();
            sSearchType = SEARCH_TYPE_SEQUENCES;
            if (sequences != null) {
                // получен список серий. Проверю, если адаптером является адаптер серий-
                // заменю значения, иначе- заменю адаптер
                if (mResultsRecycler.getAdapter() != null) {
                    if (mResultsRecycler.getAdapter() instanceof FoundedSequencesAdapter) {
                        ((FoundedSequencesAdapter) mResultsRecycler.getAdapter()).setContent(sequences);
                    } else {
                        mResultsRecycler.setAdapter(new FoundedSequencesAdapter(sequences));
                    }
                } else {
                    mResultsRecycler.setAdapter(new FoundedSequencesAdapter(sequences));
                }
            }
        });

        sLiveAuthorsFound.observe(this, authors -> {
            if (authors != null) {
                if (!mSearchAuthorsButton.isChecked()) {
                    mSearchAuthorsButton.setChecked(true);
                }
                App.sSearchType = SEARCH_AUTHORS;
            }
            hideLoadingButtons();
            // найденные авторы сброшены
            if (mResultsRecycler.getAdapter() != null) {
                if (mResultsRecycler.getAdapter() instanceof FoundedAuthorsAdapter) {
                    ((FoundedAuthorsAdapter) mResultsRecycler.getAdapter()).setContent(authors);
                } else if (authors != null) {
                    mResultsRecycler.setAdapter(new FoundedAuthorsAdapter(authors));
                }
            } else {
                Log.d("surprise", "OPDSActivity onChanged 714: append new authors adapter");
                if (authors != null) {
                    mResultsRecycler.setAdapter(new FoundedAuthorsAdapter(authors));
                }
            }
        });

        sLiveBooksFound.observe(this, books -> {
            if (books != null) {
                if (!mSearchBooksButton.isChecked()) {
                    mSearchBooksButton.setChecked(true);
                }
            }
            App.sSearchType = SEARCH_BOOKS;
            // найденные книги сброшены
            if (mResultsRecycler.getAdapter() != null) {
                if (mResultsRecycler.getAdapter() instanceof FoundedBooksAdapter) {
                    ((FoundedBooksAdapter) mResultsRecycler.getAdapter()).setContent(books, mAddToLoaded);
                    mAddToLoaded = false;
                } else {
                    mResultsRecycler.setAdapter(new FoundedBooksAdapter(books));
                }
            } else {
                mResultsRecycler.setAdapter(new FoundedBooksAdapter(books));
            }
            if (books != null && books.size() > 0) {
                mFab.setVisibility(View.VISIBLE);
                if (!App.getInstance().isDownloadAll() && sNextPage != null) {
                    mLoadMoreBtn.setVisibility(View.VISIBLE);
                    mForwardBtn.setVisibility(View.VISIBLE);
                    mBackwardBtn.setVisibility(View.VISIBLE);
                } else {
                    hideLoadingButtons();
                }
            } else {
                hideLoadingButtons();
            }
        });

        // добавлю отслеживание показа информации о книге
        LiveData<FoundedBook> selectedBook = App.getInstance().mSelectedBook;
        selectedBook.observe(this, book -> {
            if (book != null) {
                Log.d("surprise", "OPDSActivity onChanged show book info");
                BookInfoDialog = new AlertDialog.Builder(OPDSActivity.this, R.style.MyDialogStyle)
                        .setTitle(book.name)
                        .setMessage(Grammar.textFromHtml(book.bookInfo))
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                if (!OPDSActivity.this.isFinishing()) {
                    BookInfoDialog.show();
                }
            }
        });

        // добавлю отслеживание контекстного меню для книги
        LiveData<FoundedBook> contextBook = App.getInstance().mContextBook;
        contextBook.observe(this, foundedBook -> {
            // добавлю хранилище для книги
            final FoundedBook book = foundedBook;
            if (foundedBook != null) {
                // покажу контекстное меню
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(OPDSActivity.this, R.style.MyDialogStyle);
                String[] items = new String[]{"Пометить книгу как прочитанную", "Показать страницу книги"};
                dialogBuilder.setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // отмечу книгу как прочитанную
                            mViewModel.setBookRead(book);
                            Toast.makeText(OPDSActivity.this, "Книга отмечена как прочитанная", Toast.LENGTH_LONG).show();
                            // оповещу адаптер об этом
                            if (mResultsRecycler.getAdapter() instanceof FoundedBooksAdapter) {
                                ((FoundedBooksAdapter) mResultsRecycler.getAdapter()).setBookReaded(book);
                            }
                            break;
                        case 1:
                            showPage(book);
                    }
                });
                mBookTypeDialog = dialogBuilder.create();
                if (!OPDSActivity.this.isFinishing()) {
                    mBookTypeDialog.show();
                }
            }
        });

        LiveData<Author> authorNewBooks = App.getInstance().mAuthorNewBooks;
        authorNewBooks.observe(this, author -> {
            if (author != null)
                doSearch(URLHelper.getBaseOPDSUrl() + "/opds/new/0/newauthors/" + author.id.substring(22), false);
        });

        // отслеживание статуса загрузки книг
        LiveData<String> multiplyDownloadStatus = App.getInstance().mMultiplyDownloadStatus;
        multiplyDownloadStatus.observe(this, s -> {
            if (s != null && !s.isEmpty() && mMultiplyDownloadDialog != null) {
                Window window = mMultiplyDownloadDialog.getWindow();
                if (window != null) {
                    TextView dialogText = window.findViewById(R.id.title);
                    dialogText.setText(s);
                }
            }
        });

        // отслеживание незавершённого поиска
        WorkInfo workStatus = App.getInstance().mSearchWork.getValue();
        if (workStatus != null) {
            if (workStatus.getState() == RUNNING || workStatus.getState() == ENQUEUED) {
                showLoadWaitingDialog();
            }
        }
        // отслеживание незавершённого скачивания
        if (App.getInstance().mDownloadAllWork != null) {
            workStatus = App.getInstance().mDownloadAllWork.getValue();
            if (workStatus != null) {
                if (workStatus.getState() == RUNNING || workStatus.getState() == ENQUEUED) {
                    showMultiplyDownloadDialog();
                    String status = App.getInstance().mMultiplyDownloadStatus.getValue();
                    if (status != null && !status.isEmpty()) {
                        Window window = mMultiplyDownloadDialog.getWindow();
                        if (window != null) {
                            TextView dialogText = window.findViewById(R.id.title);
                            dialogText.setText(status);
                        }
                    }
                }
            }
            observeBooksDownload();
        }

        LiveData<String> loadStatus = App.getInstance().mLoadAllStatus;
        loadStatus.observe(this, s -> {
            if (s != null && !s.isEmpty() && mShowLoadDialog != null) {
                // изменю сообщение
                Window window = mShowLoadDialog.getWindow();
                if (window != null) {
                    TextView dialogText = window.findViewById(R.id.title);
                    if (dialogText != null) {
                        dialogText.setText(s);
                    }
                }
            }
            if (s != null && !s.isEmpty() && mMultiplyDownloadDialog != null) {
                // изменю сообщение
                Window window = mMultiplyDownloadDialog.getWindow();
                if (window != null) {
                    TextView dialogText = window.findViewById(R.id.title);
                    if (dialogText != null) {
                        dialogText.setText(s);
                    }
                }
            }
        });
    }

    private void hideLoadingButtons() {
        mLoadMoreBtn.setVisibility(View.GONE);
        mForwardBtn.setVisibility(View.GONE);
        mBackwardBtn.setVisibility(View.GONE);
    }

    private void showPreview(FoundedBook foundedBook) {
        if (!OPDSActivity.this.isFinishing()) {
            Log.d("surprise", "OPDSActivity showPreview: preview url is " + foundedBook.previewUrl);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
            LayoutInflater inflater = getLayoutInflater();
            @SuppressLint("InflateParams") View dialogLayout = inflater.inflate(R.layout.book_cover, null);
            ImageView imageContainer = dialogLayout.findViewById(R.id.cover_view);

            Glide
                    .with(imageContainer)
                    .load("https://flibusta.appspot.com" + foundedBook.previewUrl)
                    .into(imageContainer);

            dialogBuilder
                    .setView(dialogLayout)
                    .setCancelable(true)
                    .setPositiveButton("Ок", null)
                    .create()
                    .show();
        }
    }

    private void showPage(FoundedBook book) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.setData(Uri.parse(URLHelper.getBaseUrl() + book.bookLink));
        intent.putExtra(WebViewActivity.CALLED, true);
        startActivity(intent);
    }

    private void observeBooksDownload() {
        // отслежу загрузку книг
        final LiveData<WorkInfo> booksDownloadStatus = App.getInstance().mDownloadAllWork;
        if (booksDownloadStatus != null) {
            booksDownloadStatus.observe(this, workInfo -> {
                if (workInfo != null) {
                    if (workInfo.getState() == SUCCEEDED && !App.getInstance().mDownloadsInProgress) {
                        Toast.makeText(OPDSActivity.this, "Все книги загружены (кажется)", Toast.LENGTH_LONG).show();
                        // работа закончена, закрою диалог и выведу тост
                        if (mMultiplyDownloadDialog != null) {
                            mMultiplyDownloadDialog.dismiss();
                            booksDownloadStatus.removeObservers(OPDSActivity.this);
                        }
                    }
                    // если есть недогруженные книги- выведу Snackbar, где уведомплю об этом
                    if (App.getInstance().mBooksDownloadFailed.size() > 0) {
                        Snackbar updateSnackbar = Snackbar.make(mRootView, "Есть недокачанные книги", Snackbar.LENGTH_INDEFINITE);
                        updateSnackbar.setAction("Попробовать ещё раз", v -> {

                        });
                        updateSnackbar.setActionTextColor(getResources().getColor(android.R.color.white));
                        updateSnackbar.show();
                    }
                }
            });
        }
    }

    private void showSequenceSelectDialog(final ArrayList<FoundedSequence> sequences) {
        if (mSelectSequencesDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
            dialogBuilder.setTitle(R.string.select_sequences_choose_message);
            mSelectSequencesDialog = dialogBuilder;
        }
        // получу сисок серий
        Iterator<FoundedSequence> iterator = sequences.iterator();
        ArrayList<String> sequencesList = new ArrayList<>();
        while (iterator.hasNext()) {
            FoundedSequence s = iterator.next();
            sequencesList.add(s.title);

        }
        // покажу список выбора серии
        mSelectSequencesDialog.setItems(sequencesList.toArray(new String[0]), (dialogInterface, i) -> App.getInstance().mSelectedSequence.postValue(sequences.get(i)));
        if (!OPDSActivity.this.isFinishing()) {
            mSelectSequencesDialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        App.getInstance().mContextBook.setValue(null);
        App.getInstance().mSelectedBook.setValue(null);
        App.getInstance().mSelectedAuthor.setValue(null);
        App.getInstance().mSelectedAuthors.setValue(null);
        App.getInstance().mDownloadLinksList.setValue(null);
        App.getInstance().mSelectedSequence.setValue(null);
        App.getInstance().mSelectedSequences.setValue(null);

        if (BookInfoDialog != null) {
            BookInfoDialog.dismiss();
            BookInfoDialog = null;
        }
        if (mShowLoadDialog != null) {
            mShowLoadDialog.dismiss();
            mShowLoadDialog = null;
        }
        if (mMultiplyDownloadDialog != null) {
            mMultiplyDownloadDialog.dismiss();
            mMultiplyDownloadDialog = null;
        }
        if (mSelectBookTypeDialog != null) {
            mSelectBookTypeDialog.dismiss();
            mSelectBookTypeDialog = null;
        }
        if (mBookTypeDialog != null) {
            mBookTypeDialog.dismiss();
            mBookTypeDialog = null;
        }
        if (mCoverPreviewDialog != null) {
            mCoverPreviewDialog.dismiss();
            mCoverPreviewDialog = null;
        }
        mActivityVisible = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mConnectionTypeView != null) {
            if (App.getInstance().isExternalVpn()) {
                mConnectionTypeView.setText(getString(R.string.vpn_title));
                mConnectionTypeView.setBackgroundColor(Color.parseColor("#03A9F4"));
            } else {
                mConnectionTypeView.setText(getString(R.string.tor_title));
                mConnectionTypeView.setBackgroundColor(Color.parseColor("#4CAF50"));
            }
        }
        if (mResultsRecycler != null && mResultsRecycler.getAdapter() instanceof MyAdapterInterface) {
            mResultsRecycler.getAdapter().notifyDataSetChanged();
        }
        mActivityVisible = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        App.getInstance().mShowCover.setValue(null);
        if (mTorConnectErrorReceiver != null) {
            unregisterReceiver(mTorConnectErrorReceiver);
        }
        App.getInstance().mSelectedAuthor.setValue(null);
        App.getInstance().mSelectedAuthors.setValue(null);
        App.getInstance().mDownloadLinksList.setValue(null);
        App.getInstance().mSelectedSequence.setValue(null);
        App.getInstance().mSelectedSequences.setValue(null);
    }


    private void hideWaitingDialog() {
        if (mShowLoadDialog != null) {
            mShowLoadDialog.dismiss();
        }
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        new Handler().postDelayed(() -> {
            if (sSearchType.equals(SEARCH_TYPE_BOOKS) || sSearchType.equals(SEARCH_TYPE_AUTHORS)) {
                mSearchView.setVisibility(View.VISIBLE);
            } else {
                mSearchView.setVisibility(View.INVISIBLE);
            }
        }, 100);
        return super.onMenuOpened(featureId, menu);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.odps_menu, menu);

        // включу отображение иконок в раскрывающемся меню
        /*if (menu instanceof MenuBuilder) {
            MenuBuilder m = (MenuBuilder) menu;
            //noinspection RestrictedApi
            m.setOptionalIconsVisible(true);
        }*/

        // добавлю обработку поиска
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);

        mSearchView = (SearchView) searchMenuItem.getActionView();
        if (mSearchView != null) {
            mSearchView.setInputType(InputType.TYPE_CLASS_TEXT);
            mSearchView.setOnQueryTextListener(this);

            mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionSelect(int i) {
                    return true;
                }

                @Override
                public boolean onSuggestionClick(int i) {
                    String value = autocompleteStrings.get(i);
                    mSearchView.setQuery(value, false);
                    return true;
                }
            });

            mSearchAutoComplete = mSearchView.findViewById(R.id.search_src_text);
            mSearchAutoComplete.setTextColor(Color.WHITE);
            mSearchAutoComplete.setDropDownBackgroundResource(R.color.background_color);
            mSearchAutoComplete.setThreshold(0);
            mSearchAutoComplete.setDropDownHeight(WRAP_CONTENT);
            mSearchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, autocompleteStrings);
            mSearchAutoComplete.setAdapter(mSearchAdapter);
        }

        MenuItem myItem = menu.findItem(R.id.menuUseDarkMode);
        myItem.setChecked(mViewModel.getNightModeEnabled());


        // обработаю переключатель быстрой загрузки
        myItem = menu.findItem(R.id.discardFavoriteType);
        myItem.setEnabled(App.getInstance().getFavoriteMime() != null);

        // переключатель превью обложек
        myItem = menu.findItem(R.id.showPreviews);
        myItem.setChecked(App.getInstance().isPreviews());

        // обработаю переключатель скрытия прочтённых книг
        MenuItem hideReadSwitcher = menu.findItem(R.id.hideReadSwitcher);
        hideReadSwitcher.setChecked(App.getInstance().isHideRead());

        myItem = menu.findItem(R.id.hideDigests);
        myItem.setChecked(MyPreferences.getInstance().isDigestsHide());

        myItem = menu.findItem(R.id.hideDownloadedSwitcher);
        myItem.setChecked(MyPreferences.getInstance().isDownloadedHide());

        myItem = menu.findItem(R.id.menuCreateAuthorDir);
        myItem.setChecked(MyPreferences.getInstance().isCreateAuthorsDir());

        myItem = menu.findItem(R.id.menuCreateSequenceDir);
        myItem.setChecked(MyPreferences.getInstance().isCreateSequencesDir());

        myItem = menu.findItem(R.id.useFilter);
        myItem.setChecked(MyPreferences.getInstance().isUseFilter());
        return true;
    }

    private void selectSorting() {
        if (sSearchType != null) {
            String[] sortingOptions;
            // в зависимости от выбранного режима поиска покажу вырианты сортировки
            switch (sSearchType) {
                case SEARCH_TYPE_BOOKS:
                    sortingOptions = bookSortOptions;
                    break;
                case SEARCH_TYPE_AUTHORS:
                    sortingOptions = authorSortOptions;
                    break;
                default:
                    sortingOptions = otherSortOptions;
            }

            AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.MyDialogStyle);
            dialog.setTitle("Выберите тип сортировки")
                    .setItems(sortingOptions, (dialog1, which) -> sortList(which));
            // покажу список типов сортировки
            if (!OPDSActivity.this.isFinishing()) {
                dialog.show();
            }
        }
    }

    private void sortList(int which) {
        Log.d("surprise", "OPDSActivity sortList 1064: sort " + App.sSearchType);
        switch (App.sSearchType) {
            case SEARCH_BOOKS:
                App.getInstance().mBookSortOption = which;
                // если подключен книжный адаптер- проведу сортировку
                if (mResultsRecycler.getAdapter() instanceof FoundedBooksAdapter) {
                    ((FoundedBooksAdapter) mResultsRecycler.getAdapter()).sort();
                }
                break;
            case SEARCH_AUTHORS:
                App.getInstance().mAuthorSortOptions = which;
                Log.d("surprise", "OPDSActivity sortList 1075: sort authors");
                if (mResultsRecycler.getAdapter() instanceof FoundedAuthorsAdapter) {
                    ((FoundedAuthorsAdapter) mResultsRecycler.getAdapter()).sort();
                }
                break;
            case SEARCH_SEQUENCE:
                App.getInstance().mOtherSortOptions = which;
                if (mResultsRecycler.getAdapter() instanceof FoundedSequencesAdapter) {
                    ((FoundedSequencesAdapter) mResultsRecycler.getAdapter()).sort();
                }
                break;
            case SEARCH_GENRE:
                App.getInstance().mOtherSortOptions = which;
                if (mResultsRecycler.getAdapter() instanceof FoundedGenresAdapter) {
                    ((FoundedGenresAdapter) mResultsRecycler.getAdapter()).sort();
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new) {
            showNewDialog();
            return true;
        }
        if (id == R.id.action_bookmark) {
            addBookmark();
            return true;
        }
        if (id == R.id.menuUseDarkMode) {
            mViewModel.switchNightMode();
            new Handler().postDelayed(new ResetApp(), 100);
            return true;
        }
        if (id == R.id.hideReadSwitcher) {
            App.getInstance().switchHideRead();
            // скрою книги, если они есть
            if (App.getInstance().isHideRead()) {
                Toast.makeText(OPDSActivity.this, "Скрываю прочитанные книги", Toast.LENGTH_SHORT).show();
                hideReadedBooks();
            } else {
                Toast.makeText(OPDSActivity.this, "Отображаю прочитанные книги", Toast.LENGTH_SHORT).show();
                showReadedBooks();
            }
            invalidateMenu();
            return true;
        }
        if (id == R.id.action_sort_by) {
            selectSorting();
            return true;
        }
        if (id == R.id.clearSearchHistory) {
            clearHistory();
            return true;
        }
        if (id == R.id.discardFavoriteType) {
            App.getInstance().discardFavoriteType();
            Toast.makeText(this, "Выбранный тип загрузок сброшен", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.showPreviews) {
            App.getInstance().switchShowPreviews();
            invalidateMenu();
            return true;
        }
        if (id == R.id.switchLayout) {
            App.getInstance().switchLayout();
            recreate();
        }
        if (id == R.id.hideDigests) {
            MyPreferences.getInstance().switchDigestsHide();
            invalidateMenu();
            if (MyPreferences.getInstance().isDigestsHide()) {
                Toast.makeText(OPDSActivity.this, "Скрываю книги, у которых больше 3 авторов", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (id == R.id.menuCreateAuthorDir) {
            MyPreferences.getInstance().setCreateAuthorsDir(!item.isChecked());
            invalidateMenu();
            if (MyPreferences.getInstance().isCreateAuthorsDir()) {
                Toast.makeText(OPDSActivity.this, "Создаю папки для отдельных авторов", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(OPDSActivity.this, "Не создаю папки для отдельных авторов", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (id == R.id.menuCreateSequenceDir) {
            MyPreferences.getInstance().setCreateSequencesDir(!item.isChecked());
            invalidateMenu();
            if (MyPreferences.getInstance().isCreateSequencesDir()) {
                Toast.makeText(OPDSActivity.this, "Создаю папки для отдельных серий", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(OPDSActivity.this, "Не создаю папки для отдельных серий", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (id == R.id.hideDownloadedSwitcher) {
            MyPreferences.getInstance().switchDownloadedHide();
            invalidateMenu();
            if (MyPreferences.getInstance().isDownloadedHide()) {
                Toast.makeText(OPDSActivity.this, "Скрываю ранее скачанные книги", Toast.LENGTH_SHORT).show();
                hideDownloadedBooks();
            } else {
                Toast.makeText(OPDSActivity.this, "Показываю ранее скачанные книги", Toast.LENGTH_SHORT).show();
                showDownloadedBooks();
            }
            return true;
        }
        if (id == R.id.useFilter) {
            MyPreferences.getInstance().setUseFilter(!item.isChecked());
            invalidateMenu();
            if (MyPreferences.getInstance().isUseFilter()) {
                startActivity(new Intent(this, BlacklistActivity.class));
            } else {
                Toast.makeText(OPDSActivity.this, "Фильтрация результатов не применяется", Toast.LENGTH_SHORT).show();
                showDownloadedBooks();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addBookmark() {
        // покажу диалоговое окно с предложением подтвердить название закладки
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
        View view = getLayoutInflater().inflate(R.layout.dialog_bookmark_name, null);
        EditText input = view.findViewById(R.id.bookmarkNameInput);
        input.setText(sBookmarkName);
        dialogBuilder.setTitle("Название закладки")
                .setView(view)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String bookmarkText = input.getText().toString();
                    String link = History.getInstance().showLastPage();
                    if (!bookmarkText.isEmpty() && link != null) {
                        // сохраню закладку
                        Bookmark bookmark = new Bookmark();
                        bookmark.link = link;
                        Log.d("surprise", "onClick: add link " + link);
                        bookmark.name = bookmarkText;
                        App.getInstance().mDatabase.bookmarksDao().insert(bookmark);
                        Toast.makeText(OPDSActivity.this, "Закладка добавлена", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(OPDSActivity.this, "Нужно заполнить имя закладки и произвести поиск", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void showReadedBooks() {
        if (mResultsRecycler.getAdapter() instanceof FoundedBooksAdapter) {
            ((FoundedBooksAdapter) mResultsRecycler.getAdapter()).showReaded(sBooksForDownload);
        }
    }

    private void showDownloadedBooks() {
        if (mResultsRecycler.getAdapter() instanceof FoundedBooksAdapter) {
            ((FoundedBooksAdapter) mResultsRecycler.getAdapter()).showDownloaded(sBooksForDownload);
        }
    }

    private void hideReadedBooks() {
        if (mResultsRecycler.getAdapter() instanceof FoundedBooksAdapter) {
            ((FoundedBooksAdapter) mResultsRecycler.getAdapter()).hideReaded();
        }
    }

    private void hideDownloadedBooks() {
        if (mResultsRecycler.getAdapter() instanceof FoundedBooksAdapter) {
            ((FoundedBooksAdapter) mResultsRecycler.getAdapter()).hideDownloaded();
        }
    }


    private void clearHistory() {
        mViewModel.clearHistory();
        autocompleteStrings = new ArrayList<>();
        mSearchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, autocompleteStrings);
        mSearchAutoComplete.setAdapter(mSearchAdapter);
        Toast.makeText(this, "Автозаполнение сброшено", Toast.LENGTH_SHORT).show();
    }

    private void downloadAllBooks() {
        // добавлю книги в список для загрузки
        // если выбран тип загрузки книг и они существуют- предлагаю выбрать тип загрузки
        LiveData<WorkInfo> status = mViewModel.downloadAll(false);
        observeBookScheduleAdd(status);
    }

    private void selectBookTypeDialog() {
        LayoutInflater inflate = getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflate.inflate(R.layout.confirm_book_type_select, null);
        SwitchCompat checker = view.findViewById(R.id.save_only_selected);
        checker.setChecked(App.getInstance().isSaveOnlySelected());
        checker = view.findViewById(R.id.reDownload);
        checker.setChecked(App.getInstance().isReDownload());

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Выберите формат скачивания")
                .setItems(MimeTypes.MIMES_LIST, (dialogInterface, i) -> {
                    Dialog dialog = (Dialog) dialogInterface;
                    SwitchCompat switcher = dialog.findViewById(R.id.save_type_selection);
                    if (switcher.isChecked()) {
                        // запомню выбор формата
                        Toast.makeText(OPDSActivity.this, "Предпочтительный формат для скачивания сохранён(" + MimeTypes.getFullMime(MimeTypes.MIMES_LIST[i]) + "). Вы можете сбросить его в настройки +> разное.", Toast.LENGTH_LONG).show();
                        App.getInstance().saveFavoriteMime(MimeTypes.getFullMime(MimeTypes.MIMES_LIST[i]));
                    }
                    switcher = dialog.findViewById(R.id.save_only_selected);
                    App.getInstance().setSaveOnlySelected(switcher.isChecked());
                    switcher = dialog.findViewById(R.id.reDownload);
                    App.getInstance().setReDownload(switcher.isChecked());
                    invalidateMenu();
                    // оповещу о завершении выбора
                    App.getInstance().mSelectedFormat = MimeTypes.getFullMime(MimeTypes.MIMES_LIST[i]);
                    App.getInstance().mTypeSelected.postValue(true);
                })
                .setView(view);
        mSelectBookTypeDialog = dialogBuilder.create();
        // покажу диалог с выбором предпочтительного формата
        if (!OPDSActivity.this.isFinishing()) {
            mSelectBookTypeDialog.show();
        }
    }

    private void showMultiplyDownloadDialog() {
        if (mMultiplyDownloadDialog == null) {
            if (MyPreferences.getInstance().isEInk()) {
                mMultiplyDownloadDialog = new AlertDialog.Builder(this, R.style.MyDialogStyle)
                        .setTitle(getString(R.string.download_books_title))
                        .setMessage(getString(R.string.download_books_msg))
                        .setPositiveButton("Отменить", (dialog, which) -> {
                            if (App.getInstance().mProcess != null) {
                                WorkManager.getInstance(OPDSActivity.this).cancelWorkById(App.getInstance().mProcess.getId());
                            }
                            if (mMultiplyDownloadDialog != null) {
                                mMultiplyDownloadDialog.dismiss();
                            }
                            // отменю операцию
                            Toast.makeText(OPDSActivity.this, "Загрузка книг отменена", Toast.LENGTH_LONG).show();
                        })
                        .create();
            } else {
                mMultiplyDownloadDialog = new GifDialog.Builder(this)
                        .setTitle(getString(R.string.download_books_title))
                        .setMessage(getString(R.string.download_books_msg))
                        .setGifResource(R.drawable.loading)   //Pass your Gif here
                        .isCancellable(false)
                        .setPositiveBtnText("Отменить")
                        .OnPositiveClicked(() -> {
                            if (App.getInstance().mProcess != null) {
                                Log.d("surprise", "OPDSActivity OnClick kill process");
                                WorkManager.getInstance(OPDSActivity.this).cancelWorkById(App.getInstance().mProcess.getId());
                            }
                            if (mMultiplyDownloadDialog != null) {
                                mMultiplyDownloadDialog.dismiss();
                            }
                            // отменю операцию
                            Toast.makeText(OPDSActivity.this, "Загрузка книг отменена", Toast.LENGTH_LONG).show();

                        })
                        .build();
            }
        }
        //mMultiplyDownloadDialog.setMessage("Считаю количество книг для скачивания");
        if (!OPDSActivity.this.isFinishing()) {
            mMultiplyDownloadDialog.show();
        }
    }

    private void showNewDialog() {
        if (!OPDSActivity.this.isFinishing()) {
            String[] newCategory = new String[]{"Все новинки", "Новые книги по жанрам", "Новые книги по авторам", "Новые книги по сериям"};
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
            dialogBuilder.setTitle(R.string.showNewDialogTitle)
                    .setItems(newCategory, (dialog, which) -> {
                        // сброшу ранее загруженное
                        App.getInstance().mResultsEscalate = false;
                        switch (which) {
                            case 0:
                                App.sSearchType = SEARCH_BOOKS;
                                doSearch(URLHelper.getBaseOPDSUrl() + "/opds/new/0/new", false);
                                break;
                            case 1:
                                doSearch(URLHelper.getBaseOPDSUrl() + "/opds/newgenres", false);
                                break;
                            case 2:
                                doSearch(URLHelper.getBaseOPDSUrl() + "/opds/newauthors", false);
                                break;
                            case 3:
                                doSearch(URLHelper.getBaseOPDSUrl() + "/opds/newsequences", false);
                                break;
                        }
                    })
                    .show();
        }
    }


    @Override
    public boolean onQueryTextSubmit(String s) {
        if (!TextUtils.isEmpty(s.trim())) {
            // ищу введённое значение
            try {
                scrollToTop();
                makeSearch(s);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void makeSearch(String s) throws UnsupportedEncodingException {
        addValueToAutocompleteList(s);
        changeTitle("Поиск " + s);
        if (sSearchType.equals(SEARCH_TYPE_BOOKS)) {
            sBookmarkName = "Книга: " + s;
        } else if (sSearchType.equals(SEARCH_TYPE_AUTHORS)) {
            sBookmarkName = "Автор: " + s;
        } else {
            sBookmarkName = s;
        }
        String searchString = URLEncoder.encode(s, "utf-8").replace("+", "%20");
        doSearch(URLHelper.getSearchRequest(sSearchType, searchString), false);
    }

    private void addValueToAutocompleteList(String s) {
        // занесу значение в список автозаполнения
        if (XMLHandler.putSearchValue(s)) {
            // обновлю список поиска
            autocompleteStrings = mViewModel.getSearchAutocomplete();
            mSearchAdapter.clear();
            mSearchAdapter.addAll(autocompleteStrings);
            mSearchAdapter.notifyDataSetChanged();
        }
    }

    private void doSearch(String s, boolean searchOnBackPressed) {
        mFab.setVisibility(View.GONE);
        if (s != null && !s.isEmpty()) {
            if (mSearchView != null) {
                mSearchView.onActionViewCollapsed();
            }
            // сохраню последнюю загруженную страницу
            MyPreferences.getInstance().saveLastLoadedPage(s);
            // очищу историю поиска и положу туда начальное значение
            History.getInstance().addToHistory(s);
            showLoadWaitingDialog();
            if (!searchOnBackPressed) {
                // сохраню порядковый номер элемента по которому был клик, если он был
                mViewModel.saveClickedIndex(sClickedItemIndex);
            }
            sClickedItemIndex = -1;
            observeSearchStatus(mViewModel.request(s));
            // добавлю значение в историю
        }
    }

    private void observeSearchStatus(UUID requestId) {
        LiveData<WorkInfo> workInfo = WorkManager.getInstance(this).getWorkInfoByIdLiveData(requestId);
        workInfo.observe(this, workInfo1 -> {
            if (workInfo1 != null) {
                if (workInfo1.getState().isFinished()) {
                    hideWaitingDialog();
                }
            }
        });
    }

    private void showLoadWaitingDialog() {
        if (mShowLoadDialog == null) {
            if (MyPreferences.getInstance().isEInk()) {
                View view = getLayoutInflater().inflate(R.layout.dialog_waiting_e_ink, null, false);
                mShowLoadDialog = new AlertDialog.Builder(this, R.style.MyDialogStyle)
                        .setTitle(getString(R.string.download_books_title))
                        .setView(view)
                        .setPositiveButton("Отменить", (dialog, which) -> {
                            // отменю операцию поиска
                            WorkManager.getInstance(OPDSActivity.this).cancelUniqueWork(SearchWorker.WORK_TAG);
                            hideWaitingDialog();
                            Toast.makeText(OPDSActivity.this, "Загрузка отменена", Toast.LENGTH_LONG).show();
                        })
                        .create();
            } else {
                mShowLoadDialog = new GifDialog.Builder(this)
                        .setTitle(getString(R.string.load_waiting_title))
                        .setMessage(getString(R.string.load_waiting_message))
                        .setGifResource(R.drawable.loading)   //Pass your Gif here
                        .isCancellable(false)
                        .setPositiveBtnText("Отменить")
                        .OnPositiveClicked(() -> {
                            // отменю операцию поиска
                            WorkManager.getInstance(OPDSActivity.this).cancelUniqueWork(SearchWorker.WORK_TAG);
                            hideWaitingDialog();
                            Toast.makeText(OPDSActivity.this, "Загрузка отменена", Toast.LENGTH_LONG).show();
                        })
                        .build();
            }
        }
        if (!OPDSActivity.this.isFinishing()) {
            mShowLoadDialog.show();
        }

    }


    private void showAuthorViewSelect() {
        // получу идентификатор автора
        if (mSelectAuthorViewDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
            dialogBuilder.setTitle(R.string.select_author_view_message)
                    .setItems(mAuthorViewTypes, (dialog, which) -> loadAuthor(which, mSelectedAuthor.uri, mSelectedAuthor.name))
            ;
            mSelectAuthorViewDialog = dialogBuilder.create();
        }
        if (!OPDSActivity.this.isFinishing()) {
            mSelectAuthorViewDialog.show();
        }
    }


    private void showAuthorsSelect(final ArrayList<Author> authors) {
        if (mSelectAuthorsDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.select_authors_choose_message);
            mSelectAuthorsDialog = dialogBuilder;
        }
        // получу сисок имён авторов
        Iterator<Author> iterator = authors.iterator();
        ArrayList<String> authorsList = new ArrayList<>();
        while (iterator.hasNext()) {
            Author a = iterator.next();
            authorsList.add(a.name);

        }
        // покажу список выбора автора
        mSelectAuthorsDialog.setItems(authorsList.toArray(new String[0]), (dialogInterface, i) -> {
            mSelectedAuthor = authors.get(i);
            showAuthorViewSelect();
        });
        if (!OPDSActivity.this.isFinishing()) {
            mSelectAuthorsDialog.show();
        }
    }

    private void loadAuthor(int which, String authorId, String name) {
        String url = null;
        switch (which) {
            case 0:
                url = "/opds/authorsequences/" + authorId;
                sBookmarkName = "Автор " + name + " по сериям";
                break;
            case 1:
                url = "/opds/author/" + authorId + "/authorsequenceless";
                sBookmarkName = "Автор " + name + " вне серий";
                break;
            case 2:
                url = "/opds/author/" + authorId + "/alphabet";
                sBookmarkName = "Автор " + name + " по алфавиту";
                break;
            case 3:
                url = "/opds/author/" + authorId + "/time";
                sBookmarkName = "Автор " + name + " по времени";
                break;
        }
        if (url != null) {
            scrollToTop();
            doSearch(URLHelper.getBaseOPDSUrl() + url, false);
        }
    }

    @Override
    public boolean onQueryTextChange(String s) {
        return false;
    }

    public class TorConnectErrorReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // покажу диалоговое окно с оповещением, что TOR остановлен и кнопкой повторного запуска
            String errorDetails = intent.getStringExtra(TorWebClient.ERROR_DETAILS);
            showTorRestartDialog(errorDetails);
        }
    }

    private void showTorRestartDialog(String errorDetails) {
        if (errorDetails == null) {
            errorDetails = "";
        }
        hideWaitingDialog();
        if (mTorRestartDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
            dialogBuilder.setTitle(R.string.tor_is_stopped)
                    .setMessage(getString(R.string.tor_restart_dialog_message) + errorDetails)
                    .setPositiveButton(R.string.restart_tor_message, (dialog, which) -> {
                        App.getInstance().startTor();
                        dialog.dismiss();
                        // вернусь в основное активити и подожду перезапуска
                        Intent intent = new Intent(OPDSActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    })
                    .setNegativeButton("Ok", null)
                    .setCancelable(true);
            mTorRestartDialog = dialogBuilder.create();
        }
        if (!OPDSActivity.this.isFinishing()) {
            mTorRestartDialog.show();
        }
    }


    private void handleLoading() {
        // если приложение только запущено
        if (sFirstLoad) {
            sFirstLoad = false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mCoverPreviewDialog != null && mCoverPreviewDialog.isShowing()) {
                mBookTypeDialog.dismiss();
                return true;
            }
            // если доступен возврат назад- возвращаюсь, если нет- закрываю приложение
            if (History.getInstance().isEmpty()) {
                String lastPage = History.getInstance().getLastPage();
                if (lastPage != null) {
                    // получу значение элемента, по которому кликнули в предыдущем поиске
                    sElementForSelectionIndex = mViewModel.getLastClickedElement();
                    Log.d("surprise", "OPDSActivity onKeyDown 1637: set last clicked element by " + sElementForSelectionIndex);
                    doSearch(lastPage, true);
                    return true;
                }
            }
            if (mConfirmExit != 0) {
                if (mConfirmExit > System.currentTimeMillis() - 3000) {
                    // выйду из приложения
                    Log.d("surprise", "OPDSActivity onKeyDown exit");
                    // this.finishAffinity();
                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startMain);
                } else {
                    Toast.makeText(this, "Нечего загружать. Нажмите ещё раз для выхода", Toast.LENGTH_SHORT).show();
                    mConfirmExit = System.currentTimeMillis();
                }
            } else {
                Toast.makeText(this, "Нечего загружать. Нажмите ещё раз для выхода", Toast.LENGTH_SHORT).show();
                mConfirmExit = System.currentTimeMillis();
            }
            return true;

        }
        return super.onKeyDown(keyCode, event);
    }


    private void showDownloadsDialog(final ArrayList<DownloadLink> downloadLinks) {
        LayoutInflater inflate = getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflate.inflate(R.layout.confirm_book_type_select, null);
        SwitchCompat checker = view.findViewById(R.id.save_only_selected);
        checker.setChecked(App.getInstance().isSaveOnlySelected());
        checker = view.findViewById(R.id.reDownload);
        checker.setChecked(App.getInstance().isReDownload());

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.MyDialogStyle);
        dialogBuilder.setTitle(R.string.downloads_dialog_header);
        // получу список типов данных
        int linksLength = downloadLinks.size();
        final String[] linksArray = new String[linksLength];
        int counter = 0;
        String mime;
        while (counter < linksLength) {
            mime = downloadLinks.get(counter).mime;
            linksArray[counter] = MimeTypes.getMime(mime);
            counter++;
        }
        dialogBuilder.setItems(linksArray, (dialogInterface, i) -> {
            // проверю, выбрано ли сохранение формата загрузки
            Dialog dialog = (Dialog) dialogInterface;
            SwitchCompat switcher = dialog.findViewById(R.id.save_type_selection);
            if (switcher.isChecked()) {
                // запомню выбор формата
                Toast.makeText(OPDSActivity.this, "Предпочтительный формат для скачивания сохранён (" + MimeTypes.getFullMime(linksArray[i]) + "). Вы можете сбросить его в настройки +> разное.", Toast.LENGTH_LONG).show();
                App.getInstance().saveFavoriteMime(MimeTypes.getFullMime(linksArray[i]));
            }
            switcher = dialog.findViewById(R.id.save_only_selected);
            App.getInstance().setSaveOnlySelected(switcher.isChecked());
            switcher = dialog.findViewById(R.id.reDownload);
            App.getInstance().setReDownload(switcher.isChecked());
            invalidateMenu();
            // получу сокращённый MIME
            String shortMime = linksArray[i];
            String longMime = MimeTypes.getFullMime(shortMime);
            int counter1 = 0;
            int linksLength1 = downloadLinks.size();
            DownloadLink item;
            while (counter1 < linksLength1) {
                item = downloadLinks.get(counter1);
                if (item.mime.equals(longMime)) {
                    mViewModel.addToDownloadQueue(item);
                    break;
                }
                counter1++;
            }
            Toast.makeText(OPDSActivity.this, "Книга добавлена в очередь загрузок", Toast.LENGTH_SHORT).show();
        })
                .setView(view);
        AlertDialog dialog = dialogBuilder.create();
        if (!OPDSActivity.this.isFinishing()) {
            dialog.show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == START_TOR) {
            // перезагружу страницу
            String lastUrl = History.getInstance().showLastPage();
            if (lastUrl != null && !lastUrl.isEmpty()) {
                showLoadWaitingDialog();
                mViewModel.request(lastUrl);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void scrollToTop() {
        if (mResultsRecycler != null) {
            mResultsRecycler.scrollToPosition(0);
        }
        if (mScrollView != null) {
            mScrollView.smoothScrollTo(0, 0);
        }
    }

    private void invalidateMenu() {
        invalidateOptionsMenu();
        new Handler().postDelayed(() -> {
            if (sSearchType.equals(SEARCH_TYPE_BOOKS) || sSearchType.equals(SEARCH_TYPE_AUTHORS)) {
                Log.d("surprise", "OPDSActivity onMenuOpened 1036: showing search");
                mSearchView.setVisibility(View.VISIBLE);
            } else {
                Log.d("surprise", "OPDSActivity onMenuOpened 1040: hide search");
                mSearchView.setVisibility(View.INVISIBLE);
            }
        }, 100);
    }

}
