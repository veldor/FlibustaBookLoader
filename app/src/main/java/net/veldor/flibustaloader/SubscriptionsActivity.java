package net.veldor.flibustaloader;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.adapters.SubscribeResultsAdapter;
import net.veldor.flibustaloader.dialogs.GifDialog;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.view_models.MainViewModel;
import net.veldor.flibustaloader.workers.LoadSubscriptionsWorker;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class SubscriptionsActivity extends AppCompatActivity {

    private static final String MY_TAG = "load_subscribes";
    private Dialog mLoadDialog;
    private SubscribeResultsAdapter mAdapter;
    private AlertDialog.Builder mDownloadsDialog;
    private MainViewModel mMyViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribtions);


        // добавлю viewModel
        mMyViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // переназову окно
        ActionBar actionbar = getActionBar();
        if(actionbar != null){
            actionbar.setTitle("Подписки");
        }

        RecyclerView recycler = findViewById(R.id.booksList);
        recycler.setLayoutManager(new LinearLayoutManager(SubscriptionsActivity.this));
        mAdapter = new SubscribeResultsAdapter();
        recycler.setAdapter(mAdapter);

        // добавлю заглушку
        showLoadDialog();
        // запущу рабочего, загружающего книги
        OneTimeWorkRequest checkSubs = new OneTimeWorkRequest.Builder(LoadSubscriptionsWorker.class).addTag(MY_TAG).setInitialDelay(10, TimeUnit.MILLISECONDS).build();
        Log.d("surprise", "MainActivity onCreate show subscribes work planned");
        WorkManager.getInstance(this).enqueueUniqueWork(MY_TAG, ExistingWorkPolicy.REPLACE, checkSubs);
        // отслежу добавление книг по подписке
        LiveData<ArrayList<FoundedBook>> books = App.getInstance().mSubscribeResults;
        books.observe(this, new Observer<ArrayList<FoundedBook>>() {
            @Override
            public void onChanged(@Nullable ArrayList<FoundedBook> foundedBooks) {
                if(foundedBooks != null && foundedBooks.size() > 0){
                    Log.d("surprise", "SubscriptionsActivity onChanged books size " + foundedBooks.size());
                    hideLoadDialog();
                    mAdapter.setContent(foundedBooks);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
        // добавлю отслеживание получения списка ссылок на скачивание
        LiveData<ArrayList<DownloadLink>> downloadLinks = App.getInstance().mDownloadLinksList;
        downloadLinks.observe(this, new Observer<ArrayList<DownloadLink>>() {
            @Override
            public void onChanged(@Nullable ArrayList<DownloadLink> downloadLinks) {
                if (downloadLinks != null && downloadLinks.size() > 0) {
                    if (downloadLinks.size() == 1) {
                        // поставлю книгу в очередь
                        mMyViewModel.addToDownloadQueue(downloadLinks.get(0));
                        Toast.makeText(SubscriptionsActivity.this, R.string.book_added_to_schedule_message, Toast.LENGTH_LONG).show();

                    } else {
                        // покажу диалог для выбора ссылки для скачивания
                        showDownloadsDialog(downloadLinks);
                    }
                }
            }
        });
    }

    private void hideLoadDialog() {
        if(mLoadDialog != null){
            mLoadDialog.dismiss();
        }
    }

    private void showLoadDialog() {
        if (mLoadDialog == null) {
            mLoadDialog = new GifDialog.Builder(this)
                    .setTitle(getString(R.string.download_subscribes_title))
                    .setMessage(getString(R.string.download_subscribes_msg))
                    .setGifResource(R.drawable.loading)   //Pass your Gif here
                    .isCancellable(false)
                    .build();
        }
        mLoadDialog.show();
    }

    private void showDownloadsDialog(final ArrayList<DownloadLink> downloadLinks) {
        if (mDownloadsDialog == null) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.downloads_dialog_header);
            mDownloadsDialog = dialogBuilder;
        }
        // получу список типов данных
        int linksLength = downloadLinks.size();
        final String[] linksArray = new String[linksLength];
        int counter = 0;
        String mime;
        while (counter < linksLength) {
            mime = downloadLinks.get(counter).mime;
            linksArray[counter] = MimeTypes.getMime(mime);
            counter++;
        }
        mDownloadsDialog.setItems(linksArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // начну грузить выбранный файл
                // получу сокращённый MIME
                String shortMime = linksArray[i];
                String longMime = MimeTypes.getFullMime(shortMime);
                int counter = 0;
                int linksLength = downloadLinks.size();
                DownloadLink item;
                while (counter < linksLength) {
                    item = downloadLinks.get(counter);
                    if (item.mime.equals(longMime)) {
                        mMyViewModel.addToDownloadQueue(item);
                        Toast.makeText(SubscriptionsActivity.this, R.string.book_added_to_schedule_message, Toast.LENGTH_LONG).show();
                        break;
                    }
                    counter++;
                }
            }
        });
        mDownloadsDialog.show();
    }
}
