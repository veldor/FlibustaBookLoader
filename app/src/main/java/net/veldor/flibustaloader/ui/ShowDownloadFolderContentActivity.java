package net.veldor.flibustaloader.ui;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.adapters.DirContentAdapter;
import net.veldor.flibustaloader.selections.Book;
import net.veldor.flibustaloader.utils.FilesHandler;
import net.veldor.flibustaloader.utils.Grammar;
import net.veldor.flibustaloader.utils.MyPreferences;

import java.io.File;
import java.util.ArrayList;

public class ShowDownloadFolderContentActivity extends BaseActivity {

    private RecyclerView recycler;
    private static final String[] bookSortOptions = new String[]{"По названию книги", "По размеру", "По автору", "По формату", "По дате загрузки"};
    private DirContentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_download_folder_content);
        setupInterface();
        // получу recyclerView
        recycler = findViewById(R.id.showDirContent);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        // получу список файлов из папки
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile downloadsDir = App.getInstance().getDownloadDir();
            if (downloadsDir != null && downloadsDir.isDirectory()) {
                DocumentFile[] files = downloadsDir.listFiles();
                ArrayList<Book> books = recursiveScan(files, "");
                Log.d("surprise", "ShowDownloadFolderContentActivity onCreate 37: books size is " + books.size());
                if (books.size() > 0) {
                    adapter = new DirContentAdapter(books);
                    recycler.setAdapter(adapter);
                }
            }
        } else {
            File downloadDir = MyPreferences.getInstance().getDownloadDir();
            if(downloadDir != null && downloadDir.isDirectory()){
                File[] files = downloadDir.listFiles();
                ArrayList<Book> books = recursiveScan(files, "");
                Log.d("surprise", "ShowDownloadFolderContentActivity onCreate 57: len is " + books.size());
                if (books.size() > 0) {
                    adapter = new DirContentAdapter(books);
                    recycler.setAdapter(adapter);
                }
            }
        }
    }

    @Override
    protected void setupInterface() {
        super.setupInterface();

        // скрою переход на данное активити
        Menu menuNav = mNavigationView.getMenu();
        MenuItem item = menuNav.findItem(R.id.goToFileList);
        item.setEnabled(false);
        item.setChecked(true);
    }

    private ArrayList<Book> recursiveScan(DocumentFile[] files, String prefix) {
        ArrayList<Book> answer = new ArrayList<>();
        if (files != null && files.length > 0) {
            Book bookItem;
            String value;
            String value1;
            for (DocumentFile df :
                    files) {
                if (df.isFile()) {
                    value = df.getName();
                    if (value != null) {
                        bookItem = new Book();
                        // получу имя автора- это значение до первого слеша
                        int index = value.indexOf("_");
                        int lastIndex = value.lastIndexOf("_");
                        if(index > 0 && lastIndex > 0 && index != lastIndex){
                            value1 = value.substring(0, index);
                            bookItem.author = prefix + value1;
                            value1 = value.substring(index + 1, lastIndex);
                            bookItem.name = value1;
                        } else {
                            bookItem.author = prefix + "Неизвестно";
                            bookItem.name = value;
                        }
                        bookItem.size = Grammar.getLiteralSize(df.length());
                        bookItem.file = df;
                        bookItem.extension = Grammar.getExtension(value);
                        answer.add(bookItem);
                    }
                } else if (df.isDirectory()) {
                    answer.addAll(recursiveScan(df.listFiles(), prefix + df.getName() + "/"));
                }
            }
        }
        return answer;
    }
    private ArrayList<Book> recursiveScan(File[] files, String prefix) {
        ArrayList<Book> answer = new ArrayList<>();
        if (files != null && files.length > 0) {
            Book bookItem;
            String value;
            String value1;
            for (File df :
                    files) {
                if (df.isFile()) {
                    value = df.getName();
                    if (value != null) {
                        bookItem = new Book();
                        // получу имя автора- это значение до первого слеша
                        int index = value.indexOf("_");
                        int lastIndex = value.lastIndexOf("_");
                        if(index > 0 && lastIndex > 0 && index != lastIndex){
                            value1 = value.substring(0, index);
                            bookItem.author = prefix + value1;
                            value1 = value.substring(index + 1, lastIndex);
                            bookItem.name = value1;
                        } else {
                            bookItem.author = prefix + "Неизвестно";
                            bookItem.name = value;
                        }
                        bookItem.size = Grammar.getLiteralSize(df.length());
                        bookItem.fileCompat = df;
                        bookItem.extension = Grammar.getExtension(value);
                        answer.add(bookItem);
                    }
                } else if (df.isDirectory()) {
                    answer.addAll(recursiveScan(df.listFiles(), prefix + df.getName() + "/"));
                }
            }
        }
        return answer;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.d("surprise", "ShowDownloadFolderContentActivity onContextItemSelected 87: handle " + item.getTitle());
        int position;
        try {
            DirContentAdapter adapter = (DirContentAdapter) recycler.getAdapter();
            if (adapter != null) {
                position = adapter.getPosition();
                Book book = adapter.getItem(position);
                if (item.getTitle().equals(getString(R.string.share_link_message))) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        FilesHandler.shareFile(book.file);
                    }
                } else if (item.getTitle().equals(getString(R.string.delete_item_message))) {
                    adapter.delete(book);
                    Toast.makeText(ShowDownloadFolderContentActivity.this, getString(R.string.item_deleted_message), Toast.LENGTH_LONG).show();
                } else if (item.getTitle().equals(getString(R.string.open_with_menu_item))) {
                    Log.d("surprise", "ShowDownloadFolderContentActivity onContextItemSelected 105: open file");
                    FilesHandler.openFile(book.file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.loaded_books_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_sort_by) {
            selectSorting();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectSorting() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.MyDialogStyle);
        dialog.setTitle("Выберите тип сортировки")
                .setItems(bookSortOptions, (dialog1, which) -> {
                    if (adapter != null) adapter.sort(which);
                });
        // покажу список типов сортировки
        if (!ShowDownloadFolderContentActivity.this.isFinishing()) {
            dialog.show();
        }
    }
}