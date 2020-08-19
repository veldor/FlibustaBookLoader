package net.veldor.flibustaloader.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
        switch (itemId){
            case R.id.goToWebView:
                App.getInstance().setView(App.VIEW_WEB);
                Intent intent = new Intent(mContext, WebViewActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mContext.startActivity(intent);
                mContext.finish();
                break;
            case R.id.goToOPDS:
                App.getInstance().setView(App.VIEW_ODPS);
                intent = new Intent(mContext, OPDSActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mContext.startActivity(intent);
                mContext.finish();
                break;
            case R.id.goToDownloadsList:
                intent = new Intent(mContext, ActivityBookDownloadSchedule.class);
                mContext.startActivity(intent);
                tryCloseDrawer();
                break;
            case R.id.goToSubscriptions:
                intent = new Intent(mContext, SubscriptionsActivity.class);
                mContext.startActivity(intent);
                tryCloseDrawer();
                break;
            case R.id.goToBookmarks:
                intent = new Intent(mContext, BookmarksActivity.class);
                mContext.startActivity(intent);
                tryCloseDrawer();
                break;
            case R.id.goToBlacklist:
                intent = new Intent(mContext, BlacklistActivity.class);
                mContext.startActivity(intent);
                tryCloseDrawer();
                break;
            case R.id.goToSettings:
                intent = new Intent(mContext, SettingsActivity.class);
                mContext.startActivity(intent);
                tryCloseDrawer();
                break;
            case R.id.buyCoffee:
                new DonationDialog.Builder(mContext).build().show();
                tryCloseDrawer();
                break;
            case R.id.testAppInvite:
                Log.d("surprise", "NavigatorSelectHandler onNavigationItemSelected 66: selected append test");
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://t.me/flibusta_downloader_beta"));
                mContext.startActivity(intent);
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
