package net.veldor.flibustaloader.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.navigation.NavigationView;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.utils.MyPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Objects;

import static net.veldor.flibustaloader.utils.MyFileReader.SUBSCRIPTIONS_FILE;

public class BaseActivity extends AppCompatActivity {

    public static final MutableLiveData<Boolean> sLiveDownloadScheduleCount = new MutableLiveData<>();
    public static final MutableLiveData<Boolean> sLiveFoundedSubscriptionsCount = new MutableLiveData<>();

    private TextView mDownloadsListTextView;
    private TextView mSubscriptionsListTextView;
    protected NavigationView mNavigationView;
    private DrawerLayout mDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupObservers();
    }

    protected void setupInterface(){
        // включу аппаратное ускорение, если оно активно
        if (MyPreferences.getInstance().isHardwareAcceleration()) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        // включу поддержку тулбара
        Toolbar toolbar = findViewById(R.id.toolbar);
        if(toolbar != null){
            setSupportActionBar(toolbar);
        }
        // покажу гамбургер :)
        mDrawer = findViewById(R.id.drawer_layout);
        if(mDrawer != null){
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            mDrawer.addDrawerListener(toggle);
            toggle.setDrawerIndicatorEnabled(true);
            toggle.syncState();

            mNavigationView = findViewById(R.id.nav_view);
            mNavigationView.setNavigationItemSelectedListener(new NavigatorSelectHandler(this));
            // отображу бейджи в меню
            mNavigationView.getMenu().findItem(R.id.goToDownloadsList).getActionView();
            mDownloadsListTextView = (TextView) mNavigationView.getMenu().
                    findItem(R.id.goToDownloadsList).getActionView();
            mSubscriptionsListTextView = (TextView) mNavigationView.getMenu().
                    findItem(R.id.goToSubscriptions).getActionView();
            // метод для счетчиков
            initializeCountDrawer();
        }
    }

    private void initializeCountDrawer() {
        setDownloadsLength();
        setSubscriptionsLength();
    }


    protected void changeTitle(String s) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(getSupportActionBar()).setTitle(s);
        }
    }

    @Override
    public void onBackPressed() {
        if(mDrawer != null){
            if (mDrawer.isDrawerOpen(GravityCompat.START)) {
                mDrawer.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        }
    }

    protected void setupObservers(){
        // Отслежу изменение числа книг в очереди закачек
        sLiveDownloadScheduleCount.observe(this, changed -> {
            if(changed){
                setDownloadsLength();
            }
        });
        // Отслежу изменение числа книг в очереди закачек
        sLiveFoundedSubscriptionsCount.observe(this, changed -> {
            if(changed){
                setSubscriptionsLength();
            }
        });
    }

    private void setSubscriptionsLength() {
        // получу размер очереди подписок
        File autocompleteFile = new File(App.getInstance().getFilesDir(), SUBSCRIPTIONS_FILE);
        try {
            if (autocompleteFile.exists()) {
                FileInputStream fis = new FileInputStream(autocompleteFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                @SuppressWarnings("rawtypes") ArrayList list = (ArrayList) ois.readObject();
                int listLength = list.size();
                Log.d("surprise", "OPDSActivity initializeCountDrawer 577: founded result size is " + listLength);
                if (listLength > 0) {
                    mSubscriptionsListTextView.setGravity(Gravity.CENTER_VERTICAL);
                    mSubscriptionsListTextView.setTypeface(null, Typeface.BOLD);
                    mSubscriptionsListTextView.setTextColor(getResources().getColor(R.color.book_name_color));
                    mSubscriptionsListTextView.setText(String.valueOf(listLength));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDownloadsLength() {
        int queueSize = App.getInstance().mDatabase.booksDownloadScheduleDao().getQueueSize();
        if (queueSize > 0) {
            mDownloadsListTextView.setVisibility(View.VISIBLE);
            mDownloadsListTextView.setGravity(Gravity.CENTER_VERTICAL);
            mDownloadsListTextView.setTypeface(null, Typeface.BOLD);
            mDownloadsListTextView.setTextColor(getResources().getColor(R.color.book_name_color));
            mDownloadsListTextView.setText(String.valueOf(queueSize));
        }
        else{
            mDownloadsListTextView.setVisibility(View.INVISIBLE);
        }
    }


    public static class ResetApp implements Runnable {
        @Override
        public void run() {
            Intent intent = new Intent(App.getInstance(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            App.getInstance().startActivity(intent);
            Runtime.getRuntime().exit(0);
        }
    }
}