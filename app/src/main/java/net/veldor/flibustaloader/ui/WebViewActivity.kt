package net.veldor.flibustaloader.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.SearchAutoComplete
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.MyWebViewClient
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.ActivityWebViewBinding
import net.veldor.flibustaloader.dialogs.ChangelogDialog
import net.veldor.flibustaloader.dialogs.GifDialog
import net.veldor.flibustaloader.http.GlobalWebClient
import net.veldor.flibustaloader.http.TorWebClient
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper
import net.veldor.flibustaloader.utils.XMLHandler.putSearchValue
import net.veldor.flibustaloader.view_models.OPDSViewModel
import java.util.*

class WebViewActivity : BaseActivity(), SearchView.OnQueryTextListener {
    private lateinit var binding: ActivityWebViewBinding
    private lateinit var viewModel: OPDSViewModel
    private var mPageLoadReceiver: BookLoadingReceiver? = null
    private lateinit var autocompleteStrings: ArrayList<String>
    private lateinit var mSearchView: SearchView
    private var mSearchAdapter: ArrayAdapter<String>? = null
    private var mTorRestartDialog: AlertDialog? = null
    private var mTorConnectErrorReceiver: TorConnectErrorReceiver? = null
    private var mConfirmExit: Long = 0
    private var mShowLoadDialog: Dialog? = null
    private lateinit var mSearchAutocomplete: SearchAutoComplete
    private var mIsActivityCalled = false
    private var mDisconnectedSnackbar: Snackbar? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // добавлю viewModel
        viewModel = ViewModelProvider(this).get(OPDSViewModel::class.java)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)

        setupInterface()
        showChangesList()

        // проверю, не запущено ли приложение с помощью интента
        if (intent.data != null) {
            val data = intent.data!!
            val fullPath = data.encodedPath
            PreferencesHandler.instance.lastLoadedUrl = PreferencesHandler.BASE_URL + fullPath
        }
        // проверю, не вызвана ли активность
        mIsActivityCalled = intent.getBooleanExtra(CALLED, false)
        if (mIsActivityCalled) {
            val actionBar = supportActionBar
            actionBar?.setDisplayHomeAsUpEnabled(true)
        }

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
        autocompleteStrings = viewModel.searchAutocomplete
    }

    override fun setupInterface() {
        super.setupInterface()

        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
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
                binding.root,
                getString(R.string.connection_lost_message),
                Snackbar.LENGTH_INDEFINITE
            )
            mDisconnectedSnackbar!!.setAction(getString(R.string.reload_page_message)) {
                mDisconnectedSnackbar!!.dismiss()
                binding.myWebView.reload()
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
            val version = viewModel.startCheckUpdate()
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
        if (viewModel.nightModeEnabled) {
            binding.myWebView.setBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    android.R.color.black,
                    null
                )
            )
        } else {
            binding.myWebView.setBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    android.R.color.white,
                    null
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mPageLoadReceiver != null) unregisterReceiver(mPageLoadReceiver)
        if (mTorConnectErrorReceiver != null) unregisterReceiver(mTorConnectErrorReceiver)
    }

    private fun makeUpdateSnackbar() {
        val updateSnackbar = Snackbar.make(
           binding.root,
            getString(R.string.snackbar_found_update_message),
            Snackbar.LENGTH_LONG
        )
        updateSnackbar.setAction(getString(R.string.snackbar_update_action_message)) { viewModel.initializeUpdate() }
        updateSnackbar.setActionTextColor(
            ResourcesCompat.getColor(
                resources,
                android.R.color.white,
                null
            )
        )
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
        mSearchView.inputType = InputType.TYPE_CLASS_TEXT
        mSearchView.setOnQueryTextListener(this)
        mSearchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(i: Int): Boolean {
                return true
            }

            override fun onSuggestionClick(i: Int): Boolean {
                val value = autocompleteStrings[i]
                mSearchView.setQuery(value, true)
                return true
            }
        })
        mSearchAutocomplete = mSearchView.findViewById(R.id.search_src_text)
        if (PreferencesHandler.instance.isEInk) {
            mSearchAutocomplete.setTextColor(Color.WHITE)
        }
        mSearchAutocomplete.setDropDownBackgroundResource(android.R.color.white)
        mSearchAutocomplete.dropDownAnchor = R.id.action_search
        mSearchAutocomplete.threshold = 0
        mSearchAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, autocompleteStrings)
        mSearchAutocomplete.setAdapter(mSearchAdapter)
        when (viewModel.viewMode) {
            App.VIEW_MODE_NORMAL -> menu.findItem(R.id.menuUseNormalStyle).isChecked = true
            App.VIEW_MODE_LIGHT -> menu.findItem(R.id.menuUseLightStyle).isChecked = true
            App.VIEW_MODE_FAST -> menu.findItem(R.id.menuUseLightFastStyle).isChecked = true
            App.VIEW_MODE_FAT -> menu.findItem(R.id.menuUseLightFatStyle).isChecked = true
            App.VIEW_MODE_FAST_FAT -> menu.findItem(R.id.menuUseFatFastStyle).isChecked = true
        }
        var menuItem = menu.findItem(R.id.menuUseDarkMode)
        menuItem.isChecked = viewModel.nightModeEnabled
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
            viewModel.switchViewMode(item.itemId)
            invalidateOptionsMenu()
            binding.myWebView.reload()
            return true
        } else if (itemId == R.id.goHome) {
            binding.myWebView.loadUrl(URLHelper.getBaseUrl())
            return true
        } else if (itemId == R.id.showNew) {
            binding.myWebView.loadUrl(App.NEW_BOOKS)
            return true
        } else if (itemId == R.id.randomBook) {
            binding.myWebView.loadUrl(viewModel.randomBookUrl)
            return true
        } else if (itemId == R.id.shareLink) {
            viewModel.shareLink(binding.myWebView)
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
            viewModel.switchNightMode()
            Handler().postDelayed(ResetApp(), 100)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showLoginDialog() {
        loginLauncher.launch(Intent(this, LoginActivity::class.java))
    }

    private var loginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                invalidateOptionsMenu()
                binding.myWebView.loadUrl(PreferencesHandler.instance.lastLoadedUrl)
            }
        }

    private fun clearHistory() {
        viewModel.clearHistory()
        autocompleteStrings = ArrayList()
        mSearchAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, autocompleteStrings)
        mSearchAutocomplete.setAdapter(mSearchAdapter)
        Toast.makeText(this, "Автозаполнение сброшено", Toast.LENGTH_SHORT).show()
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
        binding.myWebView.loadUrl(searchString)
        // занесу значение в список автозаполнения
        if (putSearchValue(s)) {
            // обновлю список поиска
            autocompleteStrings = viewModel.searchAutocomplete
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
                if (binding.myWebView.canGoBack()) {
                    binding.myWebView.goBack()
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
        binding.myWebView.setup()
        Log.d("surprise", "startBrowsing: last loaded is ${PreferencesHandler.instance.lastLoadedUrl}")
        binding.myWebView.loadUrl(PreferencesHandler.instance.lastLoadedUrl)
    }

    inner class BookLoadingReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(MyWebViewClient.BOOK_LOAD_EVENT, 0)) {
                MyWebViewClient.START_BOOK_LOADING -> showBookLoadingDialog()
                MyWebViewClient.FINISH_BOOK_LOADING -> {
                    binding.myWebView.loadUrl(PreferencesHandler.instance.lastLoadedUrl)
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
        if (mTorRestartDialog == null) {
            // создам диалоговое окно
            val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            dialogBuilder.setTitle(R.string.tor_is_stopped)
                .setMessage(R.string.tor_restart_dialog_message.toString() + errorDetails)
                .setPositiveButton(R.string.restart_tor_message) { dialog: DialogInterface, _: Int ->
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.myWebView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.myWebView.restoreState(savedInstanceState)
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
    }
}