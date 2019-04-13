package net.veldor.flibustaloader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.utils.XMLHandler;
import net.veldor.flibustaloader.view_models.MainViewModel;

import java.net.URI;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SwipeRefreshLayout.OnRefreshListener {

    private static final int REQUEST_WRITE_READ = 22;
    private static final String FLIBUSTA_SEARCH_REQUEST = "http://flibustahezeous3.onion/booksearch?ask=";
    private MyWebView mWebView;
    private MainViewModel mMyViewModel;
    private LiveData<AndroidOnionProxyManager> mTorClient;
    private SwipeRefreshLayout mRefresher;
    private BookLoadingReceiver mPageLoadReceiver;
    private AlertDialog mBookLoadingDialog;
    private AlertDialog mTorLoadingDialog;
    private View mRootView;
    private SearchView.SearchAutoComplete mSearchAutoComplete;
    private ArrayList<String> autocompleteStrings;
    private SearchView mSearchView;
    private ArrayAdapter<String> mSearchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // инициализирую переменные
        mWebView = findViewById(R.id.myWebView);
        mRootView = findViewById(R.id.rootView);

        // добавлю viewModel
        mMyViewModel = ViewModelProviders.of(this).get(MainViewModel.class);

        // добавлю модный перезагрузчик страницы
        mRefresher = findViewById(R.id.refreshView);
        mRefresher.setOnRefreshListener(this);

        // зарегистрирую получатель команды возвращения на предыдущую страницу
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyWebViewClient.BOOK_LOAD_ACTION);
        mPageLoadReceiver = new BookLoadingReceiver();
        registerReceiver(mPageLoadReceiver, filter);

        if (!permissionGranted()) {
            // показываю диалог с требованием предоставить разрешения
            showPermissionDialog();
        } else {
            handleLoading();
        }

        // проверю обновления
        final LiveData<Boolean> version = mMyViewModel.startCheckUpdate();
        version.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if (aBoolean != null && aBoolean) {
                    // показываю Snackbar с уведомлением
                    makeUpdateSnackbar();
                }
                version.removeObservers(MainActivity.this);
            }
        });

        // создам тестовый массив строк для автозаполнения
        autocompleteStrings = mMyViewModel.getSearchAutocomplete();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setWebViewBackground();
    }

    private void setWebViewBackground() {
        if(mMyViewModel.getNightModeEnabled()){
            mWebView.setBackgroundColor(getResources().getColor(android.R.color.black));
            Log.d("surprise", "MainActivity setWebViewBackground: night mode");
        }
        else{
            mWebView.setBackgroundColor(getResources().getColor(android.R.color.white));
            Log.d("surprise", "MainActivity setWebViewBackground: day mode");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mPageLoadReceiver);
    }
    private void makeUpdateSnackbar() {
        Snackbar updateSnackbar = Snackbar.make(mRootView, getString(R.string.snackbar_found_update_message), Snackbar.LENGTH_INDEFINITE);
        updateSnackbar.setAction(getString(R.string.snackbar_update_action_message), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMyViewModel.initializeUpdate();
            }
        });
        updateSnackbar.show();
    }



    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);

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

        mSearchAutoComplete = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);

        mSearchAutoComplete.setDropDownBackgroundResource(android.R.color.white);
        mSearchAutoComplete.setDropDownAnchor(R.id.action_search);
        mSearchAutoComplete.setThreshold(0);

        mSearchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, autocompleteStrings);

        mSearchAutoComplete.setAdapter(mSearchAdapter);


        // добавлю обработку вида страницы
        MenuItem lightModeSwitcher = menu.findItem(R.id.menuUseLightStyle);
        lightModeSwitcher.setChecked(mMyViewModel.getLightModeEnabled());
        MenuItem nightModeSwitcher = menu.findItem(R.id.menuUseDarkMode);
        nightModeSwitcher.setChecked(mMyViewModel.getNightModeEnabled());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuUseLightStyle:
                mMyViewModel.switchLightMode();
                invalidateOptionsMenu();
                mWebView.reload();
                return true;
            case R.id.goHome:
                mWebView.loadUrl(App.BASE_URL);
                return true;
            case R.id.showNew:
                mWebView.loadUrl(App.NEW_BOOKS);
                return true;
            case R.id.randomBook:
                mWebView.loadUrl(mMyViewModel.getRandomBookUrl());
                return true;
            case R.id.buyCoffee:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=YUGUWUF99QYG4&source=url"));
                startActivity(intent);
                return true;
        }
        if (item.getItemId() == R.id.menuUseLightStyle) {
            mMyViewModel.switchLightMode();
            invalidateOptionsMenu();
            mWebView.reload();
            return true;
        }
        if (item.getItemId() == R.id.menuUseDarkMode) {
            mMyViewModel.switchNightMode();
            invalidateOptionsMenu();
            mWebView.reload();
            setWebViewBackground();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showPermissionDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Необходимо предоставить разрешения")
                .setMessage("Для загрузки книг необходимо предоставить доступ к памяти устройства")
                .setCancelable(false)
                .setPositiveButton("Предоставить разрешение", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_WRITE_READ);
                    }
                })
                .setNegativeButton("Нет, закрыть приложение", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        dialogBuilder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_READ) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog();
            } else {
                // полностью перезапущу приложение
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                this.startActivity(intent);
                Runtime.getRuntime().exit(0);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean permissionGranted() {
        int writeResult;
        int readResult;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            writeResult = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            readResult = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            writeResult = PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            readResult = PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        return writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        if (!TextUtils.isEmpty(s.trim())) {
            // ищу введённое значение
            makeSearch(s);
        }
        return true;
    }

    private void makeSearch(String s) {
        String searchString = FLIBUSTA_SEARCH_REQUEST + s.trim();
        mWebView.loadUrl(searchString);
        // занесу значение в список автозаполнения
        if(XMLHandler.putSearchValue(s)){
            // обновлю список поиска
            autocompleteStrings = mMyViewModel.getSearchAutocomplete();
            mSearchAdapter.clear();
            mSearchAdapter.addAll(autocompleteStrings);
            mSearchAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onQueryTextChange(String s) {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    // возвращаюсь на страницу назад в браузере
                    assert mWebView != null;
                    if (mWebView.canGoBack()) {
                        mWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
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
                        mTorClient.removeObservers(MainActivity.this);
                    }
                }
            });
        } else {
            // запускаю браузер
            startBrowsing();
        }
    }

    private void startBrowsing() {
        hideTorLoadingDialog();
        mWebView.setup();
        if (App.getInstance().currentLoadedUrl == null) {
            mWebView.loadUrl(App.BASE_URL);
        }
    }

    @Override
    public void onRefresh() {
        mWebView.reload();
        mRefresher.setRefreshing(false);
    }

    public class BookLoadingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int action = intent.getIntExtra(MyWebViewClient.BOOK_LOAD_EVENT, 0);
            switch (action) {
                case MyWebViewClient.START_BOOK_LOADING:
                    showBookLoadingDialog();
                    break;
                case MyWebViewClient.FINISH_BOOK_LOADING:
                    mWebView.loadUrl(App.getInstance().currentLoadedUrl);
                default:
                    hideBookLoadingDialog();
            }
        }
    }

    private void showBookLoadingDialog() {
        if (mBookLoadingDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.book_loading_dialog_title)
                    .setView(R.layout.book_loading_dialog_layout)
                    .setCancelable(false);
            mBookLoadingDialog = dialogBuilder.create();
        }
        mBookLoadingDialog.show();
    }

    private void hideBookLoadingDialog() {
        if (mBookLoadingDialog != null) {
            mBookLoadingDialog.hide();
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

    private void hideTorLoadingDialog() {
        if (mTorLoadingDialog != null) {
            mTorLoadingDialog.hide();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d("surprise", "MainActivity onLowMemory: oops, low memory... Save me...");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
        Log.d("surprise", "MainActivity onSaveInstanceState: state saved");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
        Log.d("surprise", "MainActivity onRestoreInstanceState: state restored");
    }
}