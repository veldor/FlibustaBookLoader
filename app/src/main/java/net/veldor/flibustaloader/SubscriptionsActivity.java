package net.veldor.flibustaloader;

import android.app.Dialog;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.adapters.SubscribeResultsAdapter;
import net.veldor.flibustaloader.dialogs.GifDialog;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.workers.LoadSubscriptionsWorker;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class SubscriptionsActivity extends AppCompatActivity {

    private static final String MY_TAG = "load_subscribes";
    private Dialog mLoadDialog;
    private SubscribeResultsAdapter mAdapter;
    private AlertDialog.Builder mDownloadsDialog;
    private Snackbar mBookLoadNotificaton;
    private View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribtions);

        mRootView = findViewById(R.id.rootView);

        RecyclerView recycler = findViewById(R.id.subscribe_items_list);
        recycler.setLayoutManager(new LinearLayoutManager(SubscriptionsActivity.this));
        mAdapter = new SubscribeResultsAdapter();
        recycler.setAdapter(mAdapter);

        // добавлю заглушку
        showLoadDialog();
        // запущу рабочего, загружающего книги
        OneTimeWorkRequest checkSubs = new OneTimeWorkRequest.Builder(LoadSubscriptionsWorker.class).addTag(MY_TAG).setInitialDelay(10, TimeUnit.MILLISECONDS).build();
        Log.d("surprise", "MainActivity onCreate work planned");
        WorkManager.getInstance().enqueueUniqueWork(MY_TAG, ExistingWorkPolicy.REPLACE, checkSubs);
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
                        MyWebClient mWebClient = new MyWebClient();
                        mWebClient.download(downloadLinks.get(0));
                        showBookLoadNotification();
                    } else {
                        // покажу диалог для выбора ссылки для скачивания
                        showDownloadsDialog(downloadLinks);
                    }
                }
            }
        });

        // добавлю отслеживание неудачно загруженной книги
        LiveData<String> unloadedBook = App.getInstance().mUnloadedBook;
        unloadedBook.observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (s != null && !s.isEmpty()) {
                    // не удалось загрузить книгу
                    Toast.makeText(SubscriptionsActivity.this, "Не удалось сохранить " + s, Toast.LENGTH_LONG).show();
                    hideBookLoadNotification();
                }
            }
        });

        // отслеживание прогресса загрузки книги
        LiveData<Boolean> bookLoadStatus = App.getInstance().mDownloadProgress;
        bookLoadStatus.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if (aBoolean != null && !aBoolean) {
                    hideBookLoadNotification();
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
                        MyWebClient mWebClient = new MyWebClient();
                        mWebClient.download(item);
                        Toast.makeText(SubscriptionsActivity.this, "Загрузка началась", Toast.LENGTH_LONG).show();
                        showBookLoadNotification();

                        break;
                    }
                    counter++;
                }
            }
        });
        mDownloadsDialog.show();
    }

    private void showBookLoadNotification() {
        mBookLoadNotificaton = Snackbar.make(mRootView, "Загружаю книгу", Snackbar.LENGTH_INDEFINITE);
        mBookLoadNotificaton.setAction(getString(R.string.cancel), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorkManager.getInstance().cancelAllWorkByTag(MyWebClient.DOWNLOAD_BOOK_WORKER);
                Toast.makeText(SubscriptionsActivity.this, "Загрузка книги отменена", Toast.LENGTH_SHORT).show();
            }
        });
        mBookLoadNotificaton.show();
    }
    private void hideBookLoadNotification() {
        if (mBookLoadNotificaton != null) {
            mBookLoadNotificaton.dismiss();
        }
    }
}
