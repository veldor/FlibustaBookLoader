package net.veldor.flibustaloader.ui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.github.clans.fab.FloatingActionMenu;
import com.google.android.material.snackbar.Snackbar;
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebClient;
import net.veldor.flibustaloader.MyWebViewClient;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.SubscribeActivity;
import net.veldor.flibustaloader.adapters.SearchResultsAdapter;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.database.entity.DownloadedBooks;
import net.veldor.flibustaloader.dialogs.GifDialog;
import net.veldor.flibustaloader.dialogs.GifDialogListener;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.utils.Grammar;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.utils.TransportUtils;
import net.veldor.flibustaloader.utils.URLHandler;
import net.veldor.flibustaloader.utils.XMLHandler;
import net.veldor.flibustaloader.utils.XMLParser;
import net.veldor.flibustaloader.view_models.MainViewModel;
import net.veldor.flibustaloader.workers.DownloadBooksWorker;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import lib.folderpicker.FolderPicker;

import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;
import static net.veldor.flibustaloader.ui.MainActivity.START_TOR;

public class OPDSActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private static final String FLIBUSTA_SEARCH_BOOK_REQUEST = "http://flibustahezeous3.onion/opds/search?searchType=books&searchTerm=";
    private static final String FLIBUSTA_SEARCH_AUTHOR_REQUEST = "http://flibustahezeous3.onion/opds/search?searchType=authors&searchTerm=";
    private static final String FLIBUSTA_VPN_SEARCH_BOOK_REQUEST = "http://flibusta.is/opds/search?searchType=books&searchTerm=";
    private static final String FLIBUSTA_VPN_SEARCH_AUTHOR_REQUEST = "http://flibusta.is/opds/search?searchType=authors&searchTerm=";
    private static final String[] bookSortOptions = new String[]{"По названию книги", "По размеру", "По количеству скачиваний", "По серии", "По жанру", "По автору"};
    private static final String[] authorSortOptions = new String[]{"По имени автора от А", "По имени автора от Я", "По количеству книг от большего", "По количеству книг от меньшего"};
    private static final String[] otherSortOptions = new String[]{"От А", "От Я"};
    private static final int READ_REQUEST_CODE = 5;
    private static final int REQUEST_CODE = 7;
    private static final int BACKUP_FILE_REQUEST_CODE = 8;
    private AlertDialog mTorRestartDialog;
    private MainViewModel mMyViewModel;
    private LiveData<AndroidOnionProxyManager> mTorClient;
    private SearchView mSearchView;
    private ArrayList<String> autocompleteStrings;
    private ArrayAdapter<String> mSearchAdapter;
    private RadioGroup mSearchRadioContainer;
    private SearchResultsAdapter mSearchResultsAdapter;
    private Dialog mShowLoadDialog;
    private Button mLoadMoreBtn;
    private TorConnectErrorReceiver mTorConnectErrorReceiver;
    private final String[] mAuthorViewTypes = new String[]{"Книги по сериям", "Книги вне серий", "Книги по алфавиту", "Книги по дате поступления"};
    private AlertDialog mSelectAuthorViewDialog;
    private AlertDialog.Builder mSelectAuthorsDialog;
    private RecyclerView mRecycler;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (App.getInstance().getNightMode()) {
            setTheme(R.style.NightTheme);
        }

        setContentView(R.layout.activity_odps);

        // добавлю viewModel
        mMyViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupUI();

        // проверю очередь загрузки
        checkDownloadQueue();

        // переназову окно
        ActionBar actionbar = getActionBar();
        if (actionbar != null) {
            actionbar.setTitle("OPDS");
        }

        mSearchTitle = findViewById(R.id.search_title);

        mRootView = findViewById(R.id.rootView);

        // определю кнопки прыжков по результатам
        mLoadMoreBtn = findViewById(R.id.load_more_button);

        // кнопка перехода вперёд
        mForwardBtn = findViewById(R.id.forward_btn);
        mBackwardBtn = findViewById(R.id.backward_btn);

        mBackwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.getInstance().mResultsEscalate = false;
                if (App.getInstance().havePreviousPage()) {
                    String lastUrl = App.getInstance().getPreviousPageUrl();
                    doSearchFromHistory(lastUrl);
                } else if (mConfirmExit != 0) {
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
            }
        });

        mForwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // нужно составить список загруженных страниц
                App.getInstance().mSearchHistory.add(App.BASE_URL + App.getInstance().mNextPageUrl);
                String nextUrl = App.getInstance().mNextPageUrl;
                if (nextUrl != null && !nextUrl.isEmpty()) {
                    App.getInstance().mResultsEscalate = false;
                    mMyViewModel.loadNextPage();
                    showLoadWaitingDialog();
                } else {
                    Toast.makeText(OPDSActivity.this, "Результаты закончились", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mRecycler = findViewById(R.id.booksList);

        // зарегистрирую получатель ошибки подключения к TOR
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyWebViewClient.TOR_CONNECT_ERROR_ACTION);
        mTorConnectErrorReceiver = new TorConnectErrorReceiver();
        registerReceiver(mTorConnectErrorReceiver, filter);
        handleLoading();

        // создам тестовый массив строк для автозаполнения
        autocompleteStrings = mMyViewModel.getSearchAutocomplete();

        // при нажатии кнопки назад- убираем последний элемент истории и переходим на предпоследний
        mLoadMoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // загружаю следующую страницу
                App.getInstance().mResultsEscalate = true;
                mMyViewModel.loadNextPage();
                showLoadWaitingDialog();

            }
        });


        // добавлю ссылку на тип поиска
        mSearchRadioContainer = findViewById(R.id.subscribe_type);
        // добавлю отслеживание изменения ваианта поиска
        mSearchRadioContainer.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mFab.setVisibility(View.GONE);
                // очищу список найденного
                if (mSearchResultsAdapter != null) {
                    mSearchResultsAdapter.nothingFound();
                    mSearchResultsAdapter.notifyDataSetChanged();
                }
                // скрою кнопку "показать больше"
                if (mLoadMoreBtn != null) {
                    mLoadMoreBtn.setVisibility(View.GONE);
                    mForwardBtn.setVisibility(View.GONE);
                    mBackwardBtn.setVisibility(View.GONE);
                }
                // если выбран автор или книга- покажу окно поиска, если жанр- скрою окно поиска и поищу жанры
                if (mSearchView != null) {
                    switch (checkedId) {
                        case R.id.searchBook:
                        case R.id.searchAuthor:
                            mSearchView.setVisibility(View.VISIBLE);
                            mSearchView.clearFocus();
                            mSearchView.setFocusable(true);
                            mSearchView.setIconified(false);
                            mSearchView.requestFocusFromTouch();
                            break;
                        case R.id.searchGenre:
                            mSearchView.setVisibility(View.GONE);
                            showLoadWaitingDialog();
                            App.getInstance().addToHistory(URLHandler.getBaseUrl() + "/opds/genres");
                            mMyViewModel.request(URLHandler.getBaseUrl() + "/opds/genres");
                            break;
                        case R.id.searchSequence:
                            mSearchView.setVisibility(View.GONE);
                            showLoadWaitingDialog();
                            App.getInstance().addToHistory(URLHandler.getBaseUrl() + "/opds/sequencesindex");
                            mMyViewModel.request(URLHandler.getBaseUrl() + "/opds/sequencesindex");
                            break;
                    }
                }
            }
        });

        // заголовок поиска
        LiveData<String> searchTitle = App.getInstance().mSearchTitle;
        searchTitle.observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (s != null) {
                    mSearchTitle.setVisibility(View.VISIBLE);
                    mSearchTitle.setText(s);
                } else {
                    mSearchTitle.setVisibility(View.GONE);
                }
            }
        });

        // добавлю отслеживание получения списка ссылок на скачивание
        LiveData<ArrayList<DownloadLink>> downloadLinks = App.getInstance().mDownloadLinksList;
        downloadLinks.observe(this, new Observer<ArrayList<DownloadLink>>() {
            @Override
            public void onChanged(@Nullable ArrayList<DownloadLink> downloadLinks) {
                if (downloadLinks != null && downloadLinks.size() > 0) {
                    if (downloadLinks.size() == 1) {
                        // добавлю книгу в очередь скачивания
                        mMyViewModel.addToDownloadQueue(downloadLinks.get(0));
                        Toast.makeText(OPDSActivity.this, R.string.book_added_to_schedule_message, Toast.LENGTH_LONG).show();
                    } else {
                        // покажу диалог для выбора ссылки для скачивания
                        showDownloadsDialog(downloadLinks);
                    }
                }
            }
        });
        // добавлю отслеживание получения списка ссылок на скачивание
        LiveData<ArrayList<FoundedSequence>> foundedSequences = App.getInstance().mSelectedSequences;
        foundedSequences.observe(this, new Observer<ArrayList<FoundedSequence>>() {
            @Override
            public void onChanged(@Nullable ArrayList<FoundedSequence> sequences) {
                if (sequences != null) {
                    // покажу диалог для выбора ссылки для скачивания
                    showSequenceSelectDialog(sequences);
                }
            }
        });

        // добавлю отслеживание выбора типа отображения автора
        LiveData<Author> selectedAuthor = App.getInstance().mSelectedAuthor;
        selectedAuthor.observe(this, new Observer<Author>() {
            @Override
            public void onChanged(@Nullable Author author) {
                if (author != null) {
                    mSelectedAuthor = author;
                    showAuthorViewSelect();
                }
            }
        });

        // добавлю отслеживание выбора автора
        LiveData<ArrayList<Author>> authorsList = App.getInstance().mSelectedAuthors;
        authorsList.observe(this, new Observer<ArrayList<Author>>() {
            @Override
            public void onChanged(@Nullable ArrayList<Author> authors) {
                if (authors != null) {
                    showAuthorsSelect(authors);
                }
            }
        });

        // добавлю отслеживание выбора серии
        LiveData<FoundedSequence> selectedSequence = App.getInstance().mSelectedSequence;
        selectedSequence.observe(this, new Observer<FoundedSequence>() {
            @Override
            public void onChanged(@Nullable FoundedSequence foundedSequence) {
                if (foundedSequence != null) {
                    showLoadWaitingDialog();
                    String searching = URLHandler.getBaseUrl() + foundedSequence.link;
                    App.getInstance().addToHistory(searching);
                    // уточню, что происходит переход от серий к книгам
                    App.getInstance().mResultsEscalate = false;
                    mMyViewModel.request(searching);
                }
            }
        });
        // добавлю отслеживание выбора жанра
        LiveData<Genre> selectedGenre = App.getInstance().mSelectedGenre;
        selectedGenre.observe(this, new Observer<Genre>() {
            @Override
            public void onChanged(@Nullable Genre foundedGenre) {
                if (foundedGenre != null) {
                    showLoadWaitingDialog();
                    // уточню, что происходит переход от жанра к книгам
                    App.getInstance().mResultsEscalate = false;
                    String searching = URLHandler.getBaseUrl() + foundedGenre.term;
                    App.getInstance().addToHistory(searching);
                    mMyViewModel.request(searching);
                }
            }
        });

        // добавлю обсерверы
        addObservers();
        checkUpdates();

/*        // попробую создать user guide
        new MaterialIntroView.Builder(this)
                .enableDotAnimation(true)
                .enableIcon(false)
                .setFocusGravity(FocusGravity.CENTER)
                .setFocusType(Focus.MINIMUM)
                .setDelayMillis(500)
                .enableFadeAnimation(true)
                .performClick(true)
                .setInfoText("Введите название того, что вы хотите найти.")
                .setTarget(mSearchView)
                .setShape(ShapeType.RECTANGLE)
                .setUsageId("intro_card4") //THIS SHOULD BE UNIQUE ID
                .show();*/
    }

    private void checkDownloadQueue() {
        Boolean queue = mMyViewModel.checkDownloadQueue();
        if (queue) {
            // продолжу загрузку книг
            final LiveData<List<BooksDownloadSchedule>> schedule = App.getInstance().mDatabase.booksDownloadScheduleDao().getAllBooksLive();
            schedule.observe(this, new Observer<List<BooksDownloadSchedule>>() {
                @Override
                public void onChanged(List<BooksDownloadSchedule> booksDownloadSchedule) {
                    if (booksDownloadSchedule != null) {
                        if (booksDownloadSchedule.size() > 0) {
                            // покажу диалог с предложением докачать файлы
                            schedule.removeObservers(OPDSActivity.this);
                            continueDownload();
                        }
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
        new AlertDialog.Builder(OPDSActivity.this)
                .setTitle(R.string.re_download_dialog_header_message)
                .setMessage(R.string.re_download_dialog_body_message)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mMyViewModel.initiateMassDownload();
                        Toast.makeText(OPDSActivity.this, R.string.download_continued_message, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .create().show();
    }

    private void setupUI() {
        Switch massLoadSwitcher = findViewById(R.id.showAllSwitcher);
        if (massLoadSwitcher != null) {
            massLoadSwitcher.setChecked(App.getInstance().isDownloadAll());
            massLoadSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    App.getInstance().switchDownloadAll();
                }
            });
        }
        mFab = findViewById(R.id.floatingMenu);
        mFab.setVisibility(View.GONE);

        View downloadAllButton = findViewById(R.id.fabDownloadAll);
        if (downloadAllButton != null) {
            downloadAllButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
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
                        successSelect.observe(OPDSActivity.this, new Observer<Boolean>() {
                            @Override
                            public void onChanged(Boolean selected) {
                                if (selected) {
                                    downloadAllBooks();
                                }
                            }
                        });
                    }
                    mFab.close(true);
                }
            });
        }
        View downloadUnloadedButton = findViewById(R.id.fabDOwnloadUnloaded);
        if (downloadUnloadedButton != null) {
            downloadUnloadedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String favoriteFormat = App.getInstance().getFavoriteMime();
                    if (favoriteFormat != null) {
                        downloadUnloaded();
                    } else {
                        // покажу диалог выбора предпочтительнго типа скачивания
                        // сброшу отслеживание удачного выбора типа книги
                        App.getInstance().mTypeSelected.setValue(false);
                        selectBookTypeDialog();
                        LiveData<Boolean> successSelect = App.getInstance().mTypeSelected;
                        successSelect.observe(OPDSActivity.this, new Observer<Boolean>() {
                            @Override
                            public void onChanged(Boolean selected) {
                                if (selected) {
                                    downloadUnloaded();
                                }
                            }
                        });
                    }
                    mFab.close(true);
                }
            });
        }
        View downloadSelectButton = findViewById(R.id.fabDOwnloadSelected);
        if (downloadSelectButton != null) {
            downloadSelectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String favoriteFormat = App.getInstance().getFavoriteMime();
                    if (favoriteFormat != null) {
                        showDownloadSelectedDialog();
                    } else {
                        // покажу диалог выбора предпочтительнго типа скачивания
                        // сброшу отслеживание удачного выбора типа книги
                        App.getInstance().mTypeSelected.setValue(false);
                        selectBookTypeDialog();
                        mSuccessSelect = App.getInstance().mTypeSelected;
                        mSuccessSelect.observe(OPDSActivity.this, new Observer<Boolean>() {
                            @Override
                            public void onChanged(Boolean selected) {
                                if (selected) {
                                    showDownloadSelectedDialog();
                                    mSuccessSelect.removeObservers(OPDSActivity.this);
                                }
                            }
                        });
                    }
                    mFab.close(true);
                }
            });
        }
    }

    private void downloadUnloaded() {
        LiveData<WorkInfo> status = mMyViewModel.downloadAll(true);
        observeBookScheduleAdd(status);
    }

    private void showDownloadSelectedDialog() {
        // получу названия книг
        ArrayList<FoundedItem> books = App.getInstance().mParsedResult.getValue();
        if (books != null && books.size() > 0 && books.get(0) instanceof FoundedBook) {
            FoundedBook book;
            String[] variants = new String[books.size()];
            int counter = 0;
            for (FoundedItem fb : books) {
                book = (FoundedBook) fb;
                variants[counter] = String.format(Locale.ENGLISH, "%s \n %s \n %s \n", book.name, book.format, (book.translate.isEmpty() ? "" : book.translate));
                counter++;
            }
            mDownloadSelectedDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.select_books_for_download_message)
                    .setMultiChoiceItems(variants, null, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        }
                    })
                    .setPositiveButton(R.string.download_selected_message, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mDownloadSelectedDialog.getListView().getCheckedItemCount();
                            SparseBooleanArray ids = mDownloadSelectedDialog.getListView().getCheckedItemPositions();
                            if (ids.size() > 0) {
                                LiveData<WorkInfo> status = mMyViewModel.downloadSelected(ids);
                                observeBookScheduleAdd(status);
                            } else {
                                Toast.makeText(OPDSActivity.this, R.string.books_not_selected_message, Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .create();
            mDownloadSelectedDialog.show();
        } else {
            Toast.makeText(this, R.string.books_not_found_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void observeBookScheduleAdd(final LiveData<WorkInfo> status) {
        status.observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null && workInfo.getState() == SUCCEEDED) {
                    Toast.makeText(OPDSActivity.this, "Книги добавлены в очередь скачивания", Toast.LENGTH_LONG).show();
                    // запущу скачивание
                    mMyViewModel.initiateMassDownload();
                    startActivity(new Intent(getBaseContext(), ActivityBookDownloadSchedule.class));
                    status.removeObservers(OPDSActivity.this);
                }
            }
        });
    }

    private void checkUpdates() {
        if (App.getInstance().isCheckUpdate()) {
            // проверю обновления
            final LiveData<Boolean> version = mMyViewModel.startCheckUpdate();
            version.observe(this, new Observer<Boolean>() {
                @Override
                public void onChanged(@Nullable Boolean aBoolean) {
                    if (aBoolean != null && aBoolean) {
                        // показываю Snackbar с уведомлением
                        makeUpdateSnackbar();
                    }
                    version.removeObservers(OPDSActivity.this);
                }
            });
        }
    }


    private void makeUpdateSnackbar() {
        Snackbar updateSnackbar = Snackbar.make(mRootView, getString(R.string.snackbar_found_update_message), Snackbar.LENGTH_INDEFINITE);
        updateSnackbar.setAction(getString(R.string.snackbar_update_action_message), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMyViewModel.initializeUpdate();
            }
        });
        updateSnackbar.setActionTextColor(getResources().getColor(android.R.color.white));
        updateSnackbar.show();
    }

    private void addObservers() {
        // добавлю отслеживание получения результатов поиска
        LiveData<String> searchResultText = App.getInstance().mSearchResult;
        searchResultText.observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (s != null && !s.isEmpty()) {
                    App.getInstance().mResponse = s;
                    // получен ответ сервера, разберу его и положу содержимое в список
                    XMLParser.handleResults();
                } else {
                    hideWaitingDialog();
                }

                // отслеживание отображения обложки книги
                MutableLiveData<FoundedBook> bookWithCover = App.getInstance().mShowCover;
                bookWithCover.observe(OPDSActivity.this, new Observer<FoundedBook>() {
                    @Override
                    public void onChanged(FoundedBook foundedBook) {
                        if (foundedBook != null && foundedBook.preview != null && foundedBook.preview.getByteCount() > 0) {
                            Log.d("surprise", "onChanged: show again");
                            showPreview(foundedBook);
                        }
                    }
                });
            }
        });

        // отслеживание полученных результатов
        LiveData<ArrayList<FoundedItem>> handledResults = App.getInstance().mParsedResult;
        handledResults.observe(this, new Observer<ArrayList<FoundedItem>>() {
            @Override
            public void onChanged(@Nullable ArrayList<FoundedItem> arrayList) {
                if (arrayList != null) {
                    // проверю, если найдены книги- покажу поле для скачивания
                    if (arrayList.size() > 0 && arrayList.get(0) instanceof FoundedBook) {
                        mFab.setVisibility(View.VISIBLE);
                    } else {
                        mFab.setVisibility(View.GONE);
                    }
                    // если есть возможность дальнейшей загрузки данных- покажу кнопку загрузки, иначе- скрою её
                    if (App.getInstance().isDownloadAll()) {
                        mLoadMoreBtn.setVisibility(View.GONE);
                        mForwardBtn.setVisibility(View.GONE);
                        mBackwardBtn.setVisibility(View.GONE);
                    } else {
                        if (App.getInstance().mNextPageUrl != null) {
                            mLoadMoreBtn.setVisibility(View.VISIBLE);
                            mForwardBtn.setVisibility(View.VISIBLE);
                            mBackwardBtn.setVisibility(View.VISIBLE);
                        } else {
                            mLoadMoreBtn.setVisibility(View.GONE);
                            mForwardBtn.setVisibility(View.GONE);
                        }
                    }

                    // если была дополнительная загрузка данных и есть адаптер- догружаю в него данные. Иначе- добавляю адаптер
                    if (App.getInstance().mResultsEscalate) {
                        if (mSearchResultsAdapter == null)
                            mSearchResultsAdapter = new SearchResultsAdapter(arrayList);
                        else
                            mSearchResultsAdapter.setContent(arrayList);
                        mSearchResultsAdapter.notifyDataSetChanged();
                    } else {
                        mSearchResultsAdapter = new SearchResultsAdapter(arrayList);
                        mRecycler.setAdapter(mSearchResultsAdapter);
                        mRecycler.setLayoutManager(new LinearLayoutManager(OPDSActivity.this));
                        scrollToTop();
                    }
                } else {
                    nothingFound();
                }
                hideWaitingDialog();
                App.getInstance().mResultsEscalate = false;
            }
        });
        // добавлю отслеживание показа информации о книге
        LiveData<FoundedBook> selectedBook = App.getInstance().mSelectedBook;
        selectedBook.observe(this, new Observer<FoundedBook>() {
            @Override
            public void onChanged(@Nullable FoundedBook book) {
                if (book != null) {
                    Log.d("surprise", "OPDSActivity onChanged show book info");
                    BookInfoDialog = new AlertDialog.Builder(OPDSActivity.this)
                            .setTitle(book.name)
                            .setMessage(Grammar.textFromHtml(book.bookInfo))
                            .setCancelable(true)
                            .setPositiveButton(android.R.string.ok, null)
                            .create();
                    BookInfoDialog.show();
                }
            }
        });

        // добавлю отслеживание контекстного меню для книги
        LiveData<FoundedBook> contextBook = App.getInstance().mContextBook;
        contextBook.observe(this, new Observer<FoundedBook>() {
            @Override
            public void onChanged(@Nullable FoundedBook foundedBook) {
                // добавлю хранилище для книги
                final FoundedBook book = foundedBook;
                if (foundedBook != null) {
                    // покажу контекстное меню
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(OPDSActivity.this);
                    String[] items = new String[]{"Пометить книгу как прочитанную", "Показать страницу книги"};
                    dialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    // отмечу книгу как прочитанную
                                    mMyViewModel.setBookRead(book);
                                    Toast.makeText(OPDSActivity.this, "Книга отмечена как прочитанная", Toast.LENGTH_LONG).show();
                                    if (mSearchResultsAdapter != null) {
                                        mSearchResultsAdapter.notifyDataSetChanged();
                                    }
                                    break;
                                case 1:
                                    showPage(book);
                            }
                        }
                    });
                    mBookTypeDialog = dialogBuilder.create();
                    mBookTypeDialog.show();
                }
            }
        });

        LiveData<Author> authorNewBooks = App.getInstance().mAuthorNewBooks;
        authorNewBooks.observe(this, new Observer<Author>() {
            @Override
            public void onChanged(@Nullable Author author) {
                if (author != null)
                    doSearch(App.BASE_URL + author.link);
            }
        });

        // отслеживание статуса загрузки книг
        LiveData<String> multiplyDownloadStatus = App.getInstance().mMultiplyDownloadStatus;
        multiplyDownloadStatus.observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (s != null && !s.isEmpty() && mMultiplyDownloadDialog != null) {
                    Window window = mMultiplyDownloadDialog.getWindow();
                    if (window != null) {
                        TextView dialogText = window.findViewById(R.id.title);
                        dialogText.setText(s);
                    }
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
        loadStatus.observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
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
            }
        });

        // добавлю обсервер изменения списка загруженных книг
        LiveData<List<DownloadedBooks>> downloadsList = App.getInstance().mDatabase.downloadedBooksDao().getAllBooksLive();
        downloadsList.observe(this, new Observer<List<DownloadedBooks>>() {
            @Override
            public void onChanged(List<DownloadedBooks> downloadedBooks) {
                if (downloadedBooks != null) {
                    ArrayList<FoundedItem> foundedItems = App.getInstance().mParsedResult.getValue();
                    // если в recyclerView загружены книги- оповещу о том, что список изменился- вдруг данная книга есть в списке
                    if (foundedItems != null && foundedItems.size() > 0 && foundedItems.get(0) instanceof FoundedBook && mSearchResultsAdapter != null) {
                        Log.d("surprise", "OPDSActivity onChanged: items changed");
                        mSearchResultsAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }

    private void showPreview(FoundedBook foundedBook) {
        if (mCoverPreviewDialog == null) {
            mCoverPreviewDialog = new Dialog(OPDSActivity.this, android.R.style.Theme_Light);
            mCoverPreviewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") View dialogLayout = inflater.inflate(R.layout.book_cover, null);
        ImageView imageContainer = dialogLayout.findViewById(R.id.cover_view);
        imageContainer.setImageBitmap(foundedBook.preview);
        mCoverPreviewDialog.setContentView(dialogLayout);
        mCoverPreviewDialog.show();
        mCoverPreviewDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mCoverPreviewDialog = null;
            }
        });
    }

    private void showPage(FoundedBook book) {
        Intent intent = new Intent(this, WebViewActivity.class);
        String link = book.id.split(":")[2];
        intent.setData(Uri.parse(App.BASE_BOOK_URL + link));
        intent.putExtra(WebViewActivity.CALLED, true);
        startActivity(intent);
    }

    private void scrollToTop() {
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    findViewById(R.id.subscriptions_layout).scrollTo(0, 0);
                } catch (NullPointerException e) {
                    Log.d("surprise", "OPDSActivity run nothing scroll");
                }
                //mRecycler.getLayoutManager().scrollToPositionWithOffset(0,0);
            }
            // give a delay of one second
        }, 100);
    }

    private void observeBooksDownload() {
        // отслежу загрузку книг
        final LiveData<WorkInfo> booksDownloadStatus = App.getInstance().mDownloadAllWork;
        if (booksDownloadStatus != null) {
            booksDownloadStatus.observe(this, new Observer<WorkInfo>() {
                @Override
                public void onChanged(@Nullable WorkInfo workInfo) {
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
                            updateSnackbar.setAction("Попробовать ещё раз", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            });
                            updateSnackbar.setActionTextColor(getResources().getColor(android.R.color.white));
                            updateSnackbar.show();
                        }
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
        mSelectSequencesDialog.setItems(sequencesList.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                App.getInstance().mSelectedSequence.postValue(sequences.get(i));
            }
        });
        mSelectSequencesDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        App.getInstance().mContextBook.setValue(null);
        App.getInstance().mSelectedBook.setValue(null);
        App.getInstance().mSelectedAuthor.setValue(null);
        App.getInstance().mSelectedAuthors.setValue(null);
        App.getInstance().mDownloadLinksList.setValue(null);
        App.getInstance().mSelectedGenre.setValue(null);
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
        if (mTorConnectErrorReceiver != null) {
            unregisterReceiver(mTorConnectErrorReceiver);
        }
        App.getInstance().mSelectedAuthor.setValue(null);
        App.getInstance().mSelectedAuthors.setValue(null);
        App.getInstance().mDownloadLinksList.setValue(null);
        App.getInstance().mSelectedGenre.setValue(null);
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

    private void nothingFound() {
        if (mSearchResultsAdapter != null) {
            mSearchResultsAdapter.nothingFound();
            mSearchResultsAdapter.notifyDataSetChanged();
            // очищу историю поиска
        }
        mLoadMoreBtn.setVisibility(View.INVISIBLE);
        mForwardBtn.setVisibility(View.INVISIBLE);
        // если нет истории поиска
        if (!App.getInstance().isSearchHistory()) {
            mBackwardBtn.setVisibility(View.INVISIBLE);
        }
        Toast.makeText(OPDSActivity.this, "По запросу ничего не найдено", Toast.LENGTH_LONG).show();
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
        return true;
    }

    private void selectSorting() {
        String[] sortingOptions;
        // в зависимости от выбранного режима поиска покажу вырианты сортировки
        switch (App.sSearchType) {
            case SEARCH_BOOKS:
                sortingOptions = bookSortOptions;
                break;
            case SEARCH_AUTHORS:
            case SEARCH_NEW_AUTHORS:
                sortingOptions = authorSortOptions;
                break;
            default:
                sortingOptions = otherSortOptions;
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Выберите тип сортировки")
                .setItems(sortingOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sortList(which);
                    }
                });
        // покажу список типов сортировки
        dialog.show();
    }

    private void sortList(int which) {
        if (mSearchResultsAdapter != null) {
            switch (App.sSearchType) {
                case SEARCH_BOOKS:
                    App.getInstance().mBookSortOption = which;
                    // пересортирую то, что уже есть
                    mSearchResultsAdapter.sortBooks();
                    break;
                case SEARCH_AUTHORS:
                case SEARCH_NEW_AUTHORS:
                    App.getInstance().mAuthorSortOptions = which;
                    // пересортирую то, что уже есть
                    mSearchResultsAdapter.sortAuthors();
                    break;
                case SEARCH_GENRE:
                    Log.d("surprise", "OPDSActivity sortList sort genre");
                    App.getInstance().mOtherSortOptions = which;
                    // пересортирую то, что уже есть
                    mSearchResultsAdapter.sortGenres();
                    break;
                case SEARCH_SEQUENCE:
                    App.getInstance().mOtherSortOptions = which;
                    // пересортирую то, что уже есть
                    mSearchResultsAdapter.sortSequences();
                    break;
                default:
                    App.getInstance().mOtherSortOptions = which;
            }
        }
        App.getInstance().mBookSortOption = which;
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

        }
        return super.onOptionsItemSelected(item);
    }

    private void handleUseExternalVpn() {
        // если использовался внешний VPN- просто отключу его использование
        if(App.getInstance().isExternalVpn()){
            App.getInstance().switchExternalVpnUse();
            invalidateOptionsMenu();
        }
        else{
            // покажу диалог с объяснением последствий включения VPN
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder
                    .setTitle("Использование внешнего VPN")
                    .setMessage("Оповестить об использовании внешнего VPN. В этом случае внутренний клиент TOR будет отключен и траффик приложения не будет обрабатываться. В этом случае вся ответственность за получение контента ложится на внешний VPN. Если вы будете получать сообщения об ошибках загрузки- значит, он работает неправильно. Сделано для версий Android ниже 6.0, где могут быть проблемы с доступом, но может быть использовано по желанию на ваш страх и риск.")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            App.getInstance().switchExternalVpnUse();
                            invalidateOptionsMenu();
                        }
                    });
            dialogBuilder.create().show();
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
                .setItems(MimeTypes.MIMES_LIST, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Dialog dialog = (Dialog) dialogInterface;
                        Switch switcher = dialog.findViewById(R.id.save_type_selection);
                        if (switcher.isChecked()) {
                            // запомню выбор формата
                            Toast.makeText(OPDSActivity.this, "Предпочтительный формат для скачивания сохранён. Вы можете сбросить его в настройки +> разное.", Toast.LENGTH_LONG).show();
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
                    }
                })
                .setView(view);
        mSelectBookTypeDialog = dialogBuilder.create();
        // покажу диалог с выбором предпочтительного формата
        mSelectBookTypeDialog.show();
    }

    private void showMultiplyDownloadDialog() {
        if (mMultiplyDownloadDialog == null) {
            mMultiplyDownloadDialog = new GifDialog.Builder(this)
                    .setTitle(getString(R.string.download_books_title))
                    .setMessage(getString(R.string.download_books_msg))
                    .setGifResource(R.drawable.loading)   //Pass your Gif here
                    .isCancellable(false)
                    .setPositiveBtnText("Отменить")
                    .OnPositiveClicked(new GifDialogListener() {
                        @Override
                        public void OnClick() {
                            if (App.getInstance().mProcess != null) {
                                Log.d("surprise", "OPDSActivity OnClick kill process");
                                WorkManager.getInstance(OPDSActivity.this).cancelWorkById(App.getInstance().mProcess.getId());
                            }
                            if (mMultiplyDownloadDialog != null) {
                                mMultiplyDownloadDialog.dismiss();
                            }
                            // отменю операцию
                            Toast.makeText(OPDSActivity.this, "Загрузка книг отменена", Toast.LENGTH_LONG).show();

                        }
                    })
                    .build();
        }
        //mMultiplyDownloadDialog.setMessage("Считаю количество книг для скачивания");
        mMultiplyDownloadDialog.show();
    }

    private void showNewDialog() {
        String[] newCategory = new String[]{"Все новинки", "Новые книги по жанрам", "Новые книги по авторам", "Новые книги по сериям"};
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.showNewDialogTitle)
                .setItems(newCategory, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // сброшу ранее загруженное
                        App.getInstance().mResultsEscalate = false;
                        switch (which) {
                            case 0:
                                App.sSearchType = SEARCH_BOOKS;
                                doSearch(URLHandler.getBaseUrl() + "/opds/new/0/new");
                                break;
                            case 1:
                                doSearch(URLHandler.getBaseUrl() + "/opds/newgenres");
                                break;
                            case 2:
                                doSearch(URLHandler.getBaseUrl() + "/opds/newauthors");
                                break;
                            case 3:
                                doSearch(URLHandler.getBaseUrl() + "/opds/newsequences");
                                break;
                        }
                    }
                })
                .show();
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
                makeSearch(s);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void makeSearch(String s) throws UnsupportedEncodingException {
        // новый поиск, отключу автодобавление
        App.getInstance().mResultsEscalate = false;
        // занесу значение в список автозаполнения
        if (XMLHandler.putSearchValue(s)) {
            // обновлю список поиска
            autocompleteStrings = mMyViewModel.getSearchAutocomplete();
            mSearchAdapter.clear();
            mSearchAdapter.addAll(autocompleteStrings);
            mSearchAdapter.notifyDataSetChanged();
        }

        String searchString = URLEncoder.encode(s, "utf-8").replace("+", "%20");

        // опознаю тип поиска
        if(App.getInstance().isExternalVpn()){
            if (mSearchRadioContainer.getCheckedRadioButtonId() == R.id.searchBook) {
                doSearch(FLIBUSTA_VPN_SEARCH_BOOK_REQUEST + searchString);
            } else {
                doSearch(FLIBUSTA_VPN_SEARCH_AUTHOR_REQUEST + searchString);
            }
        }
        else{
            if (mSearchRadioContainer.getCheckedRadioButtonId() == R.id.searchBook) {
                doSearch(FLIBUSTA_SEARCH_BOOK_REQUEST + searchString);
            } else {
                doSearch(FLIBUSTA_SEARCH_AUTHOR_REQUEST + searchString);
            }
        }
    }

    private void doSearch(String s) {
            if (s != null && !s.isEmpty()) {
                // очищу историю поиска и положу туда начальное значение
                App.getInstance().addToHistory(s);
                showLoadWaitingDialog();
                mMyViewModel.request(s);
                // добавлю значение в историю
            } else {
                Toast.makeText(this, R.string.empty_request_message, Toast.LENGTH_SHORT).show();
            }
    }

    private void doSearchFromHistory(String s) {
        showLoadWaitingDialog();
        mMyViewModel.request(s);
    }

    private void showLoadWaitingDialog() {

        if (mShowLoadDialog == null) {

            mShowLoadDialog = new GifDialog.Builder(this)
                    .setTitle(getString(R.string.load_waiting_title))
                    .setMessage(getString(R.string.load_waiting_message))
                    .setGifResource(R.drawable.loading)   //Pass your Gif here
                    .isCancellable(false)
                    .setPositiveBtnText("Отменить")
                    .OnPositiveClicked(new GifDialogListener() {
                        @Override
                        public void OnClick() {
                            if (App.getInstance().mProcess != null) {
                                WorkManager.getInstance(OPDSActivity.this).cancelWorkById(App.getInstance().mProcess.getId());
                            }
                            hideWaitingDialog();
                            // отменю операцию
                            Toast.makeText(OPDSActivity.this, "Загрузка отменена", Toast.LENGTH_LONG).show();

                        }
                    })
                    .build();
        }
        mShowLoadDialog.show();

    }


    private void showAuthorViewSelect() {
        // получу идентификатор автора
        if (mSelectAuthorViewDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.select_author_view_message)
                    .setItems(mAuthorViewTypes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loadAuthor(which, mSelectedAuthor.uri);
                        }
                    })
            ;
            mSelectAuthorViewDialog = dialogBuilder.create();
        }
        mSelectAuthorViewDialog.show();
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
        mSelectAuthorsDialog.setItems(authorsList.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mSelectedAuthor = authors.get(i);
                showAuthorViewSelect();
            }
        });
        mSelectAuthorsDialog.show();
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
            showLoadWaitingDialog();
            String searching = URLHandler.getBaseUrl() + url;
            App.getInstance().addToHistory(searching);
            mMyViewModel.request(searching);
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
                    .setPositiveButton(R.string.restart_tor_message, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            App.getInstance().startTor();
                            dialog.dismiss();
                            // вернусь в основное активити и подожду перезапуска
                            Intent intent = new Intent(OPDSActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    })
                    .setCancelable(false);
            mTorRestartDialog = dialogBuilder.create();
        }
        mTorRestartDialog.show();
    }


    private void handleLoading() {
        if (mTorClient == null) {
            // если клиент не загружен- загружаю
            mTorClient = mMyViewModel.getTor();
        }
        // ещё одна проверка, может вернуться null
        if (mTorClient.getValue() == null) {
            // подожду, пока TOR загрузится
            mTorClient.observe(this, new Observer<AndroidOnionProxyManager>() {
                @Override
                public void onChanged(@Nullable AndroidOnionProxyManager androidOnionProxyManager) {
                    if (androidOnionProxyManager != null) {
                        startBrowsing();
                    }
                }
            });
        } else {
            // запускаю браузер
            startBrowsing();
        }
    }

    private void startBrowsing() {
        if(!App.getInstance().isExternalVpn()){
            mMyViewModel.setWebClient(new MyWebClient());
        }
        // если нет истории поиска- гружу новинки за неделю, если есть- последнюю загруженную страницу
        if (App.getInstance().isSearchHistory()) {
            mMyViewModel.request(App.getInstance().getLastHistoryElement());
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
            // отменю добавление результатов
            App.getInstance().mResultsEscalate = false;
            if (App.getInstance().havePreviousPage()) {
                String lastUrl = App.getInstance().getPreviousPageUrl();
                doSearchFromHistory(lastUrl);
                return true;
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
        dialogBuilder.setItems(linksArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                // проверю, выбрано ли сохранение формата загрузки
                Dialog dialog = (Dialog) dialogInterface;
                Switch switcher = dialog.findViewById(R.id.save_type_selection);
                if (switcher.isChecked()) {
                    // запомню выбор формата
                    Toast.makeText(OPDSActivity.this, "Предпочтительный формат для скачивания сохранён. Вы можете сбросить его в настройки +> разное.", Toast.LENGTH_LONG).show();
                    App.getInstance().saveFavoriteMime(MimeTypes.getFullMime(MimeTypes.MIMES_LIST[i]));
                }
                switcher = dialog.findViewById(R.id.save_only_selected);
                App.getInstance().setSaveOnlySelected(switcher.isChecked());
                switcher = dialog.findViewById(R.id.reDownload);
                App.getInstance().setReDownload(switcher.isChecked());
                invalidateOptionsMenu();
                // получу сокращённый MIME
                String shortMime = linksArray[i];
                String longMime = MimeTypes.getFullMime(shortMime);
                int counter = 0;
                int linksLength = downloadLinks.size();
                DownloadLink item;
                while (counter < linksLength) {
                    item = downloadLinks.get(counter);
                    if (item.mime.equals(longMime)) {
                        mMyViewModel.addToDownloadQueue(item);
                        Toast.makeText(OPDSActivity.this, "Загрузка началась", Toast.LENGTH_LONG).show();
                        break;
                    }
                    counter++;
                }
            }
        })
                .setView(view);
        dialogBuilder.show();
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
            Log.d("surprise", "OPDSActivity onActivityResult here");
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Uri treeUri = data.getData();
                    if (treeUri != null) {
                        App.getInstance().setNewDownloadFolder(treeUri);
                        Toast.makeText(this, getText(R.string.download_folder_changed_message_new), Toast.LENGTH_LONG).show();
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
                            App.getInstance().setDownloadFolder(Uri.parse(folderLocation));
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

}
