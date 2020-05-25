package net.veldor.flibustaloader.adapters;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BR;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.database.dao.DownloadedBooksDao;
import net.veldor.flibustaloader.database.dao.ReadedBooksDao;
import net.veldor.flibustaloader.databinding.SearchedBookWithPreviewItemBinding;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;

import java.util.ArrayList;

public class FoundedBooksListAdapter extends ListAdapter<FoundedBook, FoundedBooksListAdapter.ViewHolder> {

    private final DownloadedBooksDao mDao;
    private final ReadedBooksDao mReadDao;
    private LayoutInflater mLayoutInflater;
    private DownloadLink mCurrentLink;

    public FoundedBooksListAdapter() {
        super(DIFF_CALLBACK);
        mDao = App.getInstance().mDatabase.downloadedBooksDao();
        mReadDao = App.getInstance().mDatabase.readedBooksDao();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(parent.getContext());
        }
        SearchedBookWithPreviewItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_book_with_preview_item, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    private static final DiffUtil.ItemCallback<FoundedBook> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<FoundedBook>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull FoundedBook oldBook, @NonNull FoundedBook newBook) {
                    // User properties may have changed if reloaded from the DB, but ID is fixed
                    return oldBook.id.equals(newBook.id);
                }
                @Override
                public boolean areContentsTheSame(
                        @NonNull FoundedBook oldBook, @NonNull FoundedBook newBook) {
                    // NOTE: if you use equals, your object must properly override Object#equals()
                    // Incorrectly returning false here will result in too many animations.
                    return oldBook.equals(newBook);
                }
            };

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;
        private FoundedBook mBook;
        private final View mRoot;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            mRoot = mBinding.getRoot();

            // добавлю отображение информации о книге при клике на название книги
            TextView bookNameView = mRoot.findViewById(R.id.book_name);
            bookNameView.setOnClickListener(v -> App.getInstance().mSelectedBook.postValue(mBook));


            // обработаю нажатие на кнопку меню
            ImageButton menuButton = mRoot.findViewById(R.id.menuButton);
            menuButton.setOnClickListener(view -> {
                // отправлю событие контекстного меню для книги
                App.getInstance().mContextBook.postValue(mBook);
            });

            Button downloadButton = mRoot.findViewById(R.id.downloadBookBtn);
            downloadButton.setOnClickListener(view -> {
                // если ссылка на скачивание одна- скачаю книгу, если несколько- выдам диалоговое окно со списком форматов для скачивания
                if (mBook.downloadLinks.size() > 1) {
                    String savedMime = App.getInstance().getFavoriteMime();
                    if (savedMime != null) {
                        // проверю, нет ли в списке выбранного формата
                        for (DownloadLink dl : mBook.downloadLinks) {
                            mCurrentLink = dl;
                            if (dl.mime.equals(savedMime)) {
                                ArrayList<DownloadLink> result = new ArrayList<>();
                                result.add(mCurrentLink);
                                App.getInstance().mDownloadLinksList.postValue(result);
                                return;
                            }
                        }
                    }
                }
                App.getInstance().mDownloadLinksList.postValue(mBook.downloadLinks);
            });
            downloadButton.setOnLongClickListener(v -> {
                // если ссылка на скачивание одна- скачаю книгу, если несколько- выдам диалоговое окно со списком форматов для скачивания
                App.getInstance().mDownloadLinksList.postValue(mBook.downloadLinks);
                return true;
            });
            // добавлю действие на нажатие на автора
            TextView authorsView = mRoot.findViewById(R.id.author_name);
            authorsView.setOnClickListener(v -> {
                // если автор один- вывожу диалог выбора отображения, если несколько- вывожу диалог выбора автора
                if (mBook.authors.size() > 1) {
                    App.getInstance().mSelectedAuthors.postValue(mBook.authors);
                } else {
                    App.getInstance().mSelectedAuthor.postValue(mBook.authors.get(0));
                }
            });
            // добавлю действие поиска по серии
            TextView sequenceView = mRoot.findViewById(R.id.sequence);
            sequenceView.setOnClickListener(v -> {
                if (mBook.sequences.size() > 0) {
                    if (mBook.sequences.size() > 1) {
                        App.getInstance().mSelectedSequences.postValue(mBook.sequences);
                    } else {
                        App.getInstance().mSelectedSequence.postValue(mBook.sequences.get(0));
                    }
                }
            });

            ImageView imageContainer = mRoot.findViewById(R.id.previewImage);
            imageContainer.setOnClickListener(view -> App.getInstance().mShowCover.postValue(mBook));
        }

        void bind(final FoundedBook foundedBook) {
            mBook = foundedBook;
            mBinding.setVariable(BR.book, mBook);
            mBinding.executePendingBindings();

            ImageView imageContainer = mRoot.findViewById(R.id.previewImage);
            // если включено отображение превью книг и превью существует
            if (App.getInstance().isPreviews() && foundedBook.previewUrl != null) {
                imageContainer.setVisibility(View.VISIBLE);
                // загружу изображение с помощью GLIDE
                Glide
                        .with(imageContainer)
                        .load("https://flibusta.appspot.com" + foundedBook.previewUrl)
                        .into(imageContainer);
            } else {
                imageContainer.setVisibility(View.GONE);
            }

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
