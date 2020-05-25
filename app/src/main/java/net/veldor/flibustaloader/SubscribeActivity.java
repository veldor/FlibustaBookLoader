package net.veldor.flibustaloader;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import android.app.ActionBar;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.adapters.SubscribesAdapter;
import net.veldor.flibustaloader.selections.SubscriptionItem;
import net.veldor.flibustaloader.utils.SubscribeAuthors;
import net.veldor.flibustaloader.utils.SubscribeBooks;
import net.veldor.flibustaloader.utils.SubscribeSequences;
import net.veldor.flibustaloader.workers.CheckSubscriptionsWorker;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class SubscribeActivity extends AppCompatActivity {

    private EditText mSubscribeInput;
    private SubscribeBooks mBooksSubscribeContainer;
    private SubscribesAdapter mSubscribesAdapter;
    private SubscribeAuthors mAuthorsSubscribeContainer;
    private RecyclerView mRecycler;
    private RadioGroup mRadioContainer;
    private SubscribeSequences mSequencesSubscribeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);


        // переназову окно
        ActionBar actionbar = getActionBar();
        if(actionbar != null){
            actionbar.setTitle("Подписки");
        }

        // отслежу переключение типа подписки
        mRadioContainer = findViewById(R.id.subscribe_type);
        mRadioContainer.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.searchBook:
                        showBooks();
                        break;
                    case R.id.searchAuthor:
                        showAuthors();
                        break;
                    case R.id.searchSequence:
                        showSequences();
                        break;
                }
            }
        });

        mRecycler = findViewById(R.id.resultsList);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));

        mBooksSubscribeContainer = App.getInstance().getBooksSubscribe();
        mAuthorsSubscribeContainer = App.getInstance().getAuthorsSubscribe();
        mSequencesSubscribeContainer = App.getInstance().getSequencesSubscribe();
        showBooks();



        // буду отслеживать изменения списка книг
        LiveData<Boolean> refresh = mBooksSubscribeContainer.mListRefreshed;
        refresh.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if(aBoolean != null && aBoolean && mRadioContainer.getCheckedRadioButtonId() == R.id.searchBook){
                    refreshSubscriptionList();
                }
            }
        });
        // буду отслеживать изменения списка авторов
        LiveData<Boolean> authorRefresh = mAuthorsSubscribeContainer.mListRefreshed;
        authorRefresh.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if(aBoolean != null && aBoolean && mRadioContainer.getCheckedRadioButtonId() == R.id.searchAuthor){
                    refreshAuthorSubscriptionList();
                }
            }
        });
        // буду отслеживать изменения списка серий
        LiveData<Boolean> sequenceRefresh = mSequencesSubscribeContainer.mListRefreshed;
        sequenceRefresh.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if(aBoolean != null && aBoolean && mRadioContainer.getCheckedRadioButtonId() == R.id.searchSequence){
                    refreshSequenceSubscriptionList();
                }
            }
        });

        // обработаю добавление книги в список загрузки
        // добавлю идентификатор строки поиска
        mSubscribeInput = findViewById(R.id.subscribe_name);
        mSubscribeInput.requestFocus();
    }

    private void refreshSequenceSubscriptionList() {
        ArrayList<SubscriptionItem> autocompleteValues = mSequencesSubscribeContainer.getSubscribes();
        mSubscribesAdapter.changeList(autocompleteValues);
        mSubscribesAdapter.notifyDataSetChanged();
    }

    private void showSequences() {
        // получу подписки на серии
        ArrayList<SubscriptionItem> autocompleteValues = mSequencesSubscribeContainer.getSubscribes();
        Log.d("surprise", "SubscribeActivity onCreate " + autocompleteValues.size());
        mSubscribesAdapter = new SubscribesAdapter(autocompleteValues);
        mRecycler.setAdapter(mSubscribesAdapter);
    }

    private void refreshAuthorSubscriptionList() {
        ArrayList<SubscriptionItem> autocompleteValues = mAuthorsSubscribeContainer.getSubscribes();
        mSubscribesAdapter.changeList(autocompleteValues);
        mSubscribesAdapter.notifyDataSetChanged();
    }

    private void showAuthors() {
        // получу подписки на авторов
        ArrayList<SubscriptionItem> autocompleteValues = mAuthorsSubscribeContainer.getSubscribes();
        Log.d("surprise", "SubscribeActivity onCreate " + autocompleteValues.size());
        mSubscribesAdapter = new SubscribesAdapter(autocompleteValues);
        mRecycler.setAdapter(mSubscribesAdapter);
    }

    private void showBooks() {
        // получу подписки на книги
        ArrayList<SubscriptionItem> autocompleteValues = mBooksSubscribeContainer.getSubscribes();
        Log.d("surprise", "SubscribeActivity onCreate " + autocompleteValues.size());
        mSubscribesAdapter = new SubscribesAdapter(autocompleteValues);
        mRecycler.setAdapter(mSubscribesAdapter);
    }

    private void refreshSubscriptionList() {
        ArrayList<SubscriptionItem> autocompleteValues = mBooksSubscribeContainer.getSubscribes();
        mSubscribesAdapter.changeList(autocompleteValues);
        mSubscribesAdapter.notifyDataSetChanged();

    }

    public void addSubscribe(View view) {
        // получу содержимое строки ввода
        String value = mSubscribeInput.getText().toString().trim();
        if(!value.isEmpty()){
            // добавлю подписку в зависимости от типа
            switch (mRadioContainer.getCheckedRadioButtonId()){
                case R.id.searchBook:
                    Log.d("surprise", "SubscribeActivity addSubscribe add book");
                    mBooksSubscribeContainer.addValue(value);
                    break;
                case R.id.searchAuthor:
                    Log.d("surprise", "SubscribeActivity addSubscribe add author");
                    mAuthorsSubscribeContainer.addValue(value);
                    break;
                case R.id.searchSequence:
                    Log.d("surprise", "SubscribeActivity addSubscribe add sequence");
                    mSequencesSubscribeContainer.addValue(value);
                    break;

            }
            mSubscribeInput.setText("");
            Toast.makeText(this, "Добавляю значение " + value, Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(this, "Введите значение для подписки", Toast.LENGTH_LONG).show();
            mSubscribeInput.requestFocus();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // проверю новые поступления
        OneTimeWorkRequest worker = new OneTimeWorkRequest.Builder(CheckSubscriptionsWorker.class).build();
        WorkManager.getInstance(this).enqueue(worker);
    }
}