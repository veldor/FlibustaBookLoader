package net.veldor.flibustaloader;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.adapters.SubscribesAdapter;
import net.veldor.flibustaloader.selections.SubscriptionItem;
import net.veldor.flibustaloader.utils.SubscribeBooks;
import net.veldor.flibustaloader.workers.CheckSubsctiptionsWorker;

import java.util.ArrayList;

public class SubscribeActivity extends AppCompatActivity {

    private EditText mSubsctibeInput;
    private SubscribeBooks mBooksSubscribeContainer;
    private SubscribesAdapter mSubscribesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);

        // отслежу переключение типа подписки
        RadioGroup radioContainer = findViewById(R.id.subscribe_type);
        radioContainer.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.searchBook:
                        Log.d("surprise", "SubscribeActivity onCheckedChanged book");
                        break;
                    case R.id.searchAuthor:
                        Log.d("surprise", "SubscribeActivity onCheckedChanged author");
                        break;
                }
            }
        });

        RecyclerView recycler = findViewById(R.id.subscrtibe_items_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // получу подписки на книги
        mBooksSubscribeContainer = App.getInstance().getBooksSubscribe();
        ArrayList<SubscriptionItem> autocompleteValues = mBooksSubscribeContainer.getSubscribes();
        Log.d("surprise", "SubscribeActivity onCreate " + autocompleteValues.size());
        mSubscribesAdapter = new SubscribesAdapter(autocompleteValues);
        recycler.setAdapter(mSubscribesAdapter);

        // буду отслеживать изменения списка книг
        LiveData<Boolean> refresh = mBooksSubscribeContainer.mListRefreshed;
        refresh.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if(aBoolean != null && aBoolean){
                    Log.d("surprise", "SubscribeActivity onChanged data changed");
                    refreshSubscriptionList();
                }
            }
        });

        // обработаю добавление книги в список загрузки
        // добавлю идентификатор строки поиска
        mSubsctibeInput = findViewById(R.id.subscribe_name);
        mSubsctibeInput.requestFocus();
    }

    private void refreshSubscriptionList() {
        ArrayList<SubscriptionItem> autocompleteValues = mBooksSubscribeContainer.getSubscribes();
        mSubscribesAdapter.changeList(autocompleteValues);
        mSubscribesAdapter.notifyDataSetChanged();

    }

    public void addSubscribe(View view) {
        // получу содержимое строки ввода
        String value = mSubsctibeInput.getText().toString().trim();
        if(!value.isEmpty()){
            mBooksSubscribeContainer.addValue(value);
            mSubsctibeInput.setText("");
            Toast.makeText(this, "Добавляю значение " + value, Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(this, "Введите название книги для подписки", Toast.LENGTH_LONG).show();
            mSubsctibeInput.requestFocus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // проверю новые поступления
        OneTimeWorkRequest worker = new OneTimeWorkRequest.Builder(CheckSubsctiptionsWorker.class).build();
        WorkManager.getInstance().enqueue(worker);
    }
}