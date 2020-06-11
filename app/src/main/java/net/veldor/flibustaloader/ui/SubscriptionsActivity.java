package net.veldor.flibustaloader.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import net.veldor.flibustaloader.R;

public class SubscriptionsActivity extends BaseActivity {

    public static final String START_FRAGMENT = "start fragment";
    public static final int START_RESULTS = 1;
    private BottomNavigationView mBottomNavView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.new_subscriptions_activity);

        setupInterface();


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

    @Override
    protected void setupInterface() {
        super.setupInterface();
        // скрою переход на данное активити
        Menu menuNav = mNavigationView.getMenu();
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
