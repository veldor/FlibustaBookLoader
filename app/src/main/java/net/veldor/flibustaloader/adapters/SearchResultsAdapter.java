package net.veldor.flibustaloader.adapters;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BR;
import net.veldor.flibustaloader.OPDSActivity;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.databinding.SearchedAuthorItemBinding;
import net.veldor.flibustaloader.databinding.SearchedBookItemBinding;
import net.veldor.flibustaloader.databinding.SearchedGenreItemBinding;
import net.veldor.flibustaloader.databinding.SearchedSequenceItemBinding;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.utils.SortHandler;

import java.util.ArrayList;

@SuppressWarnings({"unchecked"})
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
    private ArrayList mAuthors;
    private ArrayList mBooks;
    private ArrayList mGenres;
    private ArrayList mSequences;
    private LayoutInflater mLayoutInflater;


    private DownloadLink mCurrentLink;

    public SearchResultsAdapter(ArrayList<FoundedItem> arrayList) {
        switch (App.sSearchType) {
            case OPDSActivity
                    .SEARCH_BOOKS:
                mBooks = arrayList;
                break;
            case OPDSActivity
                    .SEARCH_AUTHORS:
            case OPDSActivity
                    .SEARCH_NEW_AUTHORS:
                mAuthors = arrayList;
                break;
            case OPDSActivity
                    .SEARCH_GENRE:
                mGenres = arrayList;
                break;
            case OPDSActivity
                    .SEARCH_SEQUENCE:
                mSequences = arrayList;
                break;
        }
    }

    @NonNull
    @Override
    public SearchResultsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        if (mAuthors != null) {
            SearchedAuthorItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_author_item, viewGroup, false);
            return new ViewHolder(binding);
        } else if (mBooks != null) {
            SearchedBookItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_book_item, viewGroup, false);
            return new ViewHolder(binding);
        } else if (mGenres != null) {
            SearchedGenreItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_genre_item, viewGroup, false);
            return new ViewHolder(binding);
        } else {
            SearchedSequenceItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_sequence_item, viewGroup, false);
            return new ViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultsAdapter.ViewHolder viewHolder, int i) {
        if (mAuthors != null) {
            viewHolder.bind((Author) mAuthors.get(i));
        } else if (mBooks != null) {
            viewHolder.bind((FoundedBook) mBooks.get(i));
        } else if (mGenres != null) {
            viewHolder.bind((Genre) mGenres.get(i));
        } else {
            viewHolder.bind((FoundedSequence) mSequences.get(i));
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


    public void nothingFound() {
        mAuthors = null;
        mBooks = null;
        mSequences = null;
        mGenres = null;
    }


    public void setContent(ArrayList<FoundedItem> arrayList) {
        switch (App.sSearchType) {
            case OPDSActivity
                    .SEARCH_BOOKS:
                mBooks = arrayList;
                break;
            case OPDSActivity
                    .SEARCH_AUTHORS:
                mAuthors = arrayList;
                break;
        }
    }

    public void sortBooks() {
        // сортирую книги
        SortHandler.sortBooks(mBooks);
        notifyDataSetChanged();
    }

    public void sortAuthors() {
        SortHandler.sortAuthors(mAuthors);
        notifyDataSetChanged();
    }

    public void sortGenres() {
        SortHandler.sortGenres(mGenres);
        notifyDataSetChanged();
    }

    public void sortSequences() {
        SortHandler.sortSequences(mSequences);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

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
                    if(foundedAuthor.uri != null){
                        App.getInstance().mSelectedAuthor.postValue(foundedAuthor);
                    }
                    else{
                        // поиск новых книг автора
                        App.getInstance().mAuthorNewBooks.postValue(foundedAuthor);
                    }
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

            // проверю, если книга прочитана- покажу это
            if(foundedBook.read){
                ImageButton readView = container.findViewById(R.id.book_read);
                if(readView != null){
                    readView.setVisibility(View.VISIBLE);
                    readView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(App.getInstance(), "Книга отмечена как прочитанная", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
            // проверю, если книга прочитана- покажу это
            if(foundedBook.downloaded){
                ImageButton downloadedView = container.findViewById(R.id.book_downloaded);
                if(downloadedView != null){
                    downloadedView.setVisibility(View.VISIBLE);
                    downloadedView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(App.getInstance(), "Книга уже скачивалась", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

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
                    else{
                        String savedMime = App.getInstance().getFavoriteMime();
                        if(savedMime !=null){
                            Log.d("surprise", "ViewHolder onClick saved mime is " + savedMime);
                            // проверю, нет ли в списке выбранного формата
                            for(DownloadLink dl:foundedBook.downloadLinks){
                                mCurrentLink = dl;
                                if(dl.mime.equals(savedMime)){
                                    ArrayList<DownloadLink> result = new ArrayList<>();
                                    result.add(mCurrentLink);
                                    App.getInstance().mDownloadLinksList.postValue(result);
                                    return;
                                }
                            }
                        }
                    }
                    App.getInstance().mDownloadLinksList.postValue(foundedBook.downloadLinks);
                }
            });
            // добавлю действие на нажатие на автора
            TextView authorsView = container.findViewById(R.id.author_name);
            authorsView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // если автор один- вывожу диалог выбора отображения, если несколько- вывожу диалог выбора автора
                    if (foundedBook.authors.size() > 1) {
                        App.getInstance().mSelectedAuthors.postValue(foundedBook.authors);
                    } else {
                        App.getInstance().mSelectedAuthor.postValue(foundedBook.authors.get(0));
                    }
                }
            });
            // добавлю действие поиска по серии
            if (foundedBook.sequences.size() > 0) {
                TextView sequenceView = container.findViewById(R.id.sequence);
                sequenceView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (foundedBook.sequences.size() > 1) {
                            App.getInstance().mSelectedSequences.postValue(foundedBook.sequences);
                        } else {
                            App.getInstance().mSelectedSequence.postValue(foundedBook.sequences.get(0));
                        }
                    }
                });
            }
            // добавлю отображение информации о книге при клике на название книги
            TextView bookNameView = container.findViewById(R.id.book_name);
            bookNameView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    App.getInstance().mSelectedBook.postValue(foundedBook);
                }
            });
        }
    }
}
