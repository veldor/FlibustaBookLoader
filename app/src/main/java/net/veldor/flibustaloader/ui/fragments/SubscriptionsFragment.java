package net.veldor.flibustaloader.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.adapters.SubscribesAdapter;
import net.veldor.flibustaloader.selections.SubscriptionItem;
import net.veldor.flibustaloader.utils.MyPreferences;
import net.veldor.flibustaloader.utils.SubscribeAuthors;
import net.veldor.flibustaloader.utils.SubscribeBooks;
import net.veldor.flibustaloader.utils.SubscribeSequences;
import net.veldor.flibustaloader.view_models.SubscriptionsViewModel;

import java.util.ArrayList;

public class SubscriptionsFragment extends Fragment {

    private View mRoot;
    private EditText mSubscribeInput;
    private SubscriptionsViewModel mViewModel;
    private RadioGroup mRadioContainer;
    private RecyclerView mRecycler;


    private SubscribeBooks mBooksSubscribeContainer;
    private SubscribesAdapter mSubscribesAdapter;
    private SubscribeAuthors mAuthorsSubscribeContainer;
    private SubscribeSequences mSequencesSubscribeContainer;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mViewModel = new ViewModelProvider(this).get(SubscriptionsViewModel.class);

        mRoot = inflater.inflate(R.layout.fragment_subscribe, container, false);

        if(mRoot != null){
            Log.d("surprise", "SubscriptionsFragment onCreate 32: setup ui");
            setupUI();

            FragmentActivity activity = getActivity();

            if(activity != null){
                // буду отслеживать изменения списка книг
                LiveData<Boolean> refresh = mBooksSubscribeContainer.mListRefreshed;
                refresh.observe(getActivity(), aBoolean -> {
                    if(aBoolean != null && aBoolean && mRadioContainer.getCheckedRadioButtonId() == R.id.searchBook){
                        refreshSubscriptionList();
                    }
                });

                // буду отслеживать изменения списка авторов
                LiveData<Boolean> authorRefresh = mAuthorsSubscribeContainer.mListRefreshed;
                authorRefresh.observe(getActivity(), aBoolean -> {
                    if(aBoolean != null && aBoolean && mRadioContainer.getCheckedRadioButtonId() == R.id.searchAuthor){
                        refreshAuthorSubscriptionList();
                    }
                });

                // буду отслеживать изменения списка серий
                LiveData<Boolean> sequenceRefresh = mSequencesSubscribeContainer.mListRefreshed;
                sequenceRefresh.observe(getActivity(), aBoolean -> {
                    if(aBoolean != null && aBoolean && mRadioContainer.getCheckedRadioButtonId() == R.id.searchSequence){
                        refreshSequenceSubscriptionList();
                    }
                });

            }
        }
        return mRoot;
    }

    private void refreshAuthorSubscriptionList() {
        ArrayList<SubscriptionItem> autocompleteValues = mAuthorsSubscribeContainer.getSubscribes();
        mSubscribesAdapter.changeList(autocompleteValues);
        mSubscribesAdapter.notifyDataSetChanged();
    }

    private void refreshSequenceSubscriptionList() {
        ArrayList<SubscriptionItem> autocompleteValues = mSequencesSubscribeContainer.getSubscribes();
        mSubscribesAdapter.changeList(autocompleteValues);
        mSubscribesAdapter.notifyDataSetChanged();
    }

    private void refreshSubscriptionList() {
        ArrayList<SubscriptionItem> autocompleteValues = mBooksSubscribeContainer.getSubscribes();
        mSubscribesAdapter.changeList(autocompleteValues);
        mSubscribesAdapter.notifyDataSetChanged();
    }

    private void setupUI() {

        mRecycler = mRoot.findViewById(R.id.resultsList);
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mBooksSubscribeContainer = App.getInstance().getBooksSubscribe();
        mAuthorsSubscribeContainer = App.getInstance().getAuthorsSubscribe();
        mSequencesSubscribeContainer = App.getInstance().getSequencesSubscribe();
        showBooks();

        // отслежу переключение типа подписки
        mRadioContainer = mRoot.findViewById(R.id.subscribe_type);
        if(mRadioContainer != null){
            mRadioContainer.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.searchBook) {
                    showBooks();
                } else if (checkedId == R.id.searchAuthor) {
                    showAuthors();
                } else if (checkedId == R.id.searchSequence) {
                    showSequences();
                }
            });
        }

        // обработаю добавление книги в список загрузки
        // добавлю идентификатор строки поиска
        mSubscribeInput = mRoot.findViewById(R.id.subscribe_name);

        Button subscribeBtn = mRoot.findViewById(R.id.add_to_blacklist_btn);
        if(subscribeBtn != null){
            subscribeBtn.setOnClickListener(this::addSubscribe);
        }

        // назначу действие переключателю автоматической подписки
        SwitchCompat switcher = mRoot.findViewById(R.id.switchAutoCheckSubscribes);
        if(switcher != null){
            switcher.setChecked(MyPreferences.getInstance().isSubscriptionsAutoCheck());
            switcher.setOnCheckedChangeListener((buttonView, isChecked) -> {
                MyPreferences.getInstance().switchSubscriptionsAutoCheck();
                mViewModel.switchSubscriptionsAutoCheck();
                if(isChecked){
                    Toast.makeText(getContext(), "Подписки будут автоматически проверяться раз в сутки",Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getContext(), "Автоматическая провека подписок отключена, чтобы проверить- нажмите на кнопки выше",Toast.LENGTH_SHORT).show();
                }
            });
        }
        // назначу действие кнопкам проверки подписок
        Button fullSubCheckBtn = mRoot.findViewById(R.id.totalCheckButton);
        if(fullSubCheckBtn != null){
            fullSubCheckBtn.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Выполняю поиск подписок по всем новинкам, это займёт время. Найденные результаты будут отображаться во вкладке 'Найденное'.",Toast.LENGTH_SHORT).show();
                v.setEnabled(false);
                mViewModel.fullCheckSubscribes();
            });
        }
        // назначу действие кнопкам проверки подписок
        Button subCheckBtn = mRoot.findViewById(R.id.fastCheckButton);
        if(fullSubCheckBtn != null){
            subCheckBtn.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Выполняю поиск по книгам, которые поступили после последней проверки. Найденные результаты будут отображаться во вкладке 'Найденное'.",Toast.LENGTH_SHORT).show();
                v.setEnabled(false);
                mViewModel.checkSubscribes();
            });
        }
    }

    private void showBooks() {
        // получу подписки на книги
        ArrayList<SubscriptionItem> autocompleteValues = mBooksSubscribeContainer.getSubscribes();
        mSubscribesAdapter = new SubscribesAdapter(autocompleteValues);
        mRecycler.setAdapter(mSubscribesAdapter);
    }

    private void showAuthors() {
        // получу подписки на авторов
        ArrayList<SubscriptionItem> autocompleteValues = mAuthorsSubscribeContainer.getSubscribes();
        mSubscribesAdapter = new SubscribesAdapter(autocompleteValues);
        mRecycler.setAdapter(mSubscribesAdapter);
    }

    private void showSequences() {
        // получу подписки на серии
        ArrayList<SubscriptionItem> autocompleteValues = mSequencesSubscribeContainer.getSubscribes();
        mSubscribesAdapter = new SubscribesAdapter(autocompleteValues);
        mRecycler.setAdapter(mSubscribesAdapter);
    }

    public void addSubscribe(View view) {
        String value = mSubscribeInput.getText().toString().trim();
        if(!value.isEmpty()){
            // добавлю подписку в зависимости от типа
            int checkedRadioButtonId = mRadioContainer.getCheckedRadioButtonId();
            if (checkedRadioButtonId == R.id.searchBook) {
                Log.d("surprise", "SubscribeActivity addSubscribe add book");
                mBooksSubscribeContainer.addValue(value);
            } else if (checkedRadioButtonId == R.id.searchAuthor) {
                Log.d("surprise", "SubscribeActivity addSubscribe add author");
                mAuthorsSubscribeContainer.addValue(value);
            } else if (checkedRadioButtonId == R.id.searchSequence) {
                Log.d("surprise", "SubscribeActivity addSubscribe add sequence");
                mSequencesSubscribeContainer.addValue(value);
            }
            mSubscribeInput.setText("");
            Toast.makeText(getContext(), "Добавляю значение " + value, Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getContext(), "Введите значение для подписки", Toast.LENGTH_LONG).show();
            mSubscribeInput.requestFocus();
        }
    }
}
