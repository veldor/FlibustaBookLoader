package net.veldor.flibustaloader.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.dialogs.DonationDialog;

public class NavigatorSelectHandler implements NavigationView.OnNavigationItemSelectedListener{

    private final Activity mContext;

    public NavigatorSelectHandler(Activity context){
        mContext = context;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Log.d("surprise", "OPDSActivityNew onNavigationItemSelected 62: click navigation");
        int itemId = item.getItemId();
        if (itemId == R.id.goToWebView) {
            App.getInstance().setView(App.VIEW_WEB);
            Intent intent = new Intent(mContext, WebViewActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(intent);
            mContext.finish();
        } else if (itemId == R.id.goToOPDS) {
            Intent intent;
            App.getInstance().setView(App.VIEW_ODPS);
            intent = new Intent(mContext, OPDSActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(intent);
            mContext.finish();
        } else if (itemId == R.id.goToDownloadsList) {
            Intent intent;
            intent = new Intent(mContext, ActivityBookDownloadSchedule.class);
            mContext.startActivity(intent);
            tryCloseDrawer();
        } else if (itemId == R.id.goToSubscriptions) {
            Intent intent;
            intent = new Intent(mContext, SubscriptionsActivity.class);
            mContext.startActivity(intent);
            tryCloseDrawer();
        } else if (itemId == R.id.goToBookmarks) {
            Intent intent;
            intent = new Intent(mContext, BookmarksActivity.class);
            mContext.startActivity(intent);
            tryCloseDrawer();
        } else if (itemId == R.id.goToBlacklist) {
            Intent intent;
            intent = new Intent(mContext, BlacklistActivity.class);
            mContext.startActivity(intent);
            tryCloseDrawer();
        } else if (itemId == R.id.goToFileList) {
            Intent intent;
            intent = new Intent(mContext, ShowDownloadFolderContentActivity.class);
            mContext.startActivity(intent);
            tryCloseDrawer();
        } else if (itemId == R.id.goToSettings) {
            Intent intent;
            intent = new Intent(mContext, SettingsActivity.class);
            mContext.startActivity(intent);
            tryCloseDrawer();
        } else if (itemId == R.id.buyCoffee) {
            new DonationDialog.Builder(mContext).build().show();
            tryCloseDrawer();
        } else if (itemId == R.id.testAppInvite) {
            Intent intent;
            Log.d("surprise", "NavigatorSelectHandler onNavigationItemSelected 66: selected append test");
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://t.me/flibusta_downloader_beta"));
            mContext.startActivity(intent);
        }else if (itemId == R.id.exitApp) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mContext.finishAndRemoveTask();
                System.exit(0);
            }
            else{
                mContext.finishAffinity();
                System.exit(0);
            }
        }
        return false;
    }

    private void tryCloseDrawer() {
        DrawerLayout drawer = mContext.findViewById(R.id.drawer_layout);
        if(drawer != null){
            drawer.closeDrawer(GravityCompat.START);
        }
    }
}
