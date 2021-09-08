package net.veldor.flibustaloader.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.SearchAutoComplete
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.MyWebView
import net.veldor.flibustaloader.MyWebViewClient
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.dialogs.ChangelogDialog
import net.veldor.flibustaloader.dialogs.GifDialog
import net.veldor.flibustaloader.http.GlobalWebClient
import net.veldor.flibustaloader.http.TorWebClient
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper.getBaseOPDSUrl
import net.veldor.flibustaloader.utils.XMLHandler.putSearchValue
import net.veldor.flibustaloader.view_models.MainViewModel
import java.util.*

class WebViewActivity : BaseActivity(), SearchView.OnQueryTextListener {
    private var mWebView: MyWebView? = null
    private var mMyViewModel: MainViewModel? = null
    private var mPageLoadReceiver: BookLoadingReceiver? = null
    private var mRootView: View? = null
    private var autocompleteStrings: ArrayList<String>? = null
    private var mSearchView: SearchView? = null
    private var mSearchAdapter: ArrayAdapter<String>? = null
    private var mTorRestartDialog: AlertDialog? = null
    private var mTorConnectErrorReceiver: TorConnectErrorReceiver? = null
    private var mConfirmExit: Long = 0
    private var mShowLoadDialog: Dialog? = null
    private var mSearchAutocomplete: SearchAutoComplete? = null
    private var mIsActivityCalled = false
    private var mDisconnectedSnackbar: Snackbar? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // проверю куку
        if (PreferencesHandler.instance.isEInk) {
            setContentView(R.layout.new_eink_webview_activity)
        } else {
            setContentView(R.layout.new_webview_activity)
        }
        setupInterface()
        showChangesList()

        // проверю, не запущено ли приложение с помощью интента
        if (intent.data != null) { //check if intent is not null
            val data = intent.data //set a variable for the Intent
            val fullPath = data.encodedPath
            PreferencesHandler.instance.lastLoadedUrl = PreferencesHandler.BASE_URL + fullPath
        }
        // проверю, не вызвана ли активность
        mIsActivityCalled = intent.getBooleanExtra(CALLED, false)
        if (mIsActivityCalled) {
            val actionBar = supportActionBar
            actionBar?.setDisplayHomeAsUpEnabled(true)
        }
        // инициализирую переменные
        mWebView = findViewById(R.id.myWebView)
        mRootView = findViewById(R.id.rootView)

        // добавлю viewModel
        mMyViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        // зарегистрирую получатель команды возвращения на предыдущую страницу
        var filter = IntentFilter()
        filter.addAction(MyWebViewClient.BOOK_LOAD_ACTION)
        mPageLoadReceiver = BookLoadingReceiver()
        registerReceiver(mPageLoadReceiver, filter)

        // зарегистрирую получатель ошибки подключения к TOR
        filter = IntentFilter()
        filter.addAction(MyWebViewClient.TOR_CONNECT_ERROR_ACTION)
        mTorConnectErrorReceiver = TorConnectErrorReceiver()
        registerReceiver(mTorConnectErrorReceiver, filter)
        handleLoading()
        checkUpdates()

        // создам тестовый массив строк для автозаполнения
        autocompleteStrings = mMyViewModel!!.searchAutocomplete
    }

    override fun setupInterface() {
        super.setupInterface()

        // скрою переход на данное активити
        val menuNav = mNavigationView!!.menu
        val item = menuNav.findItem(R.id.goToWebView)
        item.isEnabled = false
        item.isChecked = true

        // буду отслеживать событие логина
        val cookieObserver: LiveData<Boolean> = App.sResetLoginCookie
        cookieObserver.observe(this, { aBoolean: Boolean ->
            if (aBoolean) {
                Toast.makeText(
                    this@WebViewActivity,
                    "Данные для входа устарели, придётся войти ещё раз",
                    Toast.LENGTH_SHORT
                ).show()
                invalidateOptionsMenu()
            }
        })

        // добавлю отслеживание статуса сети
        val connectionState: LiveData<Int?> = GlobalWebClient.mConnectionState
        connectionState.observe(this, { state: Int? ->
            if (state == GlobalWebClient.CONNECTED) {
                // соединение установлено.
                // провожу действие только если до этого было заявлено о потере соединения
                if (mDisconnectedSnackbar != null && mDisconnectedSnackbar!!.isShown) {
                    Toast.makeText(this@WebViewActivity, "Connected", Toast.LENGTH_LONG).show()
                    mDisconnectedSnackbar!!.dismiss()
                }
            } else if (state == GlobalWebClient.DISCONNECTED) {
                showDisconnectedStateSnackbar()
            }
        })
    }

    private fun showDisconnectedStateSnackbar() {
        if (mDisconnectedSnackbar == null) {
            mDisconnectedSnackbar = Snackbar.make(
                mRootView!!,
                getString(R.string.connection_lost_message),
                Snackbar.LENGTH_INDEFINITE
            )
            mDisconnectedSnackbar!!.setAction(getString(R.string.reload_page_message)) {
                mDisconnectedSnackbar!!.dismiss()
                mWebView!!.reload()
            }
        }
        mDisconnectedSnackbar!!.show()
        hideBookLoadingDialog()
    }

    private fun showChangesList() {
        // покажу список изменений, если он ещё не показан для этой версии
        if (PreferencesHandler.instance.isShowChanges()) {
            ChangelogDialog.Builder(this).build().show()
            PreferencesHandler.instance.setChangesViewed()
        }
    }

    private fun checkUpdates() {
        if (PreferencesHandler.instance.isCheckUpdate) {
            // проверю обновления
            val version = mMyViewModel!!.startCheckUpdate()
            version.observe(this, { aBoolean: Boolean? ->
                if (aBoolean != null && aBoolean) {
                    // показываю Snackbar с уведомлением
                    makeUpdateSnackbar()
                }
                version.removeObservers(this@WebViewActivity)
            })
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        setWebViewBackground()
    }

    private fun setWebViewBackground() {
        if (mMyViewModel!!.nightModeEnabled) {
            mWebView!!.setBackgroundColor(resources.getColor(android.R.color.black))
        } else {
            mWebView!!.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mPageLoadReceiver != null) unregisterReceiver(mPageLoadReceiver)
        if (mTorConnectErrorReceiver != null) unregisterReceiver(mTorConnectErrorReceiver)
    }

    private fun makeUpdateSnackbar() {
        val updateSnackbar = Snackbar.make(
            mRootView!!,
            getString(R.string.snackbar_found_update_message),
            Snackbar.LENGTH_INDEFINITE
        )
        updateSnackbar.setAction(getString(R.string.snackbar_update_action_message)) { v: View? -> mMyViewModel!!.initializeUpdate() }
        updateSnackbar.setActionTextColor(resources.getColor(android.R.color.white))
        updateSnackbar.show()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        // добавлю обработку поиска
        val searchMenuItem = menu.findItem(R.id.action_search)
        mSearchView = searchMenuItem.actionView as SearchView
        mSearchView!!.inputType = InputType.TYPE_CLASS_TEXT
        mSearchView!!.setOnQueryTextListener(this)
        mSearchView!!.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(i: Int): Boolean {
                return true
            }

            override fun onSuggestionClick(i: Int): Boolean {
                val value = autocompleteStrings!![i]
                mSearchView!!.setQuery(value, true)
                return true
            }
        })
        mSearchAutocomplete = mSearchView!!.findViewById(R.id.search_src_text)
        if (PreferencesHandler.instance.isEInk) {
            mSearchAutocomplete!!.setTextColor(Color.WHITE)
        }
        mSearchAutocomplete!!.setDropDownBackgroundResource(android.R.color.white)
        mSearchAutocomplete!!.setDropDownAnchor(R.id.action_search)
        mSearchAutocomplete!!.setThreshold(0)
        mSearchAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, autocompleteStrings)
        mSearchAutocomplete!!.setAdapter(mSearchAdapter)
        when (mMyViewModel!!.viewMode) {
            App.VIEW_MODE_NORMAL -> menu.findItem(R.id.menuUseNormalStyle).isChecked = true
            App.VIEW_MODE_LIGHT -> menu.findItem(R.id.menuUseLightStyle).isChecked = true
            App.VIEW_MODE_FAST -> menu.findItem(R.id.menuUseLightFastStyle).isChecked = true
            App.VIEW_MODE_FAT -> menu.findItem(R.id.menuUseLightFatStyle).isChecked = true
            App.VIEW_MODE_FAST_FAT -> menu.findItem(R.id.menuUseFatFastStyle).isChecked = true
        }
        var menuItem = menu.findItem(R.id.menuUseDarkMode)
        menuItem.isChecked = mMyViewModel!!.nightModeEnabled
        menuItem = menu.findItem(R.id.logOut)
        menuItem.isVisible = PreferencesHandler.instance.authCookie != null
        menuItem = menu.findItem(R.id.login)
        if (PreferencesHandler.instance.authCookie != null) {
            menuItem.isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.menuUseLightStyle || itemId == R.id.menuUseLightFastStyle || itemId == R.id.menuUseLightFatStyle || itemId == R.id.menuUseNormalStyle || itemId == R.id.menuUseFatFastStyle) {
            mMyViewModel!!.switchViewMode(item.itemId)
            invalidateOptionsMenu()
            mWebView!!.reload()
            return true
        } else if (itemId == R.id.goHome) {
            mWebView!!.loadUrl(getBaseOPDSUrl())
            return true
        } else if (itemId == R.id.showNew) {
            mWebView!!.loadUrl(App.NEW_BOOKS)
            return true
        } else if (itemId == R.id.randomBook) {
            mWebView!!.loadUrl(mMyViewModel!!.randomBookUrl)
            return true
        } else if (itemId == R.id.shareLink) {
            mMyViewModel!!.shareLink(mWebView!!)
            return true
        } else if (itemId == R.id.login) {
            showLoginDialog()
            return true
        } else if (itemId == R.id.clearSearchHistory) {
            clearHistory()
            return true
        } else if (itemId == R.id.logOut) { // удалю куку
            PreferencesHandler.instance.authCookie = null
            recreate()
            return true
        }
        if (item.itemId == R.id.menuUseDarkMode) {
            mMyViewModel!!.switchNightMode()
            Handler().postDelayed(ResetApp(), 100)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showLoginDialog() {
        startActivityForResult(Intent(this, LoginActivity::class.java), CODE_REQUEST_LOGIN)
    }

    private fun clearHistory() {
        mMyViewModel!!.clearHistory()
        autocompleteStrings = ArrayList()
        mSearchAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, autocompleteStrings)
        mSearchAutocomplete!!.setAdapter(mSearchAdapter)
        Toast.makeText(this, "Автозаполнение сброшено", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MainActivity.Companion.START_TOR) {
            // перезагружу страницу
            mWebView!!.setup()
            mWebView!!.loadUrl(PreferencesHandler.instance.lastLoadedUrl)
        } else if (requestCode == CODE_REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                invalidateOptionsMenu()
                mWebView!!.loadUrl(PreferencesHandler.instance.lastLoadedUrl)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onQueryTextSubmit(s: String): Boolean {
        if (!TextUtils.isEmpty(s.trim { it <= ' ' })) {
            // ищу введённое значение
            makeSearch(s)
        }
        return true
    }

    private fun makeSearch(s: String) {
        changeTitle("Поиск: $s")
        val searchString = App.SEARCH_URL + s.trim { it <= ' ' }
        mWebView!!.loadUrl(searchString)
        // занесу значение в список автозаполнения
        if (putSearchValue(s)) {
            // обновлю список поиска
            autocompleteStrings = mMyViewModel!!.searchAutocomplete
            mSearchAdapter!!.clear()
            mSearchAdapter!!.addAll(autocompleteStrings)
            mSearchAdapter!!.notifyDataSetChanged()
        }
    }

    override fun onQueryTextChange(s: String): Boolean {
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) { // возвращаюсь на страницу назад в браузере
                assert(mWebView != null)
                if (mWebView!!.canGoBack()) {
                    mWebView!!.goBack()
                } else {
                    if (mIsActivityCalled) {
                        finish()
                        return true
                    }
                    if (mConfirmExit != 0L) {
                        if (mConfirmExit > System.currentTimeMillis() - 3000) {
                            // выйду из приложения
                            Log.d("surprise", "OPDSActivity onKeyDown exit")
                            finishAffinity()
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
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleLoading() {
        startBrowsing()
    }

    private fun startBrowsing() {
        mWebView!!.setup()
        mWebView!!.loadUrl(PreferencesHandler.instance.lastLoadedUrl)
    }

    inner class BookLoadingReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.getIntExtra(MyWebViewClient.BOOK_LOAD_EVENT, 0)
            when (action) {
                MyWebViewClient.START_BOOK_LOADING -> showBookLoadingDialog()
                MyWebViewClient.FINISH_BOOK_LOADING -> {
                    mWebView!!.loadUrl(PreferencesHandler.instance.lastLoadedUrl)
                    hideBookLoadingDialog()
                }
                else -> hideBookLoadingDialog()
            }
        }
    }

    inner class TorConnectErrorReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // покажу диалоговое окно с оповещением, что TOR остановлен и кнопкой повторного запуска
            val errorDetails = intent.getStringExtra(TorWebClient.ERROR_DETAILS)
            showTorRestartDialog(errorDetails)
        }
    }

    private fun showTorRestartDialog(errorDetails: String) {
        var errorDetails: String? = errorDetails
        if (errorDetails == null) {
            errorDetails = ""
        }
        if (mTorRestartDialog == null) {
            // создам диалоговое окно
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle(R.string.tor_is_stopped)
                .setMessage(R.string.tor_restart_dialog_message.toString() + errorDetails)
                .setPositiveButton(R.string.restart_tor_message) { dialog: DialogInterface, which: Int ->
                    App.instance.startTor()
                    dialog.dismiss()
                    // вернусь в основное активити и подожду перезапуска
                    val intent = Intent(this@WebViewActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                }
                .setNegativeButton("Ok", null)
                .setCancelable(true)
            mTorRestartDialog = dialogBuilder.create()
        }
        mTorRestartDialog!!.show()
    }

    private fun showBookLoadingDialog() {
        if (mShowLoadDialog == null) {
            mShowLoadDialog = GifDialog.Builder(this)
                .setTitle(getString(R.string.load_waiting_title))
                .setMessage(getString(R.string.load_waiting_message))
                .setGifResource(R.drawable.loading) //Pass your Gif here
                .isCancellable(false)
                .build()
        }
        mShowLoadDialog!!.show()
    }

    private fun hideBookLoadingDialog() {
        if (mShowLoadDialog != null) {
            mShowLoadDialog!!.hide()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.d("surprise", "MainActivity onLowMemory: oops, low memory... Save me...")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mWebView!!.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mWebView!!.restoreState(savedInstanceState)
    }

    private inner class ResetApp : Runnable {
        override fun run() {
            val intent = Intent(this@WebViewActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            this@WebViewActivity.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val CALLED = "activity_called"
        const val CODE_REQUEST_LOGIN = 1
    }
}