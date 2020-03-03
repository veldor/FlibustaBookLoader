package net.veldor.flibustaloader.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.adapters.DownloadScheduleAdapter;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.view_models.MainViewModel;

import java.util.ArrayList;


public class ActivityBookDownloadSchedule extends AppCompatActivity {
    private MainViewModel MyViewModel;
    private DownloadScheduleAdapter BooksAdapter;
    private View StopDownloadBtn;
    private View ContinueDownloadBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        setupUI();

        observeChanges();
    }

    private void observeChanges() {
        LiveData<Boolean> bookDownloaded = App.getInstance().BookDownloaded;
        bookDownloaded.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean downloaded) {
                if (downloaded) {
                    BooksAdapter.notifyItemRemoved(0);
                    // если книг в очереди не осталось- завершаю активность
                    if (BooksAdapter.mBooks.size() == 0 ||App.getInstance().mDownloadSchedule.getValue() == null || App.getInstance().mDownloadSchedule.getValue().size() == 0) {
                        Toast.makeText(ActivityBookDownloadSchedule.this, R.string.books_downloaded_message, Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }
        });

        LiveData<WorkInfo> workerStatus = App.getInstance().mDownloadAllWork;
        if(workerStatus != null){
            workerStatus.observe(this, new Observer<WorkInfo>() {
                @Override
                public void onChanged(WorkInfo workInfo) {
                    if(workInfo.getState() != WorkInfo.State.RUNNING){
                        workStopped();
                    }
                }
            });
        }

        LiveData<Boolean> downloadInterrupted = App.getInstance().DownloadInterrupted;
        downloadInterrupted.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean interrupted) {
                if(interrupted){
                    // если ещё остались результаты- отмечу, что работа приостановлена, иначе- закрою Activity
                    if(App.getInstance().mDownloadSchedule.getValue() == null || App.getInstance().mDownloadSchedule.getValue().size() == 0){
                        finish();
                    }
                    else{
                        workStopped();
                    }
                }
            }
        });

    }

    private void workStopped() {
        StopDownloadBtn.setVisibility(View.GONE);
        ContinueDownloadBtn.setVisibility(View.VISIBLE);
    }

    private void setupUI() {
        LiveData<WorkInfo> statusContainer = App.getInstance().mDownloadAllWork;

        setContentView(R.layout.activity_download_schedule);
        // активирую кнопку возвращения к предыдущему окну
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // проверю, есть ли книги в очереди скачивания
        ArrayList<FoundedBook> books = App.getInstance().mDownloadSchedule.getValue();
        if(books == null || books.size() == 0){
            Toast.makeText(this, "Downloads schedule empty", Toast.LENGTH_LONG).show();
            finish();
        }
        RecyclerView recycler = findViewById(R.id.booksList);
        BooksAdapter = new DownloadScheduleAdapter(App.getInstance().mDownloadSchedule.getValue());
        BooksAdapter.setHasStableIds(true);
        recycler.setAdapter(BooksAdapter);
        recycler.setLayoutManager(new LinearLayoutManager(ActivityBookDownloadSchedule.this));

        // найду кнопку остановки скачивания
        StopDownloadBtn = findViewById(R.id.stopMassDownload);
        ContinueDownloadBtn = findViewById(R.id.continueMassDownload);
        if(statusContainer != null && statusContainer.getValue() != null && statusContainer.getValue().getState() == WorkInfo.State.RUNNING){
            StopDownloadBtn.setVisibility(View.GONE);
            ContinueDownloadBtn.setVisibility(View.VISIBLE);

        }
        if (StopDownloadBtn != null) {
            StopDownloadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("surprise", "ActivityBookDownloadSchedule onClick: work cancelled");
                    MyViewModel.cancelMassDownload();
                    App.getInstance().getNotificator().cancelBookLoadNotification();
                    App.getInstance().getNotificator().createMassDownloadStoppedNotification();
                    StopDownloadBtn.setVisibility(View.GONE);
                    Toast.makeText(ActivityBookDownloadSchedule.this, "Загрузка книг остановлена", Toast.LENGTH_LONG).show();
                    if(ContinueDownloadBtn != null){
                        ContinueDownloadBtn.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
        if(ContinueDownloadBtn != null){
            ContinueDownloadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MyViewModel.initiateMassDownload();
                    ContinueDownloadBtn.setVisibility(View.GONE);
                    if(StopDownloadBtn != null){
                        StopDownloadBtn.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}