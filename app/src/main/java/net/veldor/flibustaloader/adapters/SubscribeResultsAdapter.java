package net.veldor.flibustaloader.adapters;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BR;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.database.dao.DownloadedBooksDao;
import net.veldor.flibustaloader.database.dao.ReadedBooksDao;
import net.veldor.flibustaloader.databinding.SearchedBookItemBinding;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;

import java.util.ArrayList;

public class SubscribeResultsAdapter extends RecyclerView.Adapter<SubscribeResultsAdapter.ViewHolder> {
    private ArrayList<FoundedBook> mBooks = new ArrayList<>();
    private LayoutInflater mLayoutInflater;
    private final DownloadedBooksDao mDao;
    private final ReadedBooksDao mReadDao;

    public SubscribeResultsAdapter() {
        mDao = App.getInstance().mDatabase.downloadedBooksDao();
        mReadDao = App.getInstance().mDatabase.readedBooksDao();
    }

    @NonNull
    @Override
    public SubscribeResultsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        SearchedBookItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_book_item, viewGroup, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SubscribeResultsAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind(mBooks.get(i));
    }

    @Override
    public int getItemCount() {
        return mBooks.size();
    }


    public void setContent(ArrayList<FoundedBook> arrayList) {
        mBooks = arrayList;
        notifyDataSetChanged();
    }

    public void bookDownloaded(String bookId) {
        for (FoundedBook f :
                mBooks) {
            if (f != null && f.id.equals(bookId)) {
                f.downloaded = true;
                notifyItemChanged(mBooks.lastIndexOf(f));
                break;
            }
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final View mRoot;

        private final ViewDataBinding mBinding;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mRoot = mBinding.getRoot();
            View menu = mRoot.findViewById(R.id.menuButton);
            if(menu != null){
                menu.setVisibility(View.GONE);
            }
        }

        void bind(final FoundedBook foundedBook) {
            mBinding.setVariable(BR.book, foundedBook);
            mBinding.executePendingBindings();
            // добавлю действие при клике на кнопку скачивания
            View container = mBinding.getRoot();

            Button downloadButton = container.findViewById(R.id.downloadBookBtn);
            downloadButton.setOnClickListener(view -> {
                // если ссылка на скачивание одна- скачаю книгу, если несколько- выдам диалоговое окно со списком форматов для скачивания
                if (foundedBook.downloadLinks.size() > 1) {
                    String savedMime = App.getInstance().getFavoriteMime();
                    if (savedMime != null) {
                        // проверю, нет ли в списке выбранного формата
                        for (DownloadLink dl : foundedBook.downloadLinks) {
                            if (dl.mime.contains(savedMime)) {
                                ArrayList<DownloadLink> result = new ArrayList<>();
                                result.add(dl);
                                App.getInstance().mDownloadLinksList.postValue(result);
                                return;
                            }
                        }
                    }
                }
                App.getInstance().mDownloadLinksList.postValue(foundedBook.downloadLinks);
            });
            (new Handler()).post(() -> {
                // проверю, если книга прочитана- покажу это
                if (mReadDao.getBookById(foundedBook.id) != null) {
                    ImageButton readView = mRoot.findViewById(R.id.book_read);
                    if (readView != null) {
                        readView.setVisibility(View.VISIBLE);
                        readView.setOnClickListener(view -> Toast.makeText(App.getInstance(), "Книга отмечена как прочитанная", Toast.LENGTH_LONG).show());
                    }
                }
                // проверю, если книга прочитана- покажу это
                if (mDao.getBookById(foundedBook.id) != null) {
                    ImageButton downloadedView = mRoot.findViewById(R.id.book_downloaded);
                    if (downloadedView != null) {
                        downloadedView.setVisibility(View.VISIBLE);
                        downloadedView.setOnClickListener(view -> Toast.makeText(App.getInstance(), "Книга уже скачивалась", Toast.LENGTH_LONG).show());
                    }
                }
            });
        }
    }
}
