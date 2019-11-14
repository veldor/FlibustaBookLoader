package net.veldor.flibustaloader;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import android.widget.Toast;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.adapters.SearchResultsAdapter;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.utils.XMLHandler;
import net.veldor.flibustaloader.utils.XMLParser;
import net.veldor.flibustaloader.view_models.MainViewModel;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class ODPSActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private static final String FLIBUSTA_SEARCH_BOOK_REQUEST = "http://flibustahezeous3.onion/opds/search?searchType=books&searchTerm=";
    private static final String FLIBUSTA_SEARCH_AUTHOR_REQUEST = "http://flibustahezeous3.onion/opds/search?searchType=authors&searchTerm=";
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
    private Button mBackButton, mForwardButton;
    private ArrayList<String> mSearchHistory;
    private AlertDialog.Builder mDownloadsDialog;
    private AlertDialog mBookLoadingDialog;
    private TorConnectErrorReceiver mTtorConnectErrorReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_odps);

        // определю кнопки прыжков по результатам
        mBackButton = findViewById(R.id.goBackSearchButton);
        mForwardButton = findViewById(R.id.goForwardSearchButton);

        // добавлю viewModel
        mMyViewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        // зарегистрирую получатель ошибки подключения к TOR
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyWebViewClient.TOR_CONNECT_ERROR_ACTION);
        mTtorConnectErrorReceiver = new TorConnectErrorReceiver();
        registerReceiver(mTtorConnectErrorReceiver, filter);
        handleLoading();

        // создам адаптер результатов поиска
        mSearchResultsAdapter = new SearchResultsAdapter();
        RecyclerView recycler = findViewById(R.id.searched_items_list);
        recycler.setAdapter(mSearchResultsAdapter);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // создам тестовый массив строк для автозаполнения
        autocompleteStrings = mMyViewModel.getSearchAutocomplete();

        // создам массив для истории поиска
        mSearchHistory = new ArrayList<>();
        // при нажатии кнопки назад- убираем последний элемент истории и переходим на предпоследний
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchHistory.remove(mSearchHistory.size() - 1);
                String link = mSearchHistory.get(mSearchHistory.size() - 1);
                showLoadWaitingDialog();
                mWebClient.search(link);
            }
        });

        // добавлю ссылку на тип поиска
        mSearchRadioContainer = findViewById(R.id.searchType);

        // добавлю отслеживание получения результатов поиска
        LiveData<String> searchResultText = App.getInstance().mSearchResult;
        searchResultText.observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                hideWaitingDialog();
                if (s != null && !s.isEmpty()) {
                    Log.d("surprise", "ODPSActivity onChanged: " + mSearchHistory.size());
                    mSearchResultsAdapter.clear();
                    // получен ответ сервера, разберу его и положу содержимое в список
                    XMLParser xmlParser = new XMLParser(s);
                    HashMap<String, ArrayList> searchResults = xmlParser.getSearchResults();
                    if (searchResults != null) {
                        // проверю, возможен ли переход к следующим результатам поиска
                        String nextPage = xmlParser.getNextPage();
                        handleBackward();
                        if (nextPage != null) {
                            handleForward(nextPage);
                        } else {
                            stopForward();
                        }
                        // добавлю найденное в список
                        // если найден список авторов
                        ArrayList authors = searchResults.get("authors");
                        ArrayList books = searchResults.get("books");
                        if (authors != null) {
                            mSearchResultsAdapter.setAuthorsList(authors);
                            mSearchResultsAdapter.notifyDataSetChanged();
                        } else if (books != null) {
                            mSearchResultsAdapter.setBooksList(books);
                            mSearchResultsAdapter.notifyDataSetChanged();
                        } else {
                            nothingFound();
                        }
                    } else {
                        nothingFound();
                    }
                }
            }
        });

        // добавлю отслеживание получения списка ссылок на скачивание
        LiveData<ArrayList<DownloadLink>> downloadLinks = App.getInstance().mDownloadLinksList;
        downloadLinks.observe(this, new Observer<ArrayList<DownloadLink>>() {
            @Override
            public void onChanged(@Nullable ArrayList<DownloadLink> downloadLinks) {
                if(downloadLinks != null && downloadLinks.size() > 0){
                    if(downloadLinks.size() == 1){
                        mWebClient.download(downloadLinks.get(0));
                    }
                    else{
                        // покажу диалог для выбора ссылки для скачивания
                        showDownloadsDialog(downloadLinks);
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mTtorConnectErrorReceiver != null){
            unregisterReceiver(mTtorConnectErrorReceiver);
        }
    }

    private void handleBackward() {
        // обработаю возвращение назад
        if (mSearchHistory.size() > 1) {
            mBackButton.setEnabled(true);
        } else {
            mBackButton.setEnabled(false);
        }
    }

    private void stopForward() {
        mForwardButton.setEnabled(false);
        Toast.makeText(this, getString(R.string.no_results_more), Toast.LENGTH_LONG).show();
    }

    private void handleForward(final String nextPage) {
        // переход на следующую страницу
        mForwardButton.setEnabled(true);
        mForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadWaitingDialog();
                mWebClient.search(App.BASE_URL + nextPage);
                mSearchHistory.add(App.BASE_URL + nextPage);
            }
        });
    }

    private void nothingFound() {
        mSearchResultsAdapter.nothingFound();
        mSearchResultsAdapter.notifyDataSetChanged();
        mBackButton.setEnabled(false);
        mForwardButton.setEnabled(false);
        // очищу историю поиска
        mSearchHistory.clear();
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

        searchAutoComplete.setDropDownBackgroundResource(android.R.color.white);
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
        mSearchHistory.add(s);
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
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // если доступен возврат назад- возвращаюсь, если нет- закрываю приложение
            if(mBackButton.isEnabled()){
                mBackButton.performClick();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    private void showDownloadsDialog(final ArrayList<DownloadLink> downloadLinks) {
        if(mDownloadsDialog == null){
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.downloads_dialog_header);
            mDownloadsDialog = dialogBuilder;
        }
        // получу список типов данных
        int linksLength = downloadLinks.size();
        final String[] linksArray = new String[linksLength];
        int counter = 0;
        String mime;
        while (counter < linksLength){
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
                while (counter < linksLength){
                     item = downloadLinks.get(counter);
                    if(item.mime.equals(longMime)){
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
}
