package net.veldor.flibustaloader.ui;

import android.annotation.SuppressLint;
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
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebView;
import net.veldor.flibustaloader.MyWebViewClient;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.dialogs.ChangelogDialog;
import net.veldor.flibustaloader.dialogs.GifDialog;
import net.veldor.flibustaloader.http.TorWebClient;
import net.veldor.flibustaloader.utils.MyPreferences;
import net.veldor.flibustaloader.utils.XMLHandler;
import net.veldor.flibustaloader.view_models.MainViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static net.veldor.flibustaloader.ui.MainActivity.START_TOR;

public class WebViewActivity extends BaseActivity implements SearchView.OnQueryTextListener{

    public static final String CALLED = "activity_called";
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


        setContentView(R.layout.new_webview_activity);

        setupInterface();

        showChangesList();

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
        checkUpdates();

        // создам тестовый массив строк для автозаполнения
        autocompleteStrings = mMyViewModel.getSearchAutocomplete();
    }

    @Override
    protected void setupInterface() {
        super.setupInterface();

        // скрою переход на данное активити
        Menu menuNav = mNavigationView.getMenu();
        MenuItem item = menuNav.findItem(R.id.goToWebView);
        item.setEnabled(false);
        item.setChecked(true);
    }

    private void showChangesList() {
        // покажу список изменений, если он ещё не показан для этой версии
        if(MyPreferences.getInstance().isShowChanges()){
            new ChangelogDialog.Builder(this).build().show();
            MyPreferences.getInstance().setChangesViewed();
        }
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
                version.removeObservers(WebViewActivity.this);
            });
        }
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
        updateSnackbar.setAction(getString(R.string.snackbar_update_action_message), v -> mMyViewModel.initializeUpdate());
        updateSnackbar.setActionTextColor(getResources().getColor(android.R.color.white));
        updateSnackbar.show();
    }


    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);

        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            //noinspection RestrictedApi
            m.setOptionalIconsVisible(true);
        }

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
        MenuItem menuItem = menu.findItem(R.id.menuUseDarkMode);
        menuItem.setChecked(mMyViewModel.getNightModeEnabled());
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


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == START_TOR) {
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
        changeTitle("Поиск: " + s);
        String searchString = App.SEARCH_URL + s.trim();
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
            String errorDetails = intent.getStringExtra(TorWebClient.ERROR_DETAILS);
            showTorRestartDialog(errorDetails);
        }
    }

    private void showTorRestartDialog(String errorDetails) {
        if(errorDetails == null){
        errorDetails = "";
    }
        if (mTorRestartDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.tor_is_stopped)
                    .setMessage(R.string.tor_restart_dialog_message + errorDetails)
                    .setPositiveButton(R.string.restart_tor_message, (dialog, which) -> {
                        App.getInstance().startTor();
                        dialog.dismiss();
                        // вернусь в основное активити и подожду перезапуска
                        Intent intent = new Intent(WebViewActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    })
                    .setNegativeButton("Ok", null)
                    .setCancelable(true);
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

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
