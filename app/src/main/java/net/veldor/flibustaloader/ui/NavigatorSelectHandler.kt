package net.veldor.flibustaloader.ui

import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.App
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import android.os.Build
import androidx.core.view.GravityCompat
import android.content.Intent
import android.app.Activity
import android.net.Uri
import android.util.Log
import android.view.*
import net.veldor.flibustaloader.dialogs.DonationDialog

class NavigatorSelectHandler(private val mContext: Activity) :
    NavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d("surprise", "OPDSActivityNew onNavigationItemSelected 62: click navigation")
        val itemId = item.itemId
        if (itemId == R.id.goToWebView) {
            App.instance.view = App.VIEW_WEB
            val intent = Intent(mContext, WebViewActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            mContext.startActivity(intent)
            mContext.finish()
        } else if (itemId == R.id.goToOPDS) {
            val intent: Intent
            App.instance.view = App.VIEW_ODPS
            intent = Intent(mContext, OPDSActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            mContext.startActivity(intent)
            mContext.finish()
        } else if (itemId == R.id.goToDownloadsList) {
            val intent: Intent
            intent = Intent(mContext, ActivityBookDownloadSchedule::class.java)
            mContext.startActivity(intent)
            tryCloseDrawer()
        } else if (itemId == R.id.goToSubscriptions) {
            val intent: Intent
            intent = Intent(mContext, SubscriptionsActivity::class.java)
            mContext.startActivity(intent)
            tryCloseDrawer()
        } else if (itemId == R.id.goToBookmarks) {
            val intent: Intent
            intent = Intent(mContext, BookmarksActivity::class.java)
            mContext.startActivity(intent)
            tryCloseDrawer()
        } else if (itemId == R.id.goToBlacklist) {
            val intent: Intent
            intent = Intent(mContext, BlacklistActivity::class.java)
            mContext.startActivity(intent)
            tryCloseDrawer()
        } else if (itemId == R.id.goToFileList) {
            val intent: Intent
            intent = Intent(mContext, ShowDownloadFolderContentActivity::class.java)
            mContext.startActivity(intent)
            tryCloseDrawer()
        } else if (itemId == R.id.goToSettings) {
            val intent: Intent
            intent = Intent(mContext, SettingsActivity::class.java)
            mContext.startActivity(intent)
            tryCloseDrawer()
        } else if (itemId == R.id.buyCoffee) {
            DonationDialog.Builder(mContext).build().show()
            tryCloseDrawer()
        } else if (itemId == R.id.testAppInvite) {
            val intent: Intent
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://t.me/flibusta_downloader_beta")
            mContext.startActivity(intent)
        } else if (itemId == R.id.shareApp) {
            // get link for latest release
            App.instance.shareLatestRelease()
            tryCloseDrawer()
        } else if (itemId == R.id.exitApp) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mContext.finishAndRemoveTask()
                System.exit(0)
            } else {
                mContext.finishAffinity()
                System.exit(0)
            }
        }
        return false
    }

    private fun tryCloseDrawer() {
        val drawer: DrawerLayout = mContext.findViewById(R.id.drawer_layout)
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START)
        }
    }
}