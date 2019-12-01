package net.veldor.flibustaloader;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
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

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.adapters.SearchResultsAdapter;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.utils.XMLHandler;
import net.veldor.flibustaloader.utils.XMLParser;
import net.veldor.flibustaloader.view_models.MainViewModel;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import lib.folderpicker.FolderPicker;

public class ODPSActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private static final String FLIBUSTA_SEARCH_BOOK_REQUEST = "http://flibustahezeous3.onion/opds/search?searchType=books&searchTerm=";
    private static final String FLIBUSTA_SEARCH_AUTHOR_REQUEST = "http://flibustahezeous3.onion/opds/search?searchType=authors&searchTerm=";
    private static final int READ_REQUEST_CODE = 5;
    private AlertDialog mTorRestartDialog;
    private AlertDialog mTorLoadingDialog;
    private MainViewModel mMyViewModel;
    private LiveData<AndroidOnionProxyManager> mTorClient;
    private SearchView mSearchView;
    private ArrayList<String> autocompleteStrings;
    private ArrayAdapter<String> mSearchAdapter;
    private RadioGroup mSearchRadioContainer;
    private MyWebClient mWebClient;
    private SearchResultsAdapter mSearchResultsAdapter;
    private AlertDialog mShowLoadDialog;
    private Button mLoadMoreBtn;
    private ArrayList<String> mSearchHistory;
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
    private boolean mResultsEscalate = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_odps);

        mSearchTitle = findViewById(R.id.search_title);

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

        // создам массив для истории поиска
        mSearchHistory = App.getInstance().mSearchHistory;
        // при нажатии кнопки назад- убираем последний элемент истории и переходим на предпоследний
        mLoadMoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mSearchHistory.size() > 0){
                    mSearchHistory.remove(mSearchHistory.size() - 1);
                    String link = mSearchHistory.get(mSearchHistory.size() - 1);
                    if (mSearchHistory.size() > 1)
                        mSearchHistory.remove(mSearchHistory.size() - 1);
                    showLoadWaitingDialog();
                    mWebClient.search(link);
                }
            }
        });

        // добавлю ссылку на тип поиска
        mSearchRadioContainer = findViewById(R.id.searchType);
        // добавлю отслеживание изменения ваианта поиска
        mSearchRadioContainer.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // если выбран автор или книга- покажу окно поиска, если жанр- скрою окно поиска и поищу жанры
                if (mSearchView != null) {
                    switch (checkedId) {
                        case R.id.searchBook:
                            App.sSearchType = SEARCH_BOOKS;
                            mSearchView.setVisibility(View.VISIBLE);
                            break;
                        case R.id.searchAuthor:
                            App.sSearchType = SEARCH_AUTHORS;
                            mSearchView.setVisibility(View.VISIBLE);
                            break;
                        case R.id.searchGenre:
                            App.sSearchType = SEARCH_GENRE;
                            mSearchView.setVisibility(View.GONE);
                            showLoadWaitingDialog();
                            mWebClient.search("http://flibustahezeous3.onion/opds/genres");
                            break;
                        case R.id.searchSequence:
                            App.sSearchType = SEARCH_SEQUENCE;
                            mSearchView.setVisibility(View.GONE);
                            showLoadWaitingDialog();
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

        // добавлю отслеживание получения результатов поиска
        LiveData<String> searchResultText = App.getInstance().mSearchResult;
        searchResultText.observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                hideWaitingDialog();
                if (s != null && !s.isEmpty()) {
                    App.getInstance().mResponce = s;
                    // получен ответ сервера, разберу его и положу содержимое в список
                        XMLParser.handleResults();
                        // проверю, возможен ли переход к следующим результатам поиска
                       /* String nextPage = xmlParser.getNextPage();
                        if (nextPage != null) {
                            handleForward(nextPage);
                        }
                        // добавлю найденное в список
                        // если найден список авторов
                        ArrayList authors = searchResults.get("authors");
                        ArrayList books = searchResults.get("books");
                        ArrayList sequences = searchResults.get("sequences");
                        ArrayList genres = searchResults.get("genres");
                        if (authors != null) {
                            // создам адаптер результатов поиска
                            mSearchResultsAdapter = new SearchResultsAdapter();
                            mRecycler.setAdapter(mSearchResultsAdapter);
                            mRecycler.setLayoutManager(new LinearLayoutManager(ODPSActivity.this));
                            mSearchResultsAdapter.setAuthorsList(authors);
                            mSearchResultsAdapter.notifyDataSetChanged();
                        } else if (books != null) {
                            // создам адаптер результатов поиска
                            mSearchResultsAdapter = new SearchResultsAdapter();
                            mRecycler.setAdapter(mSearchResultsAdapter);
                            mRecycler.setLayoutManager(new LinearLayoutManager(ODPSActivity.this));
                            mSearchResultsAdapter.setBooksList(books);
                            mSearchResultsAdapter.notifyDataSetChanged();
                        } else if (sequences != null) {
                            // создам адаптер результатов поиска
                            mSearchResultsAdapter = new SearchResultsAdapter();
                            mRecycler.setAdapter(mSearchResultsAdapter);
                            mRecycler.setLayoutManager(new LinearLayoutManager(ODPSActivity.this));
                            mSearchResultsAdapter.setSequencesList(sequences);
                            mSearchResultsAdapter.notifyDataSetChanged();
                        } else if (genres != null) {
                            // создам адаптер результатов поиска
                            mSearchResultsAdapter = new SearchResultsAdapter();
                            mRecycler.setAdapter(mSearchResultsAdapter);
                            mRecycler.setLayoutManager(new LinearLayoutManager(ODPSActivity.this));
                            mSearchResultsAdapter.setGenresList(genres);
                            mSearchResultsAdapter.notifyDataSetChanged();
                        } else {
                            nothingFound();
                        }
                        Log.d("surprise", "ODPSActivity onChanged page viewed");*/
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
                    showAuthorViewSelect(author);
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
                    mWebClient.search(App.BASE_URL + foundedSequence.link);
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
                    mWebClient.search(App.BASE_URL + foundedGenre.term);
                }
            }
        });

        // добавлю обсерверы
        addObservers();
    }

    private void addObservers() {
        LiveData<ArrayList> handledResults = App.getInstance().mParsedResult;
        handledResults.observe(this, new Observer<ArrayList>() {
            @Override
            public void onChanged(@Nullable ArrayList arrayList) {
                if(arrayList != null){
                    // если была дополнительная загрузка данных и есть адаптер- догружаю в него данные. Иначе- добавляю адаптер
                    if(mResultsEscalate){

                    }
                    else{
                        mSearchResultsAdapter = new SearchResultsAdapter(arrayList);
                        mRecycler.setAdapter(mSearchResultsAdapter);
                        mRecycler.setLayoutManager(new LinearLayoutManager(ODPSActivity.this));
                    }
                }
                else{
                    nothingFound();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void showSequenceSelectDialog(final ArrayList<FoundedSequence> sequences) {
        if (mSelectSequencesDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.select_authors_choose_message);
            mSelectSequencesDialog = dialogBuilder;
        }
        // получу сисок имён авторов
        Iterator<FoundedSequence> iterator = sequences.iterator();
        ArrayList<String> sequencesList = new ArrayList<>();
        for (; iterator.hasNext(); ) {
            FoundedSequence s = iterator.next();
            sequencesList.add(s.title);

        }
        // покажу список выбора автора
        mSelectSequencesDialog.setItems(sequencesList.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d("surprise", "ODPSActivity onClick selected sequence");
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
    }



    private void handleForward(final String nextPage) {
        // переход на следующую страницу
        mLoadMoreBtn.setVisibility(View.VISIBLE);
        mLoadMoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadWaitingDialog();
                mWebClient.search(App.BASE_URL + nextPage);
                mResultsEscalate = true;
            }
        });
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
            mShowLoadDialog.hide();
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
        return true;
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
            case R.id.menuUseODPS:
                mMyViewModel.switchODPSMode();
                new Handler().postDelayed(new ResetApp(), 100);
            case R.id.menuUseDarkMode:
                mMyViewModel.switchNightMode();
                new Handler().postDelayed(new ResetApp(), 100);
        }
        return super.onOptionsItemSelected(item);
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
        mSearchHistory.clear();
        showLoadWaitingDialog();
        mWebClient.search(s);
    }

    private void showLoadWaitingDialog() {
        if (mShowLoadDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.load_message)
                    .setMessage(R.string.waitForLoad)
                    .setCancelable(false);
            mShowLoadDialog = dialogBuilder.create();
        }
        mShowLoadDialog.show();
    }


    private void showAuthorViewSelect(final Author author) {
        // получу идентификатор автора
        if (mSelectAuthorViewDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.select_author_view_message)
                    .setItems(mAuthorViewTypes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loadAuthor(which, author.uri);
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
                showAuthorViewSelect(authors.get(i));
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
            mWebClient.search(App.BASE_URL + url);
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
        if (mTorRestartDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.tor_is_stopped)
                    .setMessage(R.string.tor_restart_dialog_message)
                    .setPositiveButton(R.string.restart_tor_message, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            hideTorRestartDialog();
                            App.getInstance().restartTor();
                            showTorLoadingDialog();
                        }
                    })
                    .setCancelable(false);
            mTorRestartDialog = dialogBuilder.create();
        }
        mTorRestartDialog.show();
    }


    private void hideTorRestartDialog() {
        if (mTorRestartDialog != null) {
            mTorRestartDialog.hide();
        }
    }

    private void hideTorLoadingDialog() {
        if (mTorLoadingDialog != null) {
            mTorLoadingDialog.hide();
        }
    }

    private void showTorLoadingDialog() {
        if (mTorLoadingDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.tor_loading_dialog_title)
                    .setView(R.layout.tor_loading_dialog_layout)
                    .setCancelable(false);
            mTorLoadingDialog = dialogBuilder.create();
        }
        mTorLoadingDialog.show();
    }

    private void handleLoading() {
        if (mTorClient == null) {
            showTorLoadingDialog();
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
            Log.d("surprise", "ODPSActivity handleLoading: just start browsing");
            // запускаю браузер
            startBrowsing();
        }
    }

    private void startBrowsing() {
        hideTorLoadingDialog();
        mWebClient = new MyWebClient();
        // если нет истории поиска- гружу новинки за неделю, если есть- последнюю загруженную страницу
        if (mSearchHistory.size() > 0) {
            mWebClient.search(mSearchHistory.get(mSearchHistory.size() - 1));
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // если доступен возврат назад- возвращаюсь, если нет- закрываю приложение
            if (mLoadMoreBtn.isEnabled()) {
                mLoadMoreBtn.performClick();
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
}
