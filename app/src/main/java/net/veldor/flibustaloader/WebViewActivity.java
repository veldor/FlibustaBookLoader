package net.veldor.flibustaloader;

import android.annotation.SuppressLint;
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import net.veldor.flibustaloader.dialogs.GifDialog;
import net.veldor.flibustaloader.utils.XMLHandler;
import net.veldor.flibustaloader.view_models.MainViewModel;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import lib.folderpicker.FolderPicker;

import static net.veldor.flibustaloader.MainActivity.START_TOR;

public class WebViewActivity extends AppCompatActivity implements SearchView.OnQueryTextListener{

    public static final String CALLED = "activity_called";
    private static final String FLIBUSTA_SEARCH_REQUEST = "http://flibustahezeous3.onion/booksearch?ask=";
    private static final int READ_REQUEST_CODE = 5;
    private static final int REQUEST_CODE = 7;
    private MyWebView mWebView;
    private MainViewModel mMyViewModel;
    private WebViewActivity.BookLoadingReceiver mPageLoadReceiver;
    private View mRootView;
    private ArrayList<String> autocompleteStrings;
    private SearchView mSearchView;
    private ArrayAdapter<String> mSearchAdapter;
    private AlertDialog mTorRestartDialog;
    private TorConnectErrorReceiver mTorConnectErrorReceiver;
    private long mConfirmExit;
    private Dialog mShowLoadDialog;
    private SearchView.SearchAutoComplete mSearchAutocomplete;
    private boolean mIsActivityCalled;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (App.getInstance().getNightMode()) {
            setTheme(R.style.NightTheme);
        }
        setContentView(R.layout.activity_main);

        // проверю, не запущено ли приложение с помощью интента
        if (getIntent().getData() != null) {//check if intent is not null
            Uri data = getIntent().getData();//set a variable for the Intent
            String fullPath = data.getEncodedPath();
            App.getInstance().setLastLoadedUrl(App.BASE_URL + fullPath);
        }
        // проверю, не вызвана ли активность
        mIsActivityCalled = getIntent().getBooleanExtra(CALLED, false);
        if (mIsActivityCalled) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
        // инициализирую переменные
        mWebView = findViewById(R.id.myWebView);
        mRootView = findViewById(R.id.rootView);

        // добавлю viewModel
        mMyViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        Log.d("surprise", "WebViewActivity onCreate " + mMyViewModel);

        // зарегистрирую получатель команды возвращения на предыдущую страницу
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyWebViewClient.BOOK_LOAD_ACTION);
        mPageLoadReceiver = new WebViewActivity.BookLoadingReceiver();
        registerReceiver(mPageLoadReceiver, filter);

        // зарегистрирую получатель ошибки подключения к TOR
        filter = new IntentFilter();
        filter.addAction(MyWebViewClient.TOR_CONNECT_ERROR_ACTION);
        mTorConnectErrorReceiver = new WebViewActivity.TorConnectErrorReceiver();
        registerReceiver(mTorConnectErrorReceiver, filter);
        handleLoading();

        if (App.getInstance().isCheckUpdate()) {
            // проверю обновления
            final LiveData<Boolean> version = mMyViewModel.startCheckUpdate();
            version.observe(this, new Observer<Boolean>() {
                @Override
                public void onChanged(@Nullable Boolean aBoolean) {
                    if (aBoolean != null && aBoolean) {
                        // показываю Snackbar с уведомлением
                        // TODO: 17.02.2020 включить в стабильной версии 
                        //makeUpdateSnackbar();
                    }
                    version.removeObservers(WebViewActivity.this);
                }
            });
        }

        // создам тестовый массив строк для автозаполнения
        autocompleteStrings = mMyViewModel.getSearchAutocomplete();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setWebViewBackground();
    }

    private void setWebViewBackground() {
        if (mMyViewModel.getNightModeEnabled()) {
            mWebView.setBackgroundColor(getResources().getColor(android.R.color.black));
        } else {
            mWebView.setBackgroundColor(getResources().getColor(android.R.color.white));
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPageLoadReceiver != null)
            unregisterReceiver(mPageLoadReceiver);
        if (mTorConnectErrorReceiver != null)
            unregisterReceiver(mTorConnectErrorReceiver);
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

        mSearchAutocomplete = mSearchView.findViewById(R.id.search_src_text);

        mSearchAutocomplete.setDropDownBackgroundResource(android.R.color.white);
        mSearchAutocomplete.setDropDownAnchor(R.id.action_search);
        mSearchAutocomplete.setThreshold(0);

        mSearchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, autocompleteStrings);

        mSearchAutocomplete.setAdapter(mSearchAdapter);

        // добавлю обработку вида страницы
        // определю, какой пункт вида выбран
        switch (mMyViewModel.getViewMode()) {
            case App.VIEW_MODE_NORMAL:
                menu.findItem(R.id.menuUseNormalStyle).setChecked(true);
                break;
            case App.VIEW_MODE_LIGHT:
                menu.findItem(R.id.menuUseLightStyle).setChecked(true);
                break;
            case App.VIEW_MODE_FAST:
                menu.findItem(R.id.menuUseLightFastStyle).setChecked(true);
                break;
            case App.VIEW_MODE_FAT:
                menu.findItem(R.id.menuUseLightFatStyle).setChecked(true);
                break;
            case App.VIEW_MODE_FAST_FAT:
                menu.findItem(R.id.menuUseFatFastStyle).setChecked(true);
                break;
        }
        MenuItem nightModeSwitcher = menu.findItem(R.id.menuUseDarkMode);
        nightModeSwitcher.setChecked(mMyViewModel.getNightModeEnabled());

        // обработаю переключатель проверки обновлений
        MenuItem checkUpdatesSwitcher = menu.findItem(R.id.setUpdateCheck);
        checkUpdatesSwitcher.setChecked(App.getInstance().isCheckUpdate());

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuUseLightStyle:
            case R.id.menuUseLightFastStyle:
            case R.id.menuUseLightFatStyle:
            case R.id.menuUseNormalStyle:
            case R.id.menuUseFatFastStyle:
                mMyViewModel.switchViewMode(item.getItemId());
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
            case R.id.shareLink:
                mMyViewModel.shareLink(mWebView);
                return true;
            case R.id.buyCoffee:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=YUGUWUF99QYG4&source=url"));
                startActivity(intent);
                return true;
            case R.id.goToTest:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://t.me/flibusta_downloader_beta"));
                startActivity(intent);
                return true;
            case R.id.setDownloadsFolder:
                changeDownloadsFolder();
                return true;
            case R.id.switchToODPS:
                switchToODPS();
            case R.id.setUpdateCheck:
                App.getInstance().switchCheckUpdate();
                return true;
            case R.id.subscribeForNew:
                subscribe();
                return true;
            case R.id.clearSearchHistory:
                clearHistory();
                return true;
        }
        if (item.getItemId() == R.id.menuUseDarkMode) {
            mMyViewModel.switchNightMode();
            new Handler().postDelayed(new WebViewActivity.ResetApp(), 100);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void clearHistory() {
        mMyViewModel.clearHistory();
        autocompleteStrings = new ArrayList<>();
        mSearchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, autocompleteStrings);
        mSearchAutocomplete.setAdapter(mSearchAdapter);
        Toast.makeText(this, "Автозаполнение сброшено", Toast.LENGTH_SHORT).show();
    }

    private void subscribe() {
        startActivity(new Intent(this, SubscribeActivity.class));
    }

    private void switchToODPS() {
        App.getInstance().setView(App.VIEW_ODPS);
        startActivity(new Intent(this, OPDSActivity.class));
        finish();
    }

    private void changeDownloadsFolder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_CODE);
        } else {
            Intent intent = new Intent(this, FolderPicker.class);
            startActivityForResult(intent, READ_REQUEST_CODE);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
                if (data != null) {
                    String folderLocation = Objects.requireNonNull(data.getExtras()).getString("data");
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
            mWebView.setup();
            mWebView.loadUrl(App.getInstance().getLastLoadedUrl());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
        if (XMLHandler.putSearchValue(s)) {
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
            if (keyCode == KeyEvent.KEYCODE_BACK) {// возвращаюсь на страницу назад в браузере
                assert mWebView != null;
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                } else {
                    if (mIsActivityCalled) {
                        finish();
                        return true;
                    }
                    if (mConfirmExit != 0) {
                        if (mConfirmExit > System.currentTimeMillis() - 3000) {
                            // выйду из приложения
                            Log.d("surprise", "OPDSActivity onKeyDown exit");
                            this.finishAffinity();
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
                return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    private void handleLoading() {
        startBrowsing();
    }

    private void startBrowsing() {
        mWebView.setup();
        mWebView.loadUrl(App.getInstance().getLastLoadedUrl());
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
                    mWebView.loadUrl(App.getInstance().getLastLoadedUrl());
                default:
                    hideBookLoadingDialog();
            }
        }
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
                            App.getInstance().restartTor();
                            dialog.dismiss();
                            // вернусь в основное активити и подожду перезапуска
                            startActivityForResult(new Intent(WebViewActivity.this, StartTorActivity.class), START_TOR);
                        }
                    })
                    .setCancelable(false);
            mTorRestartDialog = dialogBuilder.create();
        }
        mTorRestartDialog.show();
    }

    private void showBookLoadingDialog() {
        if (mShowLoadDialog == null) {

            mShowLoadDialog = new GifDialog.Builder(this)
                    .setTitle(getString(R.string.load_waiting_title))
                    .setMessage(getString(R.string.load_waiting_message))
                    .setGifResource(R.drawable.gif1)   //Pass your Gif here
                    .isCancellable(false)
                    .build();
        }
        mShowLoadDialog.show();
    }

    private void hideBookLoadingDialog() {
        if (mShowLoadDialog != null) {
            mShowLoadDialog.hide();
        }
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d("surprise", "MainActivity onLowMemory: oops, low memory... Save me...");
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
    }

    private class ResetApp implements Runnable {
        @Override
        public void run() {
            Intent intent = new Intent(WebViewActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            WebViewActivity.this.startActivity(intent);
            Runtime.getRuntime().exit(0);
        }
    }
}
