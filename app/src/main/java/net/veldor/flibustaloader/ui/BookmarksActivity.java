package net.veldor.flibustaloader.ui;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.adapters.BookmarksAdapter;
import net.veldor.flibustaloader.database.entity.Bookmark;

import java.util.List;

public class BookmarksActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_bookmarks_activity);
        setupInterface();
    }

    @Override
    protected void setupInterface() {
        super.setupInterface();
        // скрою переход на данное активити
        Menu menuNav = mNavigationView.getMenu();
        MenuItem item = menuNav.findItem(R.id.goToBookmarks);
        item.setEnabled(false);
        item.setChecked(true);

        // получу список закладок
        List<Bookmark> bookmarksList = App.getInstance().mDatabase.bookmarksDao().getAllBookmarks();
        RecyclerView recycler = findViewById(R.id.resultsList);
        BookmarksAdapter adapter = new BookmarksAdapter(bookmarksList);
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(this));
    }
}