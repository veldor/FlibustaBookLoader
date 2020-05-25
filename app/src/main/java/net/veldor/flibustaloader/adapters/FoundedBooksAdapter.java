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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BR;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.database.dao.DownloadedBooksDao;
import net.veldor.flibustaloader.database.dao.ReadedBooksDao;
import net.veldor.flibustaloader.databinding.SearchedBookWithPreviewItemBindingImpl;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.utils.SortHandler;

import java.util.ArrayList;

public class FoundedBooksAdapter extends RecyclerView.Adapter<FoundedBooksAdapter.ViewHolder> {
    private ArrayList<FoundedBook> mBooks = new ArrayList<>();
    private LayoutInflater mLayoutInflater;
    private final DownloadedBooksDao mDao;
    private final ReadedBooksDao mReadDao;

    private DownloadLink mCurrentLink;

    public FoundedBooksAdapter(ArrayList<FoundedBook> arrayList) {
        if (arrayList != null && arrayList.size() > 0) {
            mBooks = arrayList;
        }
        mDao = App.getInstance().mDatabase.downloadedBooksDao();
        mReadDao = App.getInstance().mDatabase.readedBooksDao();
    }

    @NonNull
    @Override
    public FoundedBooksAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        SearchedBookWithPreviewItemBindingImpl binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_book_with_preview_item, viewGroup, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FoundedBooksAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind(mBooks.get(i));
    }

    @Override
    public int getItemCount() {
        if (mBooks != null) {
            return mBooks.size();
        }
        return 0;
    }

    public void sort(){
        SortHandler.sortBooks(mBooks);
        notifyDataSetChanged();
        Toast.makeText(App.getInstance(), "Книги отсортированы!",Toast.LENGTH_SHORT).show();
    }

    public void setContent(ArrayList<FoundedBook> newData, boolean addToLoaded) {
        if (newData == null) {
            if (!addToLoaded) {
                mBooks = new ArrayList<>();
                notifyDataSetChanged();
            }
        } else if (newData.size() == 0 && mBooks.size() == 0) {
            Toast.makeText(App.getInstance(), "Книги не найдены", Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
        } else {
            // если выбрана загрузка всех результатов сразу- добавлю вновь пришедшие к уже существующим
            if (App.getInstance().isDownloadAll() || addToLoaded) {
                int previousArrayLen = mBooks.size();
                mBooks.addAll(newData);
                notifyItemRangeInserted(previousArrayLen, newData.size());
            } else {
                mBooks = newData;
                notifyDataSetChanged();
            }
        }
    }

    public void bookDownloaded(String bookId){
        int counter = 0;
        int booksCount = mBooks.size();
        FoundedBook fb;
        while (counter < booksCount){
            fb = mBooks.get(counter);
            if(fb != null && fb.id.equals(bookId)){
                fb.downloaded = true;
                notifyItemChanged(counter);
                break;
            }
            counter++;
        }
    }

    public void setBookReaded(FoundedBook book){
        if(mBooks.contains(book)){
            int bookIndex = mBooks.lastIndexOf(book);
            // если выбрано скрытие прочитанных книг- удалю её
            if(App.getInstance().isHideRead()){
                mBooks.remove(bookIndex);
                notifyItemRemoved(bookIndex);
            }
            else{
                notifyItemChanged(bookIndex);
            }
        }
    }


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
            if(bookNameView != null){
                bookNameView.setOnClickListener(v -> App.getInstance().mSelectedBook.postValue(mBook));
            }


            // обработаю нажатие на кнопку меню
            ImageButton menuButton = mRoot.findViewById(R.id.menuButton);
            if(menuButton != null){
                menuButton.setOnClickListener(view -> {
                    // отправлю событие контекстного меню для книги
                    App.getInstance().mContextBook.postValue(mBook);
                });
            }

            Button downloadButton = mRoot.findViewById(R.id.downloadBookBtn);
            if(downloadButton != null){
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
            }
            // добавлю действие на нажатие на автора
            TextView authorsView = mRoot.findViewById(R.id.author_name);
            if(authorsView != null){
                authorsView.setOnClickListener(v -> {
                    // если автор один- вывожу диалог выбора отображения, если несколько- вывожу диалог выбора автора
                    if (mBook.authors.size() > 1) {
                        App.getInstance().mSelectedAuthors.postValue(mBook.authors);
                    } else {
                        App.getInstance().mSelectedAuthor.postValue(mBook.authors.get(0));
                    }
                });
            }
            // добавлю действие поиска по серии
            TextView sequenceView = mRoot.findViewById(R.id.sequence);
            if(sequenceView != null){
                sequenceView.setOnClickListener(v -> {
                    if (mBook.sequences.size() > 0) {
                        if (mBook.sequences.size() > 1) {
                            App.getInstance().mSelectedSequences.postValue(mBook.sequences);
                        } else {
                            App.getInstance().mSelectedSequence.postValue(mBook.sequences.get(0));
                        }
                    }
                });
            }

            ImageView imageContainer = mRoot.findViewById(R.id.previewImage);
            if(imageContainer != null){
                imageContainer.setOnClickListener(view -> App.getInstance().mShowCover.postValue(mBook));
            }
        }

        void bind(final FoundedBook foundedBook) {
            mBook = foundedBook;
            mBinding.setVariable(BR.book, mBook);
            mBinding.executePendingBindings();

            ImageView imageContainer = mRoot.findViewById(R.id.previewImage);
            if(imageContainer != null){
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
