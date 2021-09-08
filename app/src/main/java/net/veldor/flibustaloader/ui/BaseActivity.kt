package net.veldor.flibustaloader.ui

import android.os.Bundle
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.App
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import android.os.Build
import androidx.core.view.GravityCompat
import android.graphics.Typeface
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import android.util.Log
import android.view.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import net.veldor.flibustaloader.utils.MyFileReader.SUBSCRIPTIONS_FILE
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.lang.Exception
import java.util.*

open class BaseActivity : AppCompatActivity() {
    private lateinit var mDownloadsListTextView: TextView
    private lateinit var mSubscriptionsListTextView: TextView
    protected lateinit var mNavigationView: NavigationView
    private var mDrawer: DrawerLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupObservers()
    }

    protected open fun setupInterface() {
        // включу аппаратное ускорение, если оно активно
        if (PreferencesHandler.instance.isHardwareAcceleration()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        }

        // включу поддержку тулбара
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar?.let { setSupportActionBar(it) }
        // покажу гамбургер :)
        mDrawer = findViewById(R.id.drawer_layout)
        if (mDrawer != null) {
            val toggle = ActionBarDrawerToggle(
                this,
                mDrawer,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            )
            mDrawer!!.addDrawerListener(toggle)
            toggle.isDrawerIndicatorEnabled = true
            toggle.syncState()
            mNavigationView = findViewById(R.id.nav_view)
            mNavigationView.setNavigationItemSelectedListener(NavigatorSelectHandler(this))
            // отображу бейджи в меню
            mNavigationView.getMenu().findItem(R.id.goToDownloadsList).actionView
            mDownloadsListTextView =
                mNavigationView.getMenu().findItem(R.id.goToDownloadsList).actionView as TextView
            mSubscriptionsListTextView =
                mNavigationView.getMenu().findItem(R.id.goToSubscriptions).actionView as TextView
            // метод для счетчиков
            initializeCountDrawer()
        }
    }

    private fun initializeCountDrawer() {
        setDownloadsLength()
        setSubscriptionsLength()
    }

    protected fun changeTitle(s: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(supportActionBar)!!.title = s
        }
    }

    override fun onBackPressed() {
        if (mDrawer != null) {
            if (mDrawer!!.isDrawerOpen(GravityCompat.START)) {
                mDrawer!!.closeDrawer(GravityCompat.START)
            } else {
                super.onBackPressed()
            }
        }
    }

    protected open fun setupObservers() {
        // Отслежу изменение числа книг в очереди закачек
        sLiveDownloadScheduleCount.observe(this, { changed: Boolean ->
            if (changed) {
                setDownloadsLength()
            }
        })
        // Отслежу изменение числа книг в очереди закачек
        sLiveFoundedSubscriptionsCount.observe(this, { changed: Boolean ->
            if (changed) {
                setSubscriptionsLength()
            }
        })
    }

    private fun setSubscriptionsLength() {
        // получу размер очереди подписок
        val autocompleteFile = File(App.instance.filesDir, SUBSCRIPTIONS_FILE)
        try {
            if (autocompleteFile.exists()) {
                val fis = FileInputStream(autocompleteFile)
                val ois = ObjectInputStream(fis)
                val list = ois.readObject() as ArrayList<*>
                val listLength = list.size
                Log.d(
                    "surprise",
                    "OPDSActivity initializeCountDrawer 577: founded result size is $listLength"
                )
                if (listLength > 0) {
                    mSubscriptionsListTextView.gravity = Gravity.CENTER_VERTICAL
                    mSubscriptionsListTextView.setTypeface(null, Typeface.BOLD)
                    mSubscriptionsListTextView.setTextColor(resources.getColor(R.color.book_name_color))
                    mSubscriptionsListTextView.text = listLength.toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setDownloadsLength() {
        val queueSize = App.instance.mDatabase.booksDownloadScheduleDao().queueSize
        if (queueSize > 0) {
            mDownloadsListTextView.visibility = View.VISIBLE
            mDownloadsListTextView.gravity = Gravity.CENTER_VERTICAL
            mDownloadsListTextView.setTypeface(null, Typeface.BOLD)
            mDownloadsListTextView.setTextColor(resources.getColor(R.color.book_name_color))
            mDownloadsListTextView.text = queueSize.toString()
        } else {
            mDownloadsListTextView.visibility = View.INVISIBLE
        }
    }

    class ResetApp : Runnable {
        override fun run() {
            val intent = Intent(App.instance, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            App.instance.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }

    companion object {
        @JvmField
        val sLiveDownloadScheduleCount = MutableLiveData<Boolean>()
        val sLiveFoundedSubscriptionsCount = MutableLiveData<Boolean>()
    }
}