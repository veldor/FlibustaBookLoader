package net.veldor.flibustaloader.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.adapters.SubscribeResultsAdapter;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.view_models.SubscriptionsViewModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import static net.veldor.flibustaloader.utils.MyFileReader.SUBSCRIPTIONS_FILE;

public class SubscriptionsResultFragment extends Fragment {

    private View mRoot;
    private SubscribeResultsAdapter mAdapter;
    private SubscriptionsViewModel mViewModel;
    private TextView mNothingFoundView;
    private boolean mFragmentVisible = true;
    private Context mContext;
    private RecyclerView mRecycler;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mRoot = inflater.inflate(R.layout.fragment_subscribe_results, container, false);
        if (mRoot != null) {
            setupUI();
        }

        mContext = getContext();

        mViewModel = new ViewModelProvider(this).get(SubscriptionsViewModel.class);

        // попробую рассериализировать объект
        deserealizeSubscriptions();

        // буду отслеживать появление книг по подпискам
        addObservers();
        return mRoot;
    }

    @Override
    public void onPause() {
        super.onPause();
        mFragmentVisible = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        mFragmentVisible = true;
    }

    private void addObservers() {

        LiveData<Boolean> data = mViewModel.getCheckData();
        data.observe(getViewLifecycleOwner(), aBoolean -> {
            if (aBoolean) {
                deserealizeSubscriptions();
            }
        });

        // добавлю отслеживание получения списка ссылок на скачивание
        LiveData<ArrayList<DownloadLink>> downloadLinks = App.getInstance().mDownloadLinksList;
        downloadLinks.observe(getViewLifecycleOwner(), downloadLinks1 -> {
            if (downloadLinks1 != null && downloadLinks1.size() > 0 && mFragmentVisible) {
                if (downloadLinks1.size() == 1) {
                    // добавлю книгу в очередь скачивания
                    mViewModel.addToDownloadQueue(downloadLinks1.get(0));
                    Toast.makeText(getContext(), R.string.book_added_to_schedule_message, Toast.LENGTH_LONG).show();
                } else {
                    // покажу диалог для выбора ссылки для скачивания
                    showDownloadsDialog(downloadLinks1);
                }
                App.getInstance().mDownloadLinksList.setValue(null);
            }
        });


        // отслеживание загруженной книги
        LiveData<String> downloadedBook = App.getInstance().mLiveDownloadedBookId;
        downloadedBook.observe(getViewLifecycleOwner(), downloadedBookId -> {
            if (downloadedBookId != null && mRecycler.getAdapter() instanceof SubscribeResultsAdapter) {
                ((SubscribeResultsAdapter) mRecycler.getAdapter()).bookDownloaded(downloadedBookId);
                App.getInstance().mLiveDownloadedBookId.postValue(null);
            }
        });
    }

    private void showDownloadsDialog(ArrayList<DownloadLink> downloadLinks) {
        LayoutInflater inflate = getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflate.inflate(R.layout.confirm_book_type_select, null);
        Switch checker = view.findViewById(R.id.save_only_selected);
        if (checker != null) {
            checker.setChecked(App.getInstance().isSaveOnlySelected());
            checker.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    Toast.makeText(
                            getContext(),
                            "Выбранный формат будет запомнен и в последующем, если книга будет недоступна в данном формате- она будет пропущена. Опцию можно сбросить в настройках",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
        checker = view.findViewById(R.id.reDownload);
        if (checker != null) {
            checker.setChecked(App.getInstance().isReDownload());
            checker.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    Toast.makeText(
                            getContext(),
                            "Ранее загруженные книги будут перезаписаны. Опцию можно сбросить в настройках",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
        checker = view.findViewById(R.id.save_type_selection);
        if (checker != null) {
            checker.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    Toast.makeText(getContext(),
                            "Формат будет запомнен и окно с предложением выбора больше не будет выводиться. Опцию можно сбросить в настройках",
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        dialogBuilder.setTitle(R.string.downloads_dialog_header);
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
        dialogBuilder.setItems(linksArray, (dialogInterface, i) -> {
            // проверю, выбрано ли сохранение формата загрузки
            Dialog dialog = (Dialog) dialogInterface;
            Switch switcher = dialog.findViewById(R.id.save_type_selection);
            if (switcher.isChecked()) {
                // запомню выбор формата
                App.getInstance().saveFavoriteMime(linksArray[i]);
            }
            switcher = dialog.findViewById(R.id.save_only_selected);
            App.getInstance().setSaveOnlySelected(switcher.isChecked());
            switcher = dialog.findViewById(R.id.reDownload);
            App.getInstance().setReDownload(switcher.isChecked());
            // получу сокращённый MIME
            String shortMime = linksArray[i];
            String longMime = MimeTypes.getFullMime(shortMime);
            int counter1 = 0;
            int linksLength1 = downloadLinks.size();
            DownloadLink item;
            while (counter1 < linksLength1) {
                item = downloadLinks.get(counter1);
                if (item.mime.equals(longMime)) {
                    mViewModel.addToDownloadQueue(item);
                    break;
                }
                counter1++;
            }
            Toast.makeText(getContext(), "Книга добавлена в очередь загрузок", Toast.LENGTH_SHORT).show();
        })
                .setView(view);
        AlertDialog preparedDialog = dialogBuilder.create();
        if (mFragmentVisible) {
            preparedDialog.show();
        }
    }

    private void setupUI() {
        mRecycler = mRoot.findViewById(R.id.resultsList);
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new SubscribeResultsAdapter();
        mRecycler.setAdapter(mAdapter);

        mNothingFoundView = mRoot.findViewById(R.id.booksNotFoundText);
    }


    private void deserealizeSubscriptions() {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        ArrayList<FoundedBook> arraylist = new ArrayList<>();
        try {
            File autocompleteFile = new File(App.getInstance().getFilesDir(), SUBSCRIPTIONS_FILE);
            if (autocompleteFile.exists()) {
                fis = new FileInputStream(autocompleteFile);
                ois = new ObjectInputStream(fis);
                @SuppressWarnings("rawtypes") ArrayList list = (ArrayList) ois.readObject();
                if (list.size() > 0) {
                    for (Object book :
                            list) {
                        if (book instanceof FoundedBook) {
                            arraylist.add((FoundedBook) book);
                        }
                    }
                    mAdapter.setContent(arraylist);
                    mNothingFoundView.setVisibility(View.GONE);
                } else {
                    mAdapter.setContent(arraylist);
                    mNothingFoundView.setVisibility(View.VISIBLE);
                }
                ois.close();
                fis.close();
            } else {
                Log.d("surprise", "SubscriptionsActivity deserealizeSubscriptions: serializable file not exists");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
