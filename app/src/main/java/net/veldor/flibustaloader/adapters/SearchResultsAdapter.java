package net.veldor.flibustaloader.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BR;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.databinding.SearchedAuthorItemBinding;
import net.veldor.flibustaloader.databinding.SearchedBookItemBinding;
import net.veldor.flibustaloader.selections.FoundedAuthor;
import net.veldor.flibustaloader.selections.FoundedBook;

import java.util.ArrayList;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
    private ArrayList<FoundedAuthor> mAuthors;
    private ArrayList<FoundedBook> mBooks;
    private LayoutInflater mLayoutInflater;

    @NonNull
    @Override
    public SearchResultsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        if (mAuthors != null) {
            SearchedAuthorItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_author_item, viewGroup, false);
            return new ViewHolder(binding);
        } else {
            SearchedBookItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_book_item, viewGroup, false);
            return new ViewHolder(binding);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultsAdapter.ViewHolder viewHolder, int i) {
        if (mAuthors != null) {
            viewHolder.bind(mAuthors.get(i));
        } else {
            viewHolder.bind(mBooks.get(i));
        }
    }

    @Override
    public int getItemCount() {
        if (mAuthors != null) {
            return mAuthors.size();
        } else if (mBooks != null) {
            return mBooks.size();
        }
        return 0;
    }

    public void setAuthorsList(ArrayList authors) {
        mAuthors = authors;
        mBooks = null;
    }

    public void nothingFound() {
        mAuthors = null;
        mBooks = null;
    }

    public void setBooksList(ArrayList books) {
        mAuthors = null;
        mBooks = books;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(FoundedAuthor foundedAuthor) {
            mBinding.setVariable(BR.author, foundedAuthor);
            mBinding.executePendingBindings();

        }

        void bind(final FoundedBook foundedBook) {
            mBinding.setVariable(BR.book, foundedBook);
            mBinding.executePendingBindings();

            // добавлю действие при клике на кнопку скачивания
            View container = mBinding.getRoot();
            Button downloadButton = container.findViewById(R.id.downloadBookBtn);
            downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // если ссылка на скачивание одна- скачаю книгу, если несколько- выдам диалоговое окно со списком форматов для скачивания
                    if (foundedBook.downloadLinks.size() == 1) {
                        Toast.makeText(App.getInstance(), "Начинаю скачивание", Toast.LENGTH_LONG).show();
                    }
                    App.getInstance().mDownloadLinksList.postValue(foundedBook.downloadLinks);
                }
            });
            // добавлю действие на нажатие на автора
            TextView authorsView = container.findViewById(R.id.author_name);
            authorsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // если автор один- вывожу диалог выбора отображения, если несколько- вывожу диалог выбора автора
                    if(foundedBook.authors.size() > 1){
                        App.getInstance().mSelectedAuthors.postValue(foundedBook.authors);
                    }
                    else{
                        App.getInstance().mSelectedAuthor.postValue(foundedBook.authors.get(0));
                    }
                }
            });
        }
    }

    public void clear() {
        if (mBooks != null) {
            int size = mBooks.size();
            if (size > 0) {
                mBooks.subList(0, size).clear();
                notifyItemRangeRemoved(0, size);
            }
        } else if (mAuthors != null) {
            int size = mAuthors.size();
            if (size > 0) {
                mAuthors.subList(0, size).clear();
                notifyItemRangeRemoved(0, size);
            }
        }
    }
}
