package net.veldor.flibustaloader.ui

import android.os.Bundle
import net.veldor.flibustaloader.R
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.Navigation

class SubscriptionsActivity : BaseActivity() {
    private var mBottomNavView: BottomNavigationView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_subscriptions_activity)
        setupInterface()


        // проверю, если в интенте указано открыть целевой фрагмент- открою его
        val intent = intent
        val startFragment = intent.getIntExtra(START_FRAGMENT, -1)
        if (startFragment >= 0) {
            if (startFragment == START_RESULTS) {
                switchToResults()
            }
        }
    }

    private fun switchToResults() {
        mBottomNavView!!.selectedItemId = R.id.navigation_subscribe_results
    }

    override fun setupInterface() {
        super.setupInterface()
        // скрою переход на данное активити
        val menuNav = mNavigationView!!.menu
        val item = menuNav.findItem(R.id.goToSubscriptions)
        item.isEnabled = false
        item.isChecked = true
        // активирую нижнее меню
        mBottomNavView = findViewById(R.id.bottom_nav_view)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration.Builder(
            R.id.navigation_subscribe, R.id.navigation_subscribe_results
        )
            .build()
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(mBottomNavView!!, navController)
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
        const val START_FRAGMENT = "start fragment"
        const val START_RESULTS = 1
    }
}