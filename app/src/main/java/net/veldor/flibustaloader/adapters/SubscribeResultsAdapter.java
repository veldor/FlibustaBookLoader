package net.veldor.flibustaloader.adapters;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BR;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.databinding.SearchedBookItemBinding;
import net.veldor.flibustaloader.selections.FoundedBook;

import java.util.ArrayList;

public class SubscribeResultsAdapter extends RecyclerView.Adapter<SubscribeResultsAdapter.ViewHolder> {
    private ArrayList mBooks = new ArrayList<FoundedBook>();
    private LayoutInflater mLayoutInflater;

    public SubscribeResultsAdapter() {
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
        viewHolder.bind((FoundedBook) mBooks.get(i));
    }

    @Override
    public int getItemCount() {
        return mBooks.size();
    }


    public void setContent(ArrayList<FoundedBook> arrayList) {
        mBooks = arrayList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(final FoundedBook foundedBook) {
            mBinding.setVariable(BR.book, foundedBook);
            mBinding.executePendingBindings();
            // добавлю действие при клике на кнопку скачивания
            View container = mBinding.getRoot();

            // обработаю нажатие на кнопку меню
            ImageButton menuButton = container.findViewById(R.id.menuButton);
            menuButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // отправлю событие контекстного меню для книги
                    App.getInstance().mContextBook.postValue(foundedBook);
                }
            });

            Button downloadButton = container.findViewById(R.id.downloadBookBtn);
            downloadButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // если ссылка на скачивание одна- скачаю книгу, если несколько- выдам диалоговое окно со списком форматов для скачивания
                    if (foundedBook.downloadLinks.size() == 1) {
                        Toast.makeText(App.getInstance(), "Начинаю скачивание", Toast.LENGTH_LONG).show();
                    }
                    App.getInstance().mDownloadLinksList.postValue(foundedBook.downloadLinks);
                }
            });
        }
    }
}
