package net.veldor.flibustaloader.ui

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.material.navigation.NavigationView
import lib.folderpicker.FolderPicker
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.dialogs.ChangelogDialog
import net.veldor.flibustaloader.utils.Grammar
import net.veldor.flibustaloader.utils.MyFileReader.SUBSCRIPTIONS_FILE
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.TransportUtils
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.util.*
import kotlin.system.exitProcess

open class BaseActivity : AppCompatActivity() {
    private lateinit var mDownloadsListTextView: TextView
    private lateinit var mSubscriptionsListTextView: TextView
    protected lateinit var mNavigationView: NavigationView
    private var mDrawer: DrawerLayout? = null

    protected open fun setupInterface() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (PreferencesHandler.instance.isEInk) {
                // change status bar color
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor =
                    ResourcesCompat.getColor(resources, R.color.white, null)
                window.navigationBarColor =
                    ResourcesCompat.getColor(resources, R.color.white, null)
            } else {
                // change status bar color
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor =
                    ResourcesCompat.getColor(resources, R.color.bottomNavigationColor, null)
                window.navigationBarColor =
                    ResourcesCompat.getColor(resources, R.color.bottomNavigationColor, null)
            }
        }
        // включу аппаратное ускорение, если оно активно
        if (PreferencesHandler.instance.hardwareAcceleration) {
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
            mNavigationView.menu.findItem(R.id.goToDownloadsList).actionView
            mDownloadsListTextView =
                mNavigationView.menu.findItem(R.id.goToDownloadsList).actionView as TextView
            mSubscriptionsListTextView =
                mNavigationView.menu.findItem(R.id.goToSubscriptions).actionView as TextView
            mNavigationView.menu.findItem(R.id.appVersion).title = String.format(
                Locale.ENGLISH,
                getString(R.string.application_version_message),
                Grammar.appVersion
            )
            // метод для счетчиков
            initializeCountDrawer()
        }
    }

    private fun initializeCountDrawer() {
        setDownloadsLength()
        setSubscriptionsLength()
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
        sLiveDownloadScheduleCountChanged.observe(this, { changed: Boolean ->
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
                    mSubscriptionsListTextView.setTextColor(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.book_name_color,
                            null
                        )
                    )
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
            mDownloadsListTextView.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.book_name_color,
                    null
                )
            )
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

    // observer
    class DialogDismissLifecycleObserver(private var dialog: Dialog?) : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
            dialog?.dismiss()
            dialog = null
        }
    }

    protected fun showChangesList() {
        // покажу список изменений, если он ещё не показан для этой версии
        if (PreferencesHandler.instance.isShowChanges()) {
            ChangelogDialog.Builder(this).build().show()
            PreferencesHandler.instance.setChangesViewed()
        }
    }

    protected fun paintToolbar(toolbar: Toolbar) {
        toolbar.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.white, null))
        toolbar.setTitleTextColor(ResourcesCompat.getColor(resources, R.color.black, null))
        toolbar.setSubtitleTextColor(ResourcesCompat.getColor(resources, R.color.black, null))
        val colorFilter = PorterDuffColorFilter(
            ResourcesCompat.getColor(resources, R.color.black, null),
            PorterDuff.Mode.MULTIPLY
        )

        for (i in 0 until toolbar.childCount) {
            val view: View = toolbar.getChildAt(i)
            //Back button or drawer open button
            if (view is ImageButton) {
                Log.d("surprise", "BrowserActivity.kt 113  setupInterface: have imagepaint")
                view.drawable.colorFilter = colorFilter
            }
            if (view is ActionMenuView) {
                for (j in 0 until view.childCount) {
                    val innerView: View = view.getChildAt(j)

                    //Any ActionMenuViews - icons that are not back button, text or overflow menu
                    if (innerView is ActionMenuItemView) {
                        val drawables = innerView.compoundDrawables
                        for (k in drawables.indices) {
                            val drawable = drawables[k]
                            if (drawable != null) {
                                //Set the color filter in separate thread
                                //by adding it to the message queue - won't work otherwise
                                innerView.post({
                                    innerView.compoundDrawables[k].colorFilter =
                                        colorFilter
                                })
                            }
                        }
                    }
                }
            }
        }

        //Overflow icon

        //Overflow icon
        val overflowIcon: Drawable? = toolbar.overflowIcon
        if (overflowIcon != null) {
            overflowIcon.colorFilter = colorFilter
            toolbar.overflowIcon = overflowIcon
        }
    }


    fun showSelectDownloadFolderDialog() {
        val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
        dialogBuilder.setTitle(getString(R.string.select_download_folder_title))
            .setMessage(getString(R.string.select_download_folder_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.download_folder_select_accept)) { _: DialogInterface?, _: Int ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    var intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                    )
                    if (TransportUtils.intentCanBeHandled(intent)) {
                        dirSelectResultLauncher.launch(intent)
                    } else {
                        intent = Intent(this, FolderPicker::class.java)
                        intent.addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        )
                        compatDirSelectResultLauncher.launch(intent)
                    }
                } else {
                    val intent = Intent(this, FolderPicker::class.java)
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                        intent.addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        )
                    }
                    compatDirSelectResultLauncher.launch(intent)
                }
            }
            .setNegativeButton(getString(R.string.dismiss_permissions_button)) { _: DialogInterface?, _: Int ->
                exitProcess(
                    0
                )
            }
            .setNeutralButton(getString(R.string.alternate_dir_select_button)) { _: DialogInterface?, _: Int -> showAlterDirSelectDialog() }
        if (!this.isFinishing) {
            val dialog = dialogBuilder.create()
            lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
            dialog.show()
        }
    }


    private fun showAlterDirSelectDialog() {
        val dialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
            .setTitle(getString(R.string.alter_dir_selection_title))
            .setMessage(getString(R.string.alter_dir_selection_body))
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                val intent = Intent(this, FolderPicker::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    )
                }
                compatDirSelectResultLauncher.launch(intent)
            }
        val dialog = dialogBuilder.create()
        lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
        dialog.show()
    }

    var compatDirSelectResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data != null && data.extras != null && data.extras!!.containsKey("data")) {
                    val folderLocation = data.extras!!.getString("data")
                    val file = File(folderLocation)
                    if (file.isDirectory && PreferencesHandler.instance.saveDownloadFolder(
                            folderLocation
                        )
                    ) {
                        Toast.makeText(this, "Папка сохранена!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Не удалось сохранить папку, попробуйте ещё раз!",
                            Toast.LENGTH_SHORT
                        ).show()
                        showSelectDownloadFolderDialog()
                    }
                }
            }
        }

    var dirSelectResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                if (data != null) {
                    val treeUri = data.data
                    if (treeUri != null) {
                        // проверю наличие файла
                        val dl = DocumentFile.fromTreeUri(App.instance, treeUri)
                        if (dl != null && dl.isDirectory) {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    App.instance.contentResolver.takePersistableUriPermission(
                                        treeUri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                    App.instance.contentResolver.takePersistableUriPermission(
                                        treeUri,
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    )
                                }
                                PreferencesHandler.instance.setDownloadDir(dl)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this,
                                    "Не удалось выдать разрешения на доступ, попробуем другой метод",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(this, FolderPicker::class.java)
                                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                                    intent.addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                    )
                                }
                                compatDirSelectResultLauncher.launch(intent)
                            }
                        }
                    }
                }
            }
        }



    companion object {
        @JvmField
        val sLiveDownloadScheduleCountChanged = MutableLiveData<Boolean>()
        val sLiveFoundedSubscriptionsCount = MutableLiveData<Boolean>()
    }
}