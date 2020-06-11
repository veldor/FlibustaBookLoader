package net.veldor.flibustaloader.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.adapters.DownloadScheduleAdapter;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.view_models.MainViewModel;
import net.veldor.flibustaloader.workers.DownloadBooksWorker;

import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;


public class ActivityBookDownloadSchedule extends BaseActivity {
    private MainViewModel MyViewModel;
    private DownloadScheduleAdapter BooksAdapter;
    private View StopDownloadBtn;
    private View ContinueDownloadBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.new_download_schedule_activity);

        setupInterface();

        MyViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        observeChanges();
    }

    private void observeChanges() {
        // получу состояние загрузки
        LiveData<List<WorkInfo>> loadProgress = DownloadBooksWorker.getDownloadProgress();
        loadProgress.observe(this, workInfos -> {
            if(workInfos != null && workInfos.size() > 0){
                // получу статус закачки
                WorkInfo work = workInfos.get(0);
                if(work != null){
                    switch (work.getState()){
                        case CANCELLED:
                        case SUCCEEDED:
                        case FAILED:
                        case BLOCKED:
                            showContinue();
                            break;
                        case RUNNING:
                        case ENQUEUED:
                            showStop();
                            break;
                    }
                }
            }
        });
    }

    private void showContinue() {
        StopDownloadBtn.setVisibility(View.GONE);
        ContinueDownloadBtn.setVisibility(View.VISIBLE);
    }
    private void showStop() {
        StopDownloadBtn.setVisibility(View.VISIBLE);
        ContinueDownloadBtn.setVisibility(View.GONE);
    }

    @Override
    protected void setupInterface() {

        super.setupInterface();

        // скрою переход на данное активити
        Menu menuNav = mNavigationView.getMenu();
        MenuItem item = menuNav.findItem(R.id.goToDownloadsList);
        item.setEnabled(false);
        item.setChecked(true);

        LiveData<WorkInfo> statusContainer = App.getInstance().mDownloadAllWork;

        // проверю, есть ли активный процесс загрузки
        boolean noActiveDownload = DownloadBooksWorker.noActiveDownloadProcess();

        // активирую кнопку возвращения к предыдущему окну
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // проверю, есть ли книги в очереди скачивания
        int queueSize = App.getInstance().mDatabase.booksDownloadScheduleDao().getQueueSize();
        if(queueSize == 0){
            Toast.makeText(this, R.string.download_schedule_empty_message, Toast.LENGTH_LONG).show();
            finish();
        }

        // получу данные о книгах в очереди в виде liveData
        final LiveData<List<BooksDownloadSchedule>> schedule = App.getInstance().mDatabase.booksDownloadScheduleDao().getAllBooksLive();
        schedule.observe(this, booksDownloadSchedules -> {
            if(booksDownloadSchedules != null && booksDownloadSchedules.size() > 0){
                if(BooksAdapter == null){
                    RecyclerView recycler = findViewById(R.id.resultsList);
                    BooksAdapter = new DownloadScheduleAdapter(booksDownloadSchedules);
                    BooksAdapter.setHasStableIds(true);
                    recycler.setAdapter(BooksAdapter);
                    recycler.setLayoutManager(new LinearLayoutManager(ActivityBookDownloadSchedule.this));
                }
                else{
                    BooksAdapter.setData(booksDownloadSchedules);
                    BooksAdapter.notifyDataSetChanged();
                }
            }
            else{
                finish();
            }
        });


        // найду кнопку остановки скачивания
        StopDownloadBtn = findViewById(R.id.stopMassDownload);
        ContinueDownloadBtn = findViewById(R.id.continueMassDownload);
        if(statusContainer != null && statusContainer.getValue() != null && statusContainer.getValue().getState() == WorkInfo.State.RUNNING){
            StopDownloadBtn.setVisibility(View.GONE);
            ContinueDownloadBtn.setVisibility(View.VISIBLE);

        }
        if (StopDownloadBtn != null) {
            StopDownloadBtn.setOnClickListener(view -> {
                Log.d("surprise", "ActivityBookDownloadSchedule onClick: work cancelled");
                MyViewModel.cancelMassDownload();
                App.getInstance().getNotificator().cancelBookLoadNotification();
                App.getInstance().getNotificator().createMassDownloadStoppedNotification();
                StopDownloadBtn.setVisibility(View.GONE);
                Toast.makeText(ActivityBookDownloadSchedule.this, "Загрузка книг остановлена", Toast.LENGTH_LONG).show();
                if(ContinueDownloadBtn != null){
                    ContinueDownloadBtn.setVisibility(View.VISIBLE);
                }
            });
        }


        if(ContinueDownloadBtn != null){
            ContinueDownloadBtn.setOnClickListener(view -> {
                MyViewModel.initiateMassDownload();
                ContinueDownloadBtn.setVisibility(View.GONE);
                if(StopDownloadBtn != null){
                    StopDownloadBtn.setVisibility(View.VISIBLE);
                }
            });
        }
        assert ContinueDownloadBtn != null;
        if(noActiveDownload){
            ContinueDownloadBtn.setVisibility(View.VISIBLE);
            StopDownloadBtn.setVisibility(View.GONE);
        }
        else{
            ContinueDownloadBtn.setVisibility(View.GONE);
            StopDownloadBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, OPDSActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
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