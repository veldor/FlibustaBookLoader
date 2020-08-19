package net.veldor.flibustaloader.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.adapters.BlacklistAdapter;
import net.veldor.flibustaloader.selections.BlacklistItem;
import net.veldor.flibustaloader.utils.BlacklistAuthors;
import net.veldor.flibustaloader.utils.BlacklistBooks;
import net.veldor.flibustaloader.utils.BlacklistGenres;
import net.veldor.flibustaloader.utils.BlacklistSequences;
import net.veldor.flibustaloader.utils.MyPreferences;

import java.util.ArrayList;

public class BlacklistActivity extends BaseActivity {

    private RadioGroup mRadioContainer;
    private BlacklistAdapter mBlacklistAdapter;
    private RecyclerView mRecycler;
    private BlacklistBooks mBooksBlacklistContainer;
    private EditText mInput;
    private BlacklistAuthors mAuthorsBlacklistContainer;
    private BlacklistSequences mSequencesBlacklistContainer;
    private BlacklistGenres mGenresBlacklistContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_blacklist_activity);

        setupUi();
        setupObservers();
    }

    private void setupUi() {
        setupInterface();

        // скрою переход на данное активити
        Menu menuNav = mNavigationView.getMenu();
        MenuItem item = menuNav.findItem(R.id.goToBlacklist);
        item.setEnabled(false);
        item.setChecked(true);
        // добавлю идентификатор строки поиска
        mInput = findViewById(R.id.blacklist_name);
        mRecycler = findViewById(R.id.resultsList);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));

        mBooksBlacklistContainer = App.getInstance().getBooksBlacklist();
        mAuthorsBlacklistContainer = App.getInstance().getAuthorsBlacklist();
        mSequencesBlacklistContainer = App.getInstance().getSequencesBlacklist();
        mGenresBlacklistContainer = App.getInstance().getGenresBlacklist();
        showBooks();
        // отслежу переключение типа добавления
        mRadioContainer = findViewById(R.id.blacklist_type);
        if (mRadioContainer != null) {
            mRadioContainer.setOnCheckedChangeListener((group, checkedId) -> {
                switch (checkedId) {
                    case R.id.blacklistBook:
                        showBooks();
                        break;
                    case R.id.blacklistAuthor:
                        showAuthors();
                        break;
                    case R.id.blacklistSequence:
                        showSequences();
                        break;
                    case R.id.blacklistGenre:
                        showGenres();
                        break;
                }
            });
        }

        Button subscribeBtn = findViewById(R.id.add_to_blacklist_btn);
        if (subscribeBtn != null) {
            subscribeBtn.setOnClickListener(this::addToBlacklist);
        }

        Switch switchOnlyRussian = findViewById(R.id.switchOnlyRussian);
        switchOnlyRussian.setChecked(MyPreferences.getInstance().isOnlyRussian());
        switchOnlyRussian.setOnCheckedChangeListener((buttonView, isChecked) -> MyPreferences.getInstance().setOnlyRussian(isChecked));
    }

    public void addToBlacklist(View view) {
        String value = mInput.getText().toString().trim();
        if (!value.isEmpty()) {
            // добавлю подписку в зависимости от типа
            switch (mRadioContainer.getCheckedRadioButtonId()) {
                case R.id.blacklistBook:
                    mBooksBlacklistContainer.addValue(value);
                    break;
                case R.id.blacklistAuthor:
                    mAuthorsBlacklistContainer.addValue(value);
                    break;
                case R.id.blacklistSequence:
                    mSequencesBlacklistContainer.addValue(value);
                    break;
                case R.id.blacklistGenre:
                    mGenresBlacklistContainer.addValue(value);
                    break;

            }
            mInput.setText("");
            Toast.makeText(this, "Добавляю значение " + value, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Введите значение", Toast.LENGTH_LONG).show();
            mInput.requestFocus();
        }
    }

    private void showBooks() {
        // получу подписки на книги
        ArrayList<BlacklistItem> autocompleteValues = mBooksBlacklistContainer.getBlacklist();
        mBlacklistAdapter = new BlacklistAdapter(autocompleteValues);
        mRecycler.setAdapter(mBlacklistAdapter);
    }

    private void showAuthors() {
        // получу подписки на авторов
        ArrayList<BlacklistItem> autocompleteValues = mAuthorsBlacklistContainer.getBlacklist();
        mBlacklistAdapter = new BlacklistAdapter(autocompleteValues);
        mRecycler.setAdapter(mBlacklistAdapter);
    }

    private void showSequences() {
        // получу чёрный список серий
        ArrayList<BlacklistItem> autocompleteValues = mSequencesBlacklistContainer.getBlacklist();
        mBlacklistAdapter = new BlacklistAdapter(autocompleteValues);
        mRecycler.setAdapter(mBlacklistAdapter);
    }
    private void showGenres() {
        // получу чёрный список жанров
        ArrayList<BlacklistItem> autocompleteValues = mGenresBlacklistContainer.getBlacklist();
        mBlacklistAdapter = new BlacklistAdapter(autocompleteValues);
        mRecycler.setAdapter(mBlacklistAdapter);
    }

    @Override
    protected void setupObservers() {
        super.setupObservers();
        Log.d("surprise", "BlacklistActivity.java 95 setupObservers: setting up observers");
        // буду отслеживать изменения списка книг
        LiveData<Boolean> refresh = BlacklistBooks.mListRefreshed;
        refresh.observe(this, aBoolean -> {
            if (aBoolean != null && aBoolean && mRadioContainer.getCheckedRadioButtonId() == R.id.blacklistBook) {
                refreshBooksBlacklist();
            }
        });


        // буду отслеживать изменения списка авторов
        LiveData<Boolean> authorRefresh = BlacklistAuthors.mListRefreshed;
        authorRefresh.observe(this, aBoolean -> {
            if (aBoolean != null && aBoolean && mRadioContainer.getCheckedRadioButtonId() == R.id.blacklistAuthor) {
                refreshAuthorBlacklist();
            }
        });

        // буду отслеживать изменения списка серий
        LiveData<Boolean> sequencesRefresh = BlacklistSequences.mListRefreshed;
        sequencesRefresh.observe(this, aBoolean -> {
            if (aBoolean != null && aBoolean && mRadioContainer.getCheckedRadioButtonId() == R.id.blacklistSequence) {
                refreshSequencesBlacklist();
            }
        });

        // буду отслеживать изменения списка жанров
        LiveData<Boolean> genresRefresh = BlacklistGenres.mListRefreshed;
        genresRefresh.observe(this, aBoolean -> {
            if (aBoolean != null && aBoolean && mRadioContainer.getCheckedRadioButtonId() == R.id.blacklistGenre) {
                refreshGenresBlacklist();
            }
        });
    }


    private void refreshBooksBlacklist() {
        ArrayList<BlacklistItem> autocompleteValues = mBooksBlacklistContainer.getBlacklist();
        mBlacklistAdapter.changeList(autocompleteValues);
        mBlacklistAdapter.notifyDataSetChanged();
    }

    private void refreshAuthorBlacklist() {
        ArrayList<BlacklistItem> blacklist = mAuthorsBlacklistContainer.getBlacklist();
        mBlacklistAdapter.changeList(blacklist);
        mBlacklistAdapter.notifyDataSetChanged();
    }

    private void refreshSequencesBlacklist() {
        ArrayList<BlacklistItem> blacklist = mSequencesBlacklistContainer.getBlacklist();
        mBlacklistAdapter.changeList(blacklist);
        mBlacklistAdapter.notifyDataSetChanged();
    }

    private void refreshGenresBlacklist() {
        ArrayList<BlacklistItem> blacklist = mGenresBlacklistContainer.getBlacklist();
        mBlacklistAdapter.changeList(blacklist);
        mBlacklistAdapter.notifyDataSetChanged();
    }
}