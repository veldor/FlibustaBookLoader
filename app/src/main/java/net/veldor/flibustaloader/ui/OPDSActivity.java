package net.veldor.flibustaloader.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.widget.NestedScrollView;
import androidx.documentfile.provider.DocumentFile;
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
import net.veldor.flibustaloader.SubscribeActivity;
import net.veldor.flibustaloader.SubscriptionsActivity;
import net.veldor.flibustaloader.adapters.FoundedAuthorsAdapter;
import net.veldor.flibustaloader.adapters.FoundedBooksAdapter;
import net.veldor.flibustaloader.adapters.FoundedGenresAdapter;
import net.veldor.flibustaloader.adapters.FoundedSequencesAdapter;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.dialogs.GifDialog;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.utils.Grammar;
import net.veldor.flibustaloader.utils.History;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.utils.MyPreferences;
import net.veldor.flibustaloader.utils.TransportUtils;
import net.veldor.flibustaloader.utils.URLHelper;
import net.veldor.flibustaloader.utils.XMLHandler;
import net.veldor.flibustaloader.view_models.MainViewModel;
import net.veldor.flibustaloader.workers.DownloadBooksWorker;
import net.veldor.flibustaloader.workers.SearchWorker;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import lib.folderpicker.FolderPicker;

import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;
import static net.veldor.flibustaloader.ui.MainActivity.START_TOR;

public class OPDSActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private static final String[] bookSortOptions = new String[]{"По названию книги", "По размеру", "По количеству скачиваний", "По серии", "По жанру", "По автору"};
    private static final String[] authorSortOptions = new String[]{"По имени автора от А", "По имени автора от Я", "По количеству книг от большего", "По количеству книг от меньшего"};
    private static final String[] otherSortOptions = new String[]{"От А", "От Я"};
    private static final int READ_REQUEST_CODE = 5;
    private static final int REQUEST_CODE = 7;
    private static final int BACKUP_FILE_REQUEST_CODE = 8;
    public static final String SEARCH_TYPE_BOOKS = "search books";
    public static final String SEARCH_TYPE_AUTHORS = "search authors";
    public static final String SEARCH_TYPE_SEQUENCES = "search sequences";
    public static final String SEARCH_TYPE_GENRE = "search genre";

    // список результатов поиска жанров
    public static final MutableLiveData<ArrayList<Genre>> sLiveGenresFound = new MutableLiveData<>();

    // список результатов поиска серий
    public static final MutableLiveData<ArrayList<FoundedSequence>> sLiveSequencesFound = new MutableLiveData<>();

    // список результатов поиска авторов
    public static MutableLiveData<ArrayList<Author>> sLiveAuthorsFound = new MutableLiveData<>();

    public static MutableLiveData<ArrayList<FoundedBook>> sLiveBooksFound = new MutableLiveData<>();

    // ссылка для поиска
    public static final MutableLiveData<String> sLiveSearchLink = new MutableLiveData<>();
    public static final MutableLiveData<Boolean> sNothingFound = new MutableLiveData<>();


    private AlertDialog mTorRestartDialog;
    private MainViewModel mMyViewModel;
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
    private static String sSearchType;
    public static String sNextPage;
    private boolean mAddToLoaded;
    private static boolean sFirstLoad = true;
    private NestedScrollView mScrollView;
    private RadioButton mSearchBooksButton;
    private RadioButton mSearchAuthorsButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (MyPreferences.getInstance().isHardwareAcceleration()) {
            // проверю аппаратное ускорение
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        if (App.getInstance().getNightMode()) {
            setTheme(R.style.NightTheme);
        }

        setContentView(R.layout.activity_odps);

        // добавлю viewModel
        mMyViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupUI();

        checkSearchType();

        // проверю очередь загрузки
        checkDownloadQueue();

        // определю кнопки прыжков по результатам
        mLoadMoreBtn = findViewById(R.id.load_more_button);

        // кнопка перехода вперёд
        mForwardBtn = findViewById(R.id.forward_btn);
        mBackwardBtn = findViewById(R.id.backward_btn);

        mBackwardBtn.setOnClickListener(v -> {
            if (History.getInstance().isEmpty()) {
                String lastPage = History.getInstance().getLastPage();
                if (lastPage != null) {
                    doSearch(lastPage);
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
                doSearch(sNextPage);
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
        autocompleteStrings = mMyViewModel.getSearchAutocomplete();

        // при нажатии кнопки назад- убираем последний элемент истории и переходим на предпоследний
        mLoadMoreBtn.setOnClickListener(view -> {
            // загружаю следующую страницу
            mAddToLoaded = true;
            doSearch(sNextPage);
        });
        // добавлю отслеживание изменения ваианта поиска

        mSearchBooksButton = findViewById(R.id.searchBook);
        if (mSearchBooksButton != null) {
            mSearchBooksButton.setOnClickListener(v -> {
                Toast.makeText(OPDSActivity.this, "Ищем книги", Toast.LENGTH_SHORT).show();
                sSearchType = SEARCH_TYPE_BOOKS;
                if(mSearchView != null){
                    mSearchView.setVisibility(View.VISIBLE);
                    mSearchView.setIconified(false);
                    mSearchView.requestFocusFromTouch();
                }
            });
        }

        mSearchAuthorsButton = findViewById(R.id.searchAuthor);
        mSearchAuthorsButton.setOnClickListener(v -> {
            Toast.makeText(OPDSActivity.this, "Ищем авторов", Toast.LENGTH_SHORT).show();
            sSearchType = SEARCH_TYPE_AUTHORS;
            if(mSearchView != null){
                mSearchView.setVisibility(View.VISIBLE);
                mSearchView.setIconified(false);
                mSearchView.requestFocusFromTouch();
            }
        });

        RadioButton searchGenresButton = findViewById(R.id.searchGenre);
        searchGenresButton.setOnClickListener(v -> {
            Toast.makeText(OPDSActivity.this, "Ищу жанры", Toast.LENGTH_SHORT).show();
            sSearchType = SEARCH_TYPE_GENRE;
            if(mSearchView != null){
                mSearchView.setVisibility(View.GONE);
            }
            showLoadWaitingDialog();
            doSearch(URLHelper.getSearchRequest(SEARCH_TYPE_GENRE, "genres"));
            scrollToTop();
            mFab.setVisibility(View.GONE);
        });

        RadioButton searchSequencesButton = findViewById(R.id.searchSequence);
        searchSequencesButton.setOnClickListener(v -> {
            Toast.makeText(OPDSActivity.this, "Ищу серии", Toast.LENGTH_SHORT).show();
            sSearchType = SEARCH_TYPE_SEQUENCES;
            if(mSearchView != null){
                mSearchView.setVisibility(View.GONE);
            }
            showLoadWaitingDialog();
            doSearch(URLHelper.getSearchRequest(SEARCH_TYPE_SEQUENCES, "sequencesindex"));
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
            if (downloadLinks1 != null && downloadLinks1.size() > 0) {
                if (downloadLinks1.size() == 1) {
                    // добавлю книгу в очередь скачивания
                    mMyViewModel.addToDownloadQueue(downloadLinks1.get(0));
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
                mSelectedAuthor = author;
                showAuthorViewSelect();
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
                doSearch(URLHelper.getBaseOPDSUrl() + foundedSequence.link);
            }
        });


        // добавлю обсерверы
        addObservers();
        checkUpdates();
    }

    private void checkSearchType() {
        // получу выбранный тип поиска
        switch (mSearchRadioContainer.getCheckedRadioButtonId()) {
            case R.id.searchBook:
                sSearchType = SEARCH_TYPE_BOOKS;
                break;
            case R.id.searchAuthor:
                sSearchType = SEARCH_TYPE_AUTHORS;
                break;
            case R.id.searchSequence:
                sSearchType = SEARCH_TYPE_SEQUENCES;
                break;
            case R.id.searchGenre:
                sSearchType = SEARCH_TYPE_GENRE;
                break;
        }
        Log.d("surprise", "OPDSActivity checkSearchType: search type is " + sSearchType);
    }

    private void checkDownloadQueue() {
        Boolean queue = mMyViewModel.checkDownloadQueue();
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
        if(!OPDSActivity.this.isFinishing()){
            new AlertDialog.Builder(OPDSActivity.this)
                    .setTitle(R.string.re_download_dialog_header_message)
                    .setMessage(R.string.re_download_dialog_body_message)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                        mMyViewModel.initiateMassDownload();
                        Toast.makeText(OPDSActivity.this, R.string.download_continued_message, Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton(R.string.no, null)
                    .create().show();
        }
    }

    private void setupUI() {

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

        Switch massLoadSwitcher = findViewById(R.id.showAllSwitcher);
        if (massLoadSwitcher != null) {
            massLoadSwitcher.setChecked(App.getInstance().isDownloadAll());
            massLoadSwitcher.setOnCheckedChangeListener((compoundButton, b) -> App.getInstance().switchDownloadAll());
        }
        mFab = findViewById(R.id.floatingMenu);
        mFab.setVisibility(View.GONE);

        View downloadAllButton = findViewById(R.id.fabDownloadAll);
        if (downloadAllButton != null) {
            downloadAllButton.setOnClickListener(view -> {
                Log.d("surprise", "OPDSActivity onClick initiate download all");
                String favoriteFormat = App.getInstance().getFavoriteMime();
                if (favoriteFormat != null) {
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
                if (favoriteFormat != null) {
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
                if (favoriteFormat != null) {
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
    }

    private void downloadUnloaded() {
        LiveData<WorkInfo> status = mMyViewModel.downloadAll(true);
        observeBookScheduleAdd(status);
    }

    private void showDownloadSelectedDialog() {
        // получу названия книг
        ArrayList<FoundedBook> books = sLiveBooksFound.getValue();
        if (books != null && books.size() > 0) {
            String[] variants = new String[books.size()];
            int counter = 0;
            for (FoundedBook fb : books) {
                variants[counter] = String.format(Locale.ENGLISH, "%s \n %s \n %s \n", fb.name, fb.format, (fb.translate.isEmpty() ? "" : fb.translate));
                counter++;
            }
            mDownloadSelectedDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.select_books_for_download_message)
                    .setMultiChoiceItems(variants, null, (dialogInterface, i, b) -> {
                    })
                    .setPositiveButton(R.string.download_selected_message, (dialogInterface, i) -> {
                        mDownloadSelectedDialog.getListView().getCheckedItemCount();
                        SparseBooleanArray ids = mDownloadSelectedDialog.getListView().getCheckedItemPositions();
                        if (ids.size() > 0) {
                            LiveData<WorkInfo> status = mMyViewModel.downloadSelected(ids);
                            observeBookScheduleAdd(status);
                        } else {
                            Toast.makeText(OPDSActivity.this, R.string.books_not_selected_message, Toast.LENGTH_LONG).show();
                        }
                    })
                    .create();
            if(!OPDSActivity.this.isFinishing()) {
                mDownloadSelectedDialog.show();
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
                mMyViewModel.initiateMassDownload();
                startActivity(new Intent(getBaseContext(), ActivityBookDownloadSchedule.class));
                status.removeObservers(OPDSActivity.this);
            }
        });
    }

    private void checkUpdates() {
        if (App.getInstance().isCheckUpdate()) {
            // проверю обновления
            final LiveData<Boolean> version = mMyViewModel.startCheckUpdate();
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
        updateSnackbar.setAction(getString(R.string.snackbar_update_action_message), v -> mMyViewModel.initializeUpdate());
        updateSnackbar.setActionTextColor(getResources().getColor(android.R.color.white));
        updateSnackbar.show();
    }

    private void addObservers() {
        // отслеживание отсутствия результатов
        sNothingFound.observe(this, result -> {
            if (result) {
                Toast.makeText(OPDSActivity.this, "Ничего не найдено", Toast.LENGTH_SHORT).show();
                RecyclerView.Adapter adapter = mResultsRecycler.getAdapter();
                if (adapter != null) {
                    if (adapter instanceof FoundedBooksAdapter) {
                        ((FoundedBooksAdapter) adapter).setContent(null, false);
                    } else if (adapter instanceof FoundedAuthorsAdapter) {
                        ((FoundedAuthorsAdapter) adapter).setContent(null);
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
                doSearch(URLHelper.getBaseOPDSUrl() + s);
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
                    RecyclerView.Adapter adapter = mResultsRecycler.getAdapter();
                    if (adapter instanceof FoundedGenresAdapter) {
                        ((FoundedGenresAdapter) adapter).setContent(genres);
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
                    RecyclerView.Adapter adapter = mResultsRecycler.getAdapter();
                    if (adapter instanceof FoundedSequencesAdapter) {
                        ((FoundedSequencesAdapter) adapter).setContent(sequences);
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
                    mSearchAuthorsButton.performClick();
                }
                App.sSearchType = SEARCH_AUTHORS;
            }
            Log.d("surprise", "OPDSActivity addObservers 630: found authors " + authors);
            hideLoadingButtons();
            // найденные авторы сброшены
            if (mResultsRecycler.getAdapter() != null) {
                RecyclerView.Adapter adapter = mResultsRecycler.getAdapter();
                if (adapter instanceof FoundedAuthorsAdapter) {
                    ((FoundedAuthorsAdapter) adapter).setContent(authors);
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
            Log.d("surprise", "OPDSActivity addObservers 656: books HERE " + books);
            if (books != null) {
                if (!mSearchBooksButton.isChecked()) {
                    mSearchBooksButton.performClick();
                }
            }
            App.sSearchType = SEARCH_BOOKS;
            // найденные книги сброшены
            if (mResultsRecycler.getAdapter() != null) {
                RecyclerView.Adapter adapter = mResultsRecycler.getAdapter();
                if (adapter instanceof FoundedBooksAdapter) {
                    ((FoundedBooksAdapter) adapter).setContent(books, mAddToLoaded);
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

/*        FoundedBooksListAdapter adapter = new FoundedBooksListAdapter();
        sLiveBooksFound.observe(this, list -> {
            adapter.submitList(list);
            mResultsRecycler.setAdapter(adapter);
        });*/


        // добавлю отслеживание показа информации о книге
        LiveData<FoundedBook> selectedBook = App.getInstance().mSelectedBook;
        selectedBook.observe(this, book -> {
            if (book != null) {
                Log.d("surprise", "OPDSActivity onChanged show book info");
                BookInfoDialog = new AlertDialog.Builder(OPDSActivity.this)
                        .setTitle(book.name)
                        .setMessage(Grammar.textFromHtml(book.bookInfo))
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                if(!OPDSActivity.this.isFinishing()) {
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
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(OPDSActivity.this);
                String[] items = new String[]{"Пометить книгу как прочитанную", "Показать страницу книги"};
                dialogBuilder.setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // отмечу книгу как прочитанную
                            mMyViewModel.setBookRead(book);
                            Toast.makeText(OPDSActivity.this, "Книга отмечена как прочитанная", Toast.LENGTH_LONG).show();
                            // оповещу адаптер об этом
                            RecyclerView.Adapter adapter = mResultsRecycler.getAdapter();
                            if (adapter instanceof FoundedBooksAdapter) {
                                ((FoundedBooksAdapter) adapter).setBookReaded(book);
                            }
                            break;
                        case 1:
                            showPage(book);
                    }
                });
                mBookTypeDialog = dialogBuilder.create();
                if(!OPDSActivity.this.isFinishing()) {
                    mBookTypeDialog.show();
                }
            }
        });

        LiveData<Author> authorNewBooks = App.getInstance().mAuthorNewBooks;
        authorNewBooks.observe(this, author -> {
            if (author != null)
                doSearch(App.BASE_URL + author.link);
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
                    dialogText.setText(s);
                }
            }
            if (s != null && !s.isEmpty() && mMultiplyDownloadDialog != null) {
                // изменю сообщение
                Window window = mMultiplyDownloadDialog.getWindow();
                if (window != null) {
                    TextView dialogText = window.findViewById(R.id.title);
                    dialogText.setText(s);
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
        if(!OPDSActivity.this.isFinishing()) {
            Log.d("surprise", "OPDSActivity showPreview: preview url is " + foundedBook.previewUrl);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
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
        String link = book.id.split(":")[2];
        intent.setData(Uri.parse(App.BASE_BOOK_URL + link));
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
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.select_sequences_choose_message);
            mSelectSequencesDialog = dialogBuilder;
        }
        // получу сисок серий
        Iterator<FoundedSequence> iterator = sequences.iterator();
        ArrayList<String> sequencesList = new ArrayList<>();
        for (; iterator.hasNext(); ) {
            FoundedSequence s = iterator.next();
            sequencesList.add(s.title);

        }
        // покажу список выбора серии
        mSelectSequencesDialog.setItems(sequencesList.toArray(new String[0]), (dialogInterface, i) -> App.getInstance().mSelectedSequence.postValue(sequences.get(i)));
        if(!OPDSActivity.this.isFinishing()) {
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
    }


    private void hideWaitingDialog() {
        if (mShowLoadDialog != null) {
            mShowLoadDialog.dismiss();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.odps_menu, menu);

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
                    mSearchView.setQuery(value, true);
                    return true;
                }
            });

            mSearchAutoComplete = mSearchView.findViewById(R.id.search_src_text);
            mSearchAutoComplete.setDropDownBackgroundResource(R.color.background_color);
            mSearchAutoComplete.setDropDownAnchor(R.id.action_search);
            mSearchAutoComplete.setThreshold(0);
            mSearchAutoComplete.setDropDownHeight(getResources().getDisplayMetrics().heightPixels / 2);

            mSearchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, autocompleteStrings);

            mSearchAutoComplete.setAdapter(mSearchAdapter);
        }

        MenuItem nightModeSwitcher = menu.findItem(R.id.menuUseDarkMode);
        nightModeSwitcher.setChecked(mMyViewModel.getNightModeEnabled());

        // обработаю переключатель проверки обновлений
        MenuItem checkUpdatesSwitcher = menu.findItem(R.id.setUpdateCheck);
        checkUpdatesSwitcher.setChecked(App.getInstance().isCheckUpdate());

        // обработаю переключатель скрытия прочтённых книг
        MenuItem hideReadSwitcher = menu.findItem(R.id.hideReadSwitcher);
        hideReadSwitcher.setChecked(App.getInstance().isHideRead());

        // обработаю переключатель быстрой загрузки
        MenuItem discardFavorite = menu.findItem(R.id.discardFavoriteType);
        discardFavorite.setEnabled(App.getInstance().getFavoriteMime() != null);
        // обработаю переключатель загрузки в приоритетном формате
        MenuItem myItem = menu.findItem(R.id.downloadOnlySelected);
        myItem.setChecked(App.getInstance().isSaveOnlySelected());

        // переключатель повторной загрузки
        myItem = menu.findItem(R.id.doNotReDownload);
        myItem.setChecked(App.getInstance().isReDownload());
        // переключатель превью обложек
        myItem = menu.findItem(R.id.showPreviews);
        myItem.setChecked(App.getInstance().isPreviews());
        // переключатель внешнего VPN
        myItem = menu.findItem(R.id.menuUseExternalVpn);
        myItem.setChecked(App.getInstance().isExternalVpn());

        myItem = menu.findItem(R.id.autoCheck);
        myItem.setChecked(MyPreferences.getInstance().isSubscriptionsAutoCheck());

        myItem = menu.findItem(R.id.hideDigests);
        myItem.setChecked(MyPreferences.getInstance().isDigestsHide());
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

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Выберите тип сортировки")
                    .setItems(sortingOptions, (dialog1, which) -> sortList(which));
            // покажу список типов сортировки
            if(!OPDSActivity.this.isFinishing()) {
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
                RecyclerView.Adapter adapter = mResultsRecycler.getAdapter();
                if (adapter instanceof FoundedBooksAdapter) {
                    ((FoundedBooksAdapter) adapter).sort();
                }
                break;
            case SEARCH_AUTHORS:
                App.getInstance().mAuthorSortOptions = which;
                Log.d("surprise", "OPDSActivity sortList 1075: sort authors");
                adapter = mResultsRecycler.getAdapter();
                if (adapter instanceof FoundedAuthorsAdapter) {
                    ((FoundedAuthorsAdapter) adapter).sort();
                }
                break;
            case SEARCH_SEQUENCE:
                App.getInstance().mOtherSortOptions = which;
                adapter = mResultsRecycler.getAdapter();
                if (adapter instanceof FoundedSequencesAdapter) {
                    ((FoundedSequencesAdapter) adapter).sort();
                }
                break;
            case SEARCH_GENRE:
                App.getInstance().mOtherSortOptions = which;
                adapter = mResultsRecycler.getAdapter();
                if (adapter instanceof FoundedGenresAdapter) {
                    ((FoundedGenresAdapter) adapter).sort();
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.buyCoffee:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=YUGUWUF99QYG4&source=url"));
                startActivity(intent);
                return true;
            case R.id.goToTest:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://t.me/flibusta_downloader_beta"));
                startActivity(intent);
            case R.id.showDownloadsSchedule:
                intent = new Intent(this, ActivityBookDownloadSchedule.class);
                startActivity(intent);
                return true;
            case R.id.setDownloadsFolder:
                changeDownloadsFolder();
                return true;
            case R.id.action_new:
                showNewDialog();
                return true;
            case R.id.switchToWebView:
                switchToWebView();
                return true;
            case R.id.clearSearchHistory:
                clearHistory();
                return true;
            case R.id.menuUseDarkMode:
                mMyViewModel.switchNightMode();
                new Handler().postDelayed(new ResetApp(), 100);
            case R.id.setUpdateCheck:
                App.getInstance().switchCheckUpdate();
                invalidateOptionsMenu();
                return true;
            case R.id.hideReadSwitcher:
                App.getInstance().switchHideRead();
                invalidateOptionsMenu();
                return true;
            case R.id.action_sort_by:
                selectSorting();
                return true;
            case R.id.subscribeForNew:
                subscribe();
                return true;
            case R.id.discardFavoriteType:
                App.getInstance().discardFavoriteType();
                Toast.makeText(this, "Выбранный тип загрузок сброшен", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.reserveSettings:
                Toast.makeText(this, "Начато резервирование настрек, после завершения вы получите уведомление.", Toast.LENGTH_LONG).show();
                mMyViewModel.reserveSettings();
                return true;
            case R.id.restoreSettings:
                restoreSettings();
                return true;
            case R.id.doNotReDownload:
                App.getInstance().setReDownload(!App.getInstance().isReDownload());
                invalidateOptionsMenu();
                return true;
            case R.id.downloadOnlySelected:
                App.getInstance().setSaveOnlySelected(!App.getInstance().isSaveOnlySelected());
                invalidateOptionsMenu();
                return true;
            case R.id.showPreviews:
                App.getInstance().switchShowPreviews();
                invalidateOptionsMenu();
                return true;
            case R.id.menuUseExternalVpn:
                handleUseExternalVpn();
                return true;
            case R.id.switchLayout:
                App.getInstance().switchLayout();
                recreate();
            case R.id.checkSubscribesNow:
                mMyViewModel.checkSubscribes();
                return true;
            case R.id.fullCheckSubscribes:
                mMyViewModel.fullCheckSubscribes();
                return true;
            case R.id.showSubscriptionsList:
                startActivity(new Intent(this, SubscriptionsActivity.class));
                return true;
            case R.id.autoCheck:
                mMyViewModel.switchSubscriptionsAutoCheck();
                invalidateOptionsMenu();
                return true;
            case R.id.hideDigests:
                MyPreferences.getInstance().switchDigestsHide();
                invalidateOptionsMenu();
                if (MyPreferences.getInstance().isDigestsHide()) {
                    Toast.makeText(OPDSActivity.this, "Скрываю книги, у которых больше 3 авторов", Toast.LENGTH_SHORT).show();
                }
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void handleUseExternalVpn() {
        // если использовался внешний VPN- просто отключу его использование
        if (App.getInstance().isExternalVpn()) {
            App.getInstance().switchExternalVpnUse();
            invalidateOptionsMenu();
        } else {
            // покажу диалог с объяснением последствий включения VPN
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder
                    .setTitle("Использование внешнего VPN")
                    .setMessage("Оповестить об использовании внешнего VPN. В этом случае внутренний клиент TOR будет отключен и траффик приложения не будет обрабатываться. В этом случае вся ответственность за получение контента ложится на внешний VPN. Если вы будете получать сообщения об ошибках загрузки- значит, он работает неправильно. Сделано для версий Android ниже 6.0, где могут быть проблемы с доступом, но может быть использовано по желанию на ваш страх и риск.")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                        App.getInstance().switchExternalVpnUse();
                        invalidateOptionsMenu();
                    });
            if(!OPDSActivity.this.isFinishing()) {
                dialogBuilder.create().show();
            }
        }
    }

    private void restoreSettings() {
        Toast.makeText(this, "Выберите сохранённый ранее файл с настройками.", Toast.LENGTH_LONG).show();
        // открою окно выбота файла для восстановления
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        if (TransportUtils.intentCanBeHandled(intent)) {
            startActivityForResult(intent, BACKUP_FILE_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Упс, не нашлось приложения, которое могло бы это сделать.", Toast.LENGTH_LONG).show();
        }
    }

    private void subscribe() {
        startActivity(new Intent(this, SubscribeActivity.class));
    }

    private void clearHistory() {
        mMyViewModel.clearHistory();
        autocompleteStrings = new ArrayList<>();
        mSearchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, autocompleteStrings);
        mSearchAutoComplete.setAdapter(mSearchAdapter);
        Toast.makeText(this, "Автозаполнение сброшено", Toast.LENGTH_SHORT).show();
    }

    private void switchToWebView() {
        // переключу отображение на WebView, запущу webview вид и завершу активность
        App.getInstance().setView(App.VIEW_WEB);
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void downloadAllBooks() {
        // если выбран тип загрузки книг и они существуют- предлагаю выбрать тип загрузки
        LiveData<WorkInfo> status = mMyViewModel.downloadAll(false);
        observeBookScheduleAdd(status);
    }

    private void selectBookTypeDialog() {
        LayoutInflater inflate = getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflate.inflate(R.layout.confirm_book_type_select, null);
        Switch checker = view.findViewById(R.id.save_only_selected);
        checker.setChecked(App.getInstance().isSaveOnlySelected());
        checker = view.findViewById(R.id.reDownload);
        checker.setChecked(App.getInstance().isReDownload());

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Выберите формат скачивания")
                .setItems(MimeTypes.MIMES_LIST, (dialogInterface, i) -> {
                    Dialog dialog = (Dialog) dialogInterface;
                    Switch switcher = dialog.findViewById(R.id.save_type_selection);
                    Log.d("surprise", "OPDSActivity onClick: switcher is " + switcher);
                    if (switcher.isChecked()) {
                        // запомню выбор формата
                        Toast.makeText(OPDSActivity.this, "Предпочтительный формат для скачивания сохранён(" + MimeTypes.getFullMime(MimeTypes.MIMES_LIST[i]) + "). Вы можете сбросить его в настройки +> разное.", Toast.LENGTH_LONG).show();
                        App.getInstance().saveFavoriteMime(MimeTypes.getFullMime(MimeTypes.MIMES_LIST[i]));
                    }
                    switcher = dialog.findViewById(R.id.save_only_selected);
                    App.getInstance().setSaveOnlySelected(switcher.isChecked());
                    switcher = dialog.findViewById(R.id.reDownload);
                    App.getInstance().setReDownload(switcher.isChecked());
                    invalidateOptionsMenu();
                    // оповещу о завершении выбора
                    App.getInstance().mSelectedFormat = MimeTypes.getFullMime(MimeTypes.MIMES_LIST[i]);
                    App.getInstance().mTypeSelected.postValue(true);
                })
                .setView(view);
        mSelectBookTypeDialog = dialogBuilder.create();
        // покажу диалог с выбором предпочтительного формата
        if(!OPDSActivity.this.isFinishing()) {
            mSelectBookTypeDialog.show();
        }
    }

    private void showMultiplyDownloadDialog() {
        if (mMultiplyDownloadDialog == null) {
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
        //mMultiplyDownloadDialog.setMessage("Считаю количество книг для скачивания");
        if(!OPDSActivity.this.isFinishing()) {
            mMultiplyDownloadDialog.show();
        }
    }

    private void showNewDialog() {
        if(!OPDSActivity.this.isFinishing()) {
            String[] newCategory = new String[]{"Все новинки", "Новые книги по жанрам", "Новые книги по авторам", "Новые книги по сериям"};
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.showNewDialogTitle)
                    .setItems(newCategory, (dialog, which) -> {
                        // сброшу ранее загруженное
                        App.getInstance().mResultsEscalate = false;
                        switch (which) {
                            case 0:
                                App.sSearchType = SEARCH_BOOKS;
                                doSearch(URLHelper.getBaseOPDSUrl() + "/opds/new/0/new");
                                break;
                            case 1:
                                doSearch(URLHelper.getBaseOPDSUrl() + "/opds/newgenres");
                                break;
                            case 2:
                                doSearch(URLHelper.getBaseOPDSUrl() + "/opds/newauthors");
                                break;
                            case 3:
                                doSearch(URLHelper.getBaseOPDSUrl() + "/opds/newsequences");
                                break;
                        }
                    })
                    .show();
        }
    }

    private void changeDownloadsFolder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_CODE);
        } else {
            Intent intent = new Intent(this, FolderPicker.class);
            startActivityForResult(intent, READ_REQUEST_CODE);
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
        String searchString = URLEncoder.encode(s, "utf-8").replace("+", "%20");
        doSearch(URLHelper.getSearchRequest(sSearchType, searchString));
    }

    private void addValueToAutocompleteList(String s) {
        // занесу значение в список автозаполнения
        if (XMLHandler.putSearchValue(s)) {
            // обновлю список поиска
            autocompleteStrings = mMyViewModel.getSearchAutocomplete();
            mSearchAdapter.clear();
            mSearchAdapter.addAll(autocompleteStrings);
            mSearchAdapter.notifyDataSetChanged();
        }
    }

    private void doSearch(String s) {
        mFab.setVisibility(View.GONE);
        if (s != null && !s.isEmpty()) {
            // сохраню последнюю загруженную страницу
            MyPreferences.getInstance().saveLastLoadedPage(s);
            // очищу историю поиска и положу туда начальное значение
            History.getInstance().addToHistory(s);
            showLoadWaitingDialog();
            observeSearchStatus(mMyViewModel.request(s));
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
        if(!OPDSActivity.this.isFinishing()) {
            mShowLoadDialog.show();
        }

    }


    private void showAuthorViewSelect() {
        // получу идентификатор автора
        if (mSelectAuthorViewDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.select_author_view_message)
                    .setItems(mAuthorViewTypes, (dialog, which) -> loadAuthor(which, mSelectedAuthor.uri))
            ;
            mSelectAuthorViewDialog = dialogBuilder.create();
        }
        if(!OPDSActivity.this.isFinishing()) {
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
        for (; iterator.hasNext(); ) {
            Author a = iterator.next();
            authorsList.add(a.name);

        }
        // покажу список выбора автора
        mSelectAuthorsDialog.setItems(authorsList.toArray(new String[0]), (dialogInterface, i) -> {
            mSelectedAuthor = authors.get(i);
            showAuthorViewSelect();
        });
        if(!OPDSActivity.this.isFinishing()) {
            mSelectAuthorsDialog.show();
        }
    }

    private void loadAuthor(int which, String authorId) {
        String url = null;
        switch (which) {
            case 0:
                url = "/opds/authorsequences/" + authorId;
                break;
            case 1:
                url = "/opds/author/" + authorId + "/authorsequenceless";
                break;
            case 2:
                url = "/opds/author/" + authorId + "/alphabet";
                break;
            case 3:
                url = "/opds/author/" + authorId + "/time";
                break;
        }
        if (url != null) {
            scrollToTop();
            doSearch(URLHelper.getBaseOPDSUrl() + url);
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
            showTorRestartDialog();
        }
    }

    private void showTorRestartDialog() {
        hideWaitingDialog();
        if (mTorRestartDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.tor_is_stopped)
                    .setMessage(R.string.tor_restart_dialog_message)
                    .setPositiveButton(R.string.restart_tor_message, (dialog, which) -> {
                        App.getInstance().startTor();
                        dialog.dismiss();
                        // вернусь в основное активити и подожду перезапуска
                        Intent intent = new Intent(OPDSActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    })
                    .setCancelable(false);
            mTorRestartDialog = dialogBuilder.create();
        }
        if(!OPDSActivity.this.isFinishing()) {
            mTorRestartDialog.show();
        }
    }


    private void handleLoading() {
        // если приложение только запущено
        if (sFirstLoad) {
            startBrowsing();
            sFirstLoad = false;
        }
    }

    private void startBrowsing() {
        // гружу последнюю загруженную страницу
        //doSearch(MyPreferences.getInstance().getLastLoadUrl());
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
                    doSearch(lastPage);
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
                    return true;
                } else {
                    Toast.makeText(this, "Нечего загружать. Нажмите ещё раз для выхода", Toast.LENGTH_SHORT).show();
                    mConfirmExit = System.currentTimeMillis();
                    return true;
                }
            } else {
                Toast.makeText(this, "Нечего загружать. Нажмите ещё раз для выхода", Toast.LENGTH_SHORT).show();
                mConfirmExit = System.currentTimeMillis();
                return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }


    private void showDownloadsDialog(final ArrayList<DownloadLink> downloadLinks) {
        LayoutInflater inflate = getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflate.inflate(R.layout.confirm_book_type_select, null);
        Switch checker = view.findViewById(R.id.save_only_selected);
        checker.setChecked(App.getInstance().isSaveOnlySelected());
        checker = view.findViewById(R.id.reDownload);
        checker.setChecked(App.getInstance().isReDownload());

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
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
        for (String s :
                linksArray) {
            Log.d("surprise", "OPDSActivity showDownloadsDialog: " + s);
        }
        dialogBuilder.setItems(linksArray, (dialogInterface, i) -> {
            // проверю, выбрано ли сохранение формата загрузки
            Dialog dialog = (Dialog) dialogInterface;
            Switch switcher = dialog.findViewById(R.id.save_type_selection);
            if (switcher.isChecked()) {
                // запомню выбор формата
                Toast.makeText(OPDSActivity.this, "Предпочтительный формат для скачивания сохранён (" + MimeTypes.getFullMime(linksArray[i]) + "). Вы можете сбросить его в настройки +> разное.", Toast.LENGTH_LONG).show();
                App.getInstance().saveFavoriteMime(MimeTypes.getFullMime(linksArray[i]));
                Log.d("surprise", "OPDSActivity onClick: saved format is " + App.getInstance().getFavoriteMime());
            }
            switcher = dialog.findViewById(R.id.save_only_selected);
            App.getInstance().setSaveOnlySelected(switcher.isChecked());
            switcher = dialog.findViewById(R.id.reDownload);
            App.getInstance().setReDownload(switcher.isChecked());
            invalidateOptionsMenu();
            // получу сокращённый MIME
            String shortMime = linksArray[i];
            String longMime = MimeTypes.getFullMime(shortMime);
            int counter1 = 0;
            int linksLength1 = downloadLinks.size();
            DownloadLink item;
            while (counter1 < linksLength1) {
                item = downloadLinks.get(counter1);
                if (item.mime.equals(longMime)) {
                    mMyViewModel.addToDownloadQueue(item);
                    break;
                }
                counter1++;
            }
            Toast.makeText(OPDSActivity.this, "Книга добавлена в очередь загрузок", Toast.LENGTH_SHORT).show();
        })
                .setView(view);
        AlertDialog dialog = dialogBuilder.create();
        if(!OPDSActivity.this.isFinishing()) {
            dialog.show();
        }
    }


    private class ResetApp implements Runnable {
        @Override
        public void run() {
            Intent intent = new Intent(OPDSActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            OPDSActivity.this.startActivity(intent);
            Runtime.getRuntime().exit(0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Uri treeUri = data.getData();
                    if (treeUri != null) {
                        // проверю наличие файла
                        DocumentFile dl = DocumentFile.fromTreeUri(App.getInstance(), treeUri);
                        if (dl != null && dl.isDirectory()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                App.getInstance().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                App.getInstance().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            }
                            App.getInstance().setDownloadDir(treeUri);
                            Toast.makeText(this, getText(R.string.download_folder_changed_message_new), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        } else if (requestCode == READ_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getExtras() != null) {
                    String folderLocation = data.getExtras().getString("data");
                    if (folderLocation != null) {
                        File destination = new File(folderLocation);
                        if (destination.exists()) {
                            App.getInstance().setDownloadDir(Uri.parse(folderLocation));
                            Toast.makeText(this, getText(R.string.download_folder_changed_message) + folderLocation, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        } else if (requestCode == START_TOR) {
            // перезагружу страницу
            String lastUrl = App.getInstance().getLastHistoryElement();
            if (lastUrl != null && !lastUrl.isEmpty()) {
                showLoadWaitingDialog();
                mMyViewModel.request(lastUrl);
            }
        } else if (requestCode == BACKUP_FILE_REQUEST_CODE) {
            // выбран файл, вероятно с бекапом
            if (resultCode == Activity.RESULT_OK) {
                Uri uri;
                if (data != null) {
                    uri = data.getData();
                    if (uri != null)
                        mMyViewModel.restore(uri);
                    Toast.makeText(OPDSActivity.this, "Настройки приложения восстановлены", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void changeTitle(String s) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(getSupportActionBar()).setTitle(s);
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

}
