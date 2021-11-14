package net.veldor.flibustaloader.ui

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.MenuItem
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.navigation.NavigationView
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.dialogs.DonationDialog
import net.veldor.flibustaloader.workers.SendLogWorker

class NavigatorSelectHandler(private val mContext: Activity) :
    NavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d("surprise", "OPDSActivityNew onNavigationItemSelected 62: click navigation")
        when (item.itemId) {
            R.id.goBrowse -> {
                val intent = Intent(mContext, BrowserActivity::class.java)
                intent.flags = FLAG_ACTIVITY_CLEAR_TOP
                mContext.startActivity(intent)
                mContext.finish()
            }
            R.id.goToDownloadsList -> {
                val intent = Intent(mContext, DownloadScheduleActivity::class.java)
                mContext.startActivity(intent)
                tryCloseDrawer()
            }
            R.id.goToSubscriptions -> {
                val intent = Intent(mContext, SubscriptionsActivity::class.java)
                mContext.startActivity(intent)
                tryCloseDrawer()
            }
            R.id.goToBookmarks -> {
                val intent = Intent(mContext, BookmarksActivity::class.java)
                mContext.startActivity(intent)
                tryCloseDrawer()
            }
            R.id.goToBlacklist -> {
                val intent = Intent(mContext, BlacklistActivity::class.java)
                mContext.startActivity(intent)
                tryCloseDrawer()
            }
            R.id.goToFileList -> {
                val intent = Intent(mContext, DirContentActivity::class.java)
                mContext.startActivity(intent)
                tryCloseDrawer()
            }
            R.id.goToSettings -> {
                val intent = Intent(mContext, SettingsActivity::class.java)
                mContext.startActivity(intent)
                tryCloseDrawer()
            }
            R.id.buyCoffee -> {
                DonationDialog.Builder(mContext).build().show()
                tryCloseDrawer()
            }
            R.id.testAppInvite -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://t.me/flibusta_downloader_beta")
                mContext.startActivity(intent)
            }
            R.id.shareApp -> {
                // get link for latest release
                App.instance.shareLatestRelease()
                tryCloseDrawer()
            }
            R.id.exitApp -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mContext.finishAndRemoveTask()
                }
                else{
                    mContext.finishAffinity()
                }
                Runtime.getRuntime().exit(0)
            }
            R.id.sendLog -> {
                val work = OneTimeWorkRequest.Builder(SendLogWorker::class.java).build()
                WorkManager.getInstance(App.instance).enqueue(work)
                tryCloseDrawer()
            }
        }
        return false
    }

    private fun tryCloseDrawer() {
        val drawer: DrawerLayout = mContext.findViewById(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
    }
}