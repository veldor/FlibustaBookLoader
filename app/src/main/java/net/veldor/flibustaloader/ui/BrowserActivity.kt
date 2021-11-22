package net.veldor.flibustaloader.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import co.mobiwise.materialintro.shape.Focus
import co.mobiwise.materialintro.shape.FocusGravity
import co.mobiwise.materialintro.shape.ShapeType
import co.mobiwise.materialintro.view.MaterialIntroView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.ActivityBrowserBinding
import net.veldor.flibustaloader.ui.fragments.OpdsFragment
import net.veldor.flibustaloader.ui.fragments.WebViewFragment
import net.veldor.flibustaloader.utils.History
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.view_models.WebViewViewModel


class BrowserActivity : BaseActivity() {

    private var mConfirmExit: Long = 0
    lateinit var viewModel: WebViewViewModel
    lateinit var binding: ActivityBrowserBinding
    lateinit var mBottomNavView: BottomNavigationView

    override fun getTheme(): Resources.Theme {
        if (PreferencesHandler.instance.isEInk) {
            val theme = super.getTheme()
            theme.applyStyle(R.style.EInkAppTheme, true)
            return theme
        }
        return super.getTheme()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(WebViewViewModel::class.java)

        // проверю папку загрузок
        val dd = PreferencesHandler.instance.getDownloadDir()
        if (dd == null || !dd.exists() || !dd.isDirectory) {
            val cdd = PreferencesHandler.instance.getCompatDownloadDir()
            if(cdd == null || !cdd.exists() || !cdd.isDirectory){
                showSelectDownloadFolderDialog()
            }
        }

        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupInterface()
        showChangesList()
        setupObservers()
        checkUpdates()
        showTooltip()

        val webViewTarget = intent.getStringExtra(EXTERNAL_LINK)
        if (webViewTarget != null) {
            mBottomNavView.selectedItemId = R.id.navigation_web_view
            PreferencesHandler.instance.lastLoadedUrl =
                webViewTarget.replace("http://flibustahezeous3.onion", "")
                    .replace("http://flibusta.is", "")
        }
    }

    override fun setupObservers() {
        super.setupObservers()
        viewModel.isUpdateAvailable.observe(this, {
            if (it) {
                makeUpdateSnackbar()
            }
        })
    }


    override fun setupInterface() {
        super.setupInterface()

        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goBrowse)
        item.isEnabled = false
        item.isChecked = true

        if (PreferencesHandler.instance.isEInk) {
            val colors = intArrayOf(
                Color.LTGRAY,
                Color.BLACK
            )

            val states = arrayOf(
                intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked)
            )
            binding.bottomNavView.itemTextColor = ColorStateList(states, colors)
            binding.bottomNavView.itemIconTintList = ColorStateList(states, colors)

            binding.bottomNavView.setBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )
            binding.bottomNavView.itemTextColor
            paintToolbar(binding.toolbar)
        }

        // активирую нижнее меню
        mBottomNavView = findViewById(R.id.bottom_nav_view)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration.Builder(
            R.id.navigation_opds, R.id.navigation_web_view
        )
            .build()
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(mBottomNavView, navController)
        if (PreferencesHandler.instance.view == VIEW_WEB) {
            mBottomNavView.selectedItemId = R.id.navigation_web_view
        }
        navController.addOnDestinationChangedListener { _: NavController, _: NavDestination, _: Bundle? ->
            val selectedFragment = mBottomNavView.selectedItemId
            if (selectedFragment == R.id.navigation_web_view) {
                PreferencesHandler.instance.view = VIEW_WEB
            } else {
                PreferencesHandler.instance.view = VIEW_OPDS
            }
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

    private fun checkUpdates() {
        if (PreferencesHandler.instance.isCheckUpdate) {
            // проверю обновления
            viewModel.checkUpdate()
        }
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val fragment = getCurrentFragment()
        if (fragment is WebViewFragment) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    // возвращаюсь на страницу назад в браузере
                    if (fragment.binding.myWebView.canGoBack()) {
                        fragment.binding.myWebView.goBack()
                    } else if (PreferencesHandler.instance.openedFromOpds) {
                        PreferencesHandler.instance.openedFromOpds = false
                        mBottomNavView.selectedItemId =
                            R.id.navigation_opds
                    } else {
//                        if (mIsActivityCalled) {
//                            finish()
//                            return true
//                        }
                        if (mConfirmExit > 0) {
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
        } else if (fragment is OpdsFragment) {
            if (PreferencesHandler.instance.isEInk) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_PAGE_UP) {
                    fragment.scrollUp()
                    return true
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
                    fragment.scrollDown()
                    return true
                }
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (fragment.mCoverPreviewDialog != null && fragment.mCoverPreviewDialog!!.isShowing) {
                    fragment.mBookTypeDialog!!.dismiss()
                    return true
                }
                // если доступен возврат назад- возвращаюсь, если нет- закрываю приложение
                if (!History.instance!!.isEmpty) {
                    val lastPage = History.instance!!.lastPage
                    if (lastPage != null) {
                        Log.d("surprise", "onKeyDown: go back to history ${lastPage.loadedValues}")
                        fragment.loadFromHistory(lastPage)
                        return true
                    }
                }
                if (mConfirmExit != 0L) {
                    if (mConfirmExit > System.currentTimeMillis() - 3000) {
                        // выйду из приложения
                        Log.d("surprise", "OPDSActivity onKeyDown exit")
                        // this.finishAffinity();
                        val startMain = Intent(Intent.ACTION_MAIN)
                        startMain.addCategory(Intent.CATEGORY_HOME)
                        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(startMain)
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
            return super.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)


        val link = intent.getStringExtra(OpdsFragment.TARGET_LINK)
        if (link != null) {
            // переключу на OPDS
            mBottomNavView.selectedItemId = R.id.navigation_opds
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    fun getCurrentFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        return navHostFragment?.childFragmentManager?.findFragmentById(R.id.nav_host_fragment)
    }

    companion object {
        const val VIEW_WEB = 1
        const val VIEW_OPDS = 2
        const val EXTERNAL_LINK = "external link"
    }
}


private fun BrowserActivity.showTooltip() {
    Handler(Looper.getMainLooper()).postDelayed({
        kotlin.run {
            MaterialIntroView.Builder(this)
                .enableDotAnimation(true)
                .enableIcon(false)
                .setFocusGravity(FocusGravity.CENTER)
                .setFocusType(Focus.MINIMUM)
                .setDelayMillis(300)
                .setUsageId("select browse type")
                .enableFadeAnimation(true)
                .performClick(true)
                .setListener {

                }
                .setInfoText(getString(R.string.browser_first_help_text))
                .setTarget(binding.bottomNavView)
                .setShape(ShapeType.CIRCLE)
                .show()
        }
    }, 100)
}