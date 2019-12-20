package net.veldor.flibustaloader;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.adapters.SearchResultsAdapter;
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
import net.veldor.flibustaloader.utils.XMLHandler;
import net.veldor.flibustaloader.utils.XMLParser;
import net.veldor.flibustaloader.view_models.MainViewModel;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import lib.folderpicker.FolderPicker;

import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;
import static net.veldor.flibustaloader.MainActivity.START_TOR;

public class ODPSActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    public static final String BOOK_ID = "book id";
    private static final String FLIBUSTA_SEARCH_BOOK_REQUEST = "http://flibustahezeous3.onion/opds/search?searchType=books&searchTerm=";
    private static final String FLIBUSTA_SEARCH_AUTHOR_REQUEST = "http://flibustahezeous3.onion/opds/search?searchType=authors&searchTerm=";
    private static final String[] bookSortOptions = new String[]{"По названию книги", "По размеру", "По количеству скачиваний", "По серии", "По жанру"};
    private static final int READ_REQUEST_CODE = 5;
    private AlertDialog mTorRestartDialog;
    private MainViewModel mMyViewModel;
    private LiveData<AndroidOnionProxyManager> mTorClient;
    private SearchView mSearchView;
    private ArrayList<String> autocompleteStrings;
    private ArrayAdapter<String> mSearchAdapter;
    private RadioGroup mSearchRadioContainer;
    private MyWebClient mWebClient;
    private SearchResultsAdapter mSearchResultsAdapter;
    private Dialog mShowLoadDialog;
    private Button mLoadMoreBtn;
    private AlertDialog.Builder mDownloadsDialog;
    private TorConnectErrorReceiver mTtorConnectErrorReceiver;
    private String[] mAuthorViewTypes = new String[]{"Книги по сериям", "Книги вне серий", "Книги по алфавиту", "Книги по дате поступления"};
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
    private int mBookSortingOption;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (App.getInstance().getNightMode()) {
            setTheme(R.style.NightTheme);
        }

        setContentView(R.layout.activity_main_odps);

        mSearchTitle = findViewById(R.id.search_title);

        mRootView = findViewById(R.id.rootView);

        // определю кнопки прыжков по результатам
        mLoadMoreBtn = findViewById(R.id.load_more_button);

        mRecycler = findViewById(R.id.search_items_list);

        // добавлю viewModel
        mMyViewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        // зарегистрирую получатель ошибки подключения к TOR
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyWebViewClient.TOR_CONNECT_ERROR_ACTION);
        mTtorConnectErrorReceiver = new TorConnectErrorReceiver();
        registerReceiver(mTtorConnectErrorReceiver, filter);
        handleLoading();

        // создам тестовый массив строк для автозаполнения
        autocompleteStrings = mMyViewModel.getSearchAutocomplete();

        // при нажатии кнопки назад- убираем последний элемент истории и переходим на предпоследний
        mLoadMoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // загружаю следующую страницу
                App.getInstance().mResultsEscalate = true;
                mWebClient.loadNextPage();
                showLoadWaitingDialog();

            }
        });


        // добавлю ссылку на тип поиска
        mSearchRadioContainer = findViewById(R.id.searchType);
        // добавлю отслеживание изменения ваианта поиска
        mSearchRadioContainer.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // очищу список найденного
                if (mSearchResultsAdapter != null) {
                    mSearchResultsAdapter.nothingFound();
                    mSearchResultsAdapter.notifyDataSetChanged();
                }
                // скрою кнопку "показать больше"
                if (mLoadMoreBtn != null) {
                    mLoadMoreBtn.setVisibility(View.GONE);
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
                            App.getInstance().addToHistory("http://flibustahezeous3.onion/opds/genres");
                            mWebClient.search("http://flibustahezeous3.onion/opds/genres");
                            break;
                        case R.id.searchSequence:
                            mSearchView.setVisibility(View.GONE);
                            showLoadWaitingDialog();
                            App.getInstance().addToHistory("http://flibustahezeous3.onion/opds/sequencesindex");
                            mWebClient.search("http://flibustahezeous3.onion/opds/sequencesindex");
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
                        mWebClient.download(downloadLinks.get(0));
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
                    String searching = App.BASE_URL + foundedSequence.link;
                    App.getInstance().addToHistory(searching);
                    // уточню, что происходит переход от серий к книгам
                    App.getInstance().mResultsEscalate = false;
                    mWebClient.search(searching);
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
                    String searching = App.BASE_URL + foundedGenre.term;
                    App.getInstance().addToHistory(searching);
                    mWebClient.search(searching);
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
                    version.removeObservers(ODPSActivity.this);
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
                    App.getInstance().mResponce = s;
                    // получен ответ сервера, разберу его и положу содержимое в список
                    XMLParser.handleResults();
                } else {
                    hideWaitingDialog();
                }
            }
        });

        // отслеживание полученных результатов
        LiveData<ArrayList<FoundedItem>> handledResults = App.getInstance().mParsedResult;
        handledResults.observe(this, new Observer<ArrayList<FoundedItem>>() {
            @Override
            public void onChanged(@Nullable ArrayList<FoundedItem> arrayList) {
                hideWaitingDialog();
                if (arrayList != null) {
                    // если есть возможность дальнейшей загрузки данных- покажу кнопку загрузки, иначе- скрою её
                    if (App.getInstance().mNextPageUrl != null) {
                        mLoadMoreBtn.setVisibility(View.VISIBLE);
                    } else {
                        mLoadMoreBtn.setVisibility(View.GONE);
                    }

                    // если была дополнительная загрузка данных и есть адаптер- догружаю в него данные. Иначе- добавляю адаптер
                    if (App.getInstance().mResultsEscalate) {
                        Log.d("surprise", "ODPSActivity onChanged escalate data");
                        mSearchResultsAdapter.setContent(arrayList);
                        mSearchResultsAdapter.notifyDataSetChanged();
                    } else {
                        Log.d("surprise", "ODPSActivity onChanged replace data");
                        mSearchResultsAdapter = new SearchResultsAdapter(arrayList);
                        mRecycler.setAdapter(mSearchResultsAdapter);
                        mRecycler.setLayoutManager(new LinearLayoutManager(ODPSActivity.this));
                        scrollToTop();
                    }
                } else {
                    nothingFound();
                }
            }
        });
        // добавлю отслеживание показа информации о книге
        LiveData<FoundedBook> selectedBook = App.getInstance().mSelectedBook;
        selectedBook.observe(this, new Observer<FoundedBook>() {
            @Override
            public void onChanged(@Nullable FoundedBook book) {
                if (book != null) {
                    if (!book.readed) {
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ODPSActivity.this);
                        dialogBuilder.setTitle(book.name)
                                .setMessage(Grammar.textFromHtml(book.bookInfo))
                                .setCancelable(true)
                                .setPositiveButton(android.R.string.ok, null);
                        dialogBuilder.show();
                    }
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
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ODPSActivity.this);
                    String[] items = new String[]{"Пометить книгу как прочитанную"};
                    dialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // отмечу книгу как прочитанную
                            mMyViewModel.setBookRead(book);
                            Toast.makeText(ODPSActivity.this, "Книга отмечена как прочитанная", Toast.LENGTH_LONG).show();
                        }
                    });
                    dialogBuilder.create().show();
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
                    TextView dialogText = mMultiplyDownloadDialog.getWindow().findViewById(R.id.title);
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
                        TextView dialogText = mMultiplyDownloadDialog.getWindow().findViewById(R.id.title);
                        dialogText.setText(status);
                    }
                }
            }
            observeBooksDownload();
        }
        // добавлю отслеживание неудачно загруженной книги
        LiveData<String> unloadedBook = App.getInstance().mUnloadedBook;
        unloadedBook.observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (s != null && !s.isEmpty()) {
                    // не удалось загрузить книгу
                    Toast.makeText(ODPSActivity.this, "Не удалось сохранить " + s, Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private void scrollToTop() {
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    findViewById(R.id.scrollLayout).scrollTo(0, 0);
                } catch (NullPointerException e) {
                    Log.d("surprise", "ODPSActivity run nothing scroll");
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
                        if (workInfo.getState() == SUCCEEDED && !App.getInstance().mDownloadInProgress) {
                            Toast.makeText(ODPSActivity.this, "Все книги загружены (кажется)", Toast.LENGTH_LONG).show();
                            // работа закончена, закрою диалог и выведу тост
                            if (mMultiplyDownloadDialog != null) {
                                mMultiplyDownloadDialog.dismiss();
                                booksDownloadStatus.removeObservers(ODPSActivity.this);
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


    @Override
    protected void onResume() {
        super.onResume();
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
    protected void onDestroy() {
        super.onDestroy();
        if (mTtorConnectErrorReceiver != null) {
            unregisterReceiver(mTtorConnectErrorReceiver);
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
    }

    private void nothingFound() {
        if (mSearchResultsAdapter != null) {
            mSearchResultsAdapter.nothingFound();
            mSearchResultsAdapter.notifyDataSetChanged();
            // очищу историю поиска
        }
        Toast.makeText(ODPSActivity.this, "По запросу ничего не найдено", Toast.LENGTH_LONG).show();
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

        SearchView.SearchAutoComplete searchAutoComplete = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setDropDownBackgroundResource(R.color.background_color);
        searchAutoComplete.setDropDownAnchor(R.id.action_search);
        searchAutoComplete.setThreshold(0);

        mSearchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, autocompleteStrings);

        searchAutoComplete.setAdapter(mSearchAdapter);

        MenuItem nightModeSwitcher = menu.findItem(R.id.menuUseDarkMode);
        nightModeSwitcher.setChecked(mMyViewModel.getNightModeEnabled());

        // обработаю переключатель ODPS
        MenuItem useODPSSwitcher = menu.findItem(R.id.menuUseODPS);
        useODPSSwitcher.setChecked(App.getInstance().isODPS());

        // обработаю переключатель проверки обновлений
        MenuItem checkUpdatesSwitcher = menu.findItem(R.id.setUpdateCheck);
        checkUpdatesSwitcher.setChecked(App.getInstance().isCheckUpdate());

        // обработаю переключатель скрытия прочтённых книг
        MenuItem hideReadSwitcher = menu.findItem(R.id.hideReadedSwitcher);
        hideReadSwitcher.setChecked(App.getInstance().isHideRead());

        // обработаю переключатель быстрой загрузки
        MenuItem downloadAllPages = menu.findItem(R.id.downloadAllData);
        downloadAllPages.setChecked(App.getInstance().isDownloadAll());

        return true;
    }

    private void selectSorting() {
        String[] sortingOptions = null;
        // в зависимости от выбранного режима поиска покажу вырианты сортировки
        switch (App.sSearchType){
            case SEARCH_BOOKS:
            sortingOptions = bookSortOptions;
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
            case R.id.setDownloadsFolder:
                changeDownloadsFolder();
                return true;
            case R.id.action_new:
                showNewDialog();
                return true;
            case R.id.menuUseODPS:
                mMyViewModel.switchODPSMode();
                new Handler().postDelayed(new ResetApp(), 100);
            case R.id.menuUseDarkMode:
                mMyViewModel.switchNightMode();
                new Handler().postDelayed(new ResetApp(), 100);
            case R.id.setUpdateCheck:
                App.getInstance().switchCheckUpdate();
                invalidateOptionsMenu();
                return true;
            case R.id.hideReadedSwitcher:
                App.getInstance().switchHideRead();
                invalidateOptionsMenu();
                return true;
            case R.id.downloadAll:
                downloadAllBooks();
                return true;
            case R.id.downloadAllData:
                App.getInstance().switchDownloadAll();
                invalidateOptionsMenu();
                return true;
            case R.id.action_sort_by:
                selectSorting();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void downloadAllBooks() {
        // если выбран тип загрузки книг и они существуют- предлагаю выбрать тип загрузки
        if (App.sSearchType == SEARCH_BOOKS) {
            ArrayList<FoundedItem> books = App.getInstance().mParsedResult.getValue();
            if (books != null && books.size() > 0) {
                if (mSelectBookTypeDialog == null) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                    dialogBuilder.setTitle("Выберите формат скачивания")
                            //.setMessage("Если возможно, файлы будут скачаны в выбранном формате")
                            .setItems(MimeTypes.MIMES_LIST, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // запущу рабочего, который всё закачает и покажу диалог закачки
                                    doMultiplyDownload(i);
                                }
                            });
                    mSelectBookTypeDialog = dialogBuilder.create();
                }
                // покажу диалог с выбором предпочтительного формата
                mSelectBookTypeDialog.show();
            } else {
                Toast.makeText(this, "Нет книг- нечего качать", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Сначала найдите книги, потом скачаем", Toast.LENGTH_LONG).show();
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
                    .OnPositiveClicked(new GifDialogListener() {
                        @Override
                        public void OnClick() {
                            if (App.getInstance().mProcess != null) {
                                Log.d("surprise", "ODPSActivity OnClick kill process");
                                WorkManager.getInstance().cancelWorkById(App.getInstance().mProcess.getId());
                            }
                            if (mMultiplyDownloadDialog != null) {
                                mMultiplyDownloadDialog.dismiss();
                            }
                            // отменю операцию
                            Toast.makeText(ODPSActivity.this, "Загрузка книг отменена", Toast.LENGTH_LONG).show();

                        }
                    })
                    .build();
        }
        //mMultiplyDownloadDialog.setMessage("Считаю количество книг для скачивания");
        mMultiplyDownloadDialog.show();
    }

    private void doMultiplyDownload(int i) {
        mMyViewModel.downloadMultiply(i);
        showMultiplyDownloadDialog();
        observeBooksDownload();
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
                                doSearch(App.BASE_URL + "/opds/new/0/new");
                                break;
                            case 1:
                                doSearch(App.BASE_URL + "/opds/newgenres");
                                break;
                            case 2:
                                doSearch(App.BASE_URL + "/opds/newauthors");
                                break;
                            case 3:
                                doSearch(App.BASE_URL + "/opds/newsequences");
                                break;
                        }
                    }
                })
                .show();
    }

    private void changeDownloadsFolder() {
        Intent intent = new Intent(this, FolderPicker.class);
        startActivityForResult(intent, READ_REQUEST_CODE);
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

        String searchString = URLEncoder.encode(s, "utf-8");

        // опознаю тип поиска
        if (mSearchRadioContainer.getCheckedRadioButtonId() == R.id.searchBook) {
            doSearch(FLIBUSTA_SEARCH_BOOK_REQUEST + searchString);
        } else {
            doSearch(FLIBUSTA_SEARCH_AUTHOR_REQUEST + searchString);
        }
    }

    private void doSearch(String s) {
        // очищу историю поиска и положу туда начальное значение
        App.getInstance().addToHistory(s);
        showLoadWaitingDialog();
        mWebClient.search(s);
    }

    private void doSearchFromHistory(String s) {
        showLoadWaitingDialog();
        mWebClient.search(s);
    }

    private void showLoadWaitingDialog() {

        if (mShowLoadDialog == null) {

            mShowLoadDialog = new GifDialog.Builder(this)
                    .setTitle(getString(R.string.load_waiting_title))
                    .setMessage(getString(R.string.load_waiting_message))
                    .setGifResource(R.drawable.gif1)   //Pass your Gif here
                    .isCancellable(false)
                    .setPositiveBtnText("Отменить")
                    .OnPositiveClicked(new GifDialogListener() {
                        @Override
                        public void OnClick() {
                            if (App.getInstance().mProcess != null) {
                                Log.d("surprise", "ODPSActivity OnClick kill process");
                                WorkManager.getInstance().cancelWorkById(App.getInstance().mProcess.getId());
                            }
                            hideWaitingDialog();
                            // отменю операцию
                            Toast.makeText(ODPSActivity.this, "Загрузка отменена", Toast.LENGTH_LONG).show();

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
            String searching = App.BASE_URL + url;
            App.getInstance().addToHistory(searching);
            mWebClient.search(searching);
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
                            App.getInstance().restartTor();
                            dialog.dismiss();
                            // вернусь в основное активити и подожду перезапуска
                            startActivityForResult(new Intent(ODPSActivity.this, StartTorActivity.class), START_TOR);
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
        mWebClient = new MyWebClient();
        // если нет истории поиска- гружу новинки за неделю, если есть- последнюю загруженную страницу
        if (App.getInstance().isSearchHistory()) {
            mWebClient.search(App.getInstance().getLastHistoryElement());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
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
                    return super.onKeyDown(keyCode, event);
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
        if (mDownloadsDialog == null) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.downloads_dialog_header);
            mDownloadsDialog = dialogBuilder;
        }
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
        mDownloadsDialog.setItems(linksArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // начну грузить выбранный файл
                // получу сокращённый MIME
                String shortMime = linksArray[i];
                String longMime = MimeTypes.getFullMime(shortMime);
                int counter = 0;
                int linksLength = downloadLinks.size();
                DownloadLink item;
                while (counter < linksLength) {
                    item = downloadLinks.get(counter);
                    if (item.mime.equals(longMime)) {
                        mWebClient.download(item);
                        Toast.makeText(ODPSActivity.this, "Загрузка началась", Toast.LENGTH_LONG).show();
                        break;
                    }
                    counter++;
                }
            }
        });
        mDownloadsDialog.show();
    }


    private class ResetApp implements Runnable {
        @Override
        public void run() {
            Intent intent = new Intent(ODPSActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ODPSActivity.this.startActivity(intent);
            Runtime.getRuntime().exit(0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == START_TOR) {
            // перезагружу страницу
            String lastUrl = App.getInstance().getLastHistoryElement();
            if (lastUrl != null && !lastUrl.isEmpty()) {
                showLoadWaitingDialog();
                mWebClient.search(lastUrl);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
