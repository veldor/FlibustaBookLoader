package net.veldor.flibustaloader.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.utils.MyPreferences;

public class SubscriptionsActivity extends AppCompatActivity {

    public static final String START_FRAGMENT = "start fragment";
    public static final int START_RESULTS = 1;
    private BottomNavigationView mBottomNavView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupUI();


        // проверю, если в интенте указано открыть целевой фрагмент- открою его
        Intent intent = getIntent();
        int startFragment = intent.getIntExtra(START_FRAGMENT, -1);
        if(startFragment >= 0){
            if (startFragment == START_RESULTS) {
                switchToResults();
            }
        }
    }

    private void switchToResults() {
        mBottomNavView.setSelectedItemId(R.id.navigation_subscribe_results);
    }

    private void setupUI() {
        if (MyPreferences.getInstance().isHardwareAcceleration()) {
            // включу аппаратное ускорение
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        setContentView(R.layout.new_subscriptions_activity);

        // включу поддержку тулбара
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // покажу гамбургер :)
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();

        // скрою переход на данное активити
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigatorSelectHandler(this));
        Menu menuNav=navigationView.getMenu();
        MenuItem item = menuNav.findItem(R.id.goToSubscriptions);
        item.setEnabled(false);
        item.setChecked(true);

        // активирую нижнее меню
        mBottomNavView = findViewById(R.id.bottom_nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_subscribe, R.id.navigation_subscribe_results)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(mBottomNavView, navController);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
