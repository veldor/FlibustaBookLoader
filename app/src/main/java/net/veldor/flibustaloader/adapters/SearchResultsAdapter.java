package net.veldor.flibustaloader.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import net.veldor.flibustaloader.databinding.SearchedGenreItemBinding;
import net.veldor.flibustaloader.databinding.SearchedSequenceItemBinding;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;

import java.util.ArrayList;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
    private ArrayList<Author> mAuthors;
    private ArrayList<FoundedBook> mBooks;
    private ArrayList<Genre> mGenres;
    private LayoutInflater mLayoutInflater;
    private ArrayList<FoundedSequence> mSequences;

    @NonNull
    @Override
    public SearchResultsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        if (mAuthors != null) {
            SearchedAuthorItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_author_item, viewGroup, false);
            return new ViewHolder(binding);
        } else if(mBooks != null) {
            SearchedBookItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_book_item, viewGroup, false);
            return new ViewHolder(binding);
        }else if(mGenres != null) {
            SearchedGenreItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_genre_item, viewGroup, false);
            return new ViewHolder(binding);
        } else{
            SearchedSequenceItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_sequence_item, viewGroup, false);
            return new ViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultsAdapter.ViewHolder viewHolder, int i) {
        if (mAuthors != null) {
            viewHolder.bind(mAuthors.get(i));
        } else if(mBooks != null) {
            viewHolder.bind(mBooks.get(i));
        } else if(mGenres != null) {
            viewHolder.bind(mGenres.get(i));
        }
        else{
            viewHolder.bind(mSequences.get(i));
        }
    }

    @Override
    public int getItemCount() {
        if (mAuthors != null) {
            return mAuthors.size();
        } else if (mBooks != null) {
            return mBooks.size();
        } else if (mSequences != null) {
            return mSequences.size();
        } else if (mGenres != null) {
            return mGenres.size();
        }
        return 0;
    }

    public void setAuthorsList(ArrayList authors) {
        mAuthors = authors;
    }

    public void setBooksList(ArrayList books) {
        mBooks = books;
    }

    public void setSequencesList(ArrayList sequences) {
        mSequences = sequences;
    }

    public void nothingFound() {
        mAuthors = null;
        mBooks = null;
        mSequences = null;
    }

    public void setGenresList(ArrayList genres) {
        mGenres = genres;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(final Author foundedAuthor) {
            mBinding.setVariable(BR.author, foundedAuthor);
            mBinding.executePendingBindings();
            View container = mBinding.getRoot();
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    App.getInstance().mSelectedAuthor.postValue(foundedAuthor);
                }
            });
        }
        void bind(final Genre foundedGenre) {
            mBinding.setVariable(BR.genre, foundedGenre);
            mBinding.executePendingBindings();
            View container = mBinding.getRoot();
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    App.getInstance().mSelectedGenre.postValue(foundedGenre);
                }
            });
        }
        void bind(final FoundedSequence sequence) {
            mBinding.setVariable(BR.sequence, sequence);
            mBinding.executePendingBindings();
            View container = mBinding.getRoot();
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    App.getInstance().mSelectedSequence.postValue(sequence);
                }
            });
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
/*            // добавлю действие поиска по жанру
            if(foundedBook.genres != null){
                TextView genreView = container.findViewById(R.id.genre);
                genreView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(foundedBook.genres.size() > 1){

                        }
                        else{
                            App.getInstance().mSelectedGenre.postValue(foundedBook.genres.get(0));
                        }
                    }
                });
            }*/
            // добавлю действие поиска по серии
            if(foundedBook.sequences != null){
                TextView sequenceView = container.findViewById(R.id.sequence);
                sequenceView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(foundedBook.sequences.size() > 1){
                            App.getInstance().mSelectedSequences.postValue(foundedBook.sequences);
                        }
                        else{
                            App.getInstance().mSelectedSequence.postValue(foundedBook.sequences.get(0));
                        }
                    }
                });
            }
        }
    }
}
