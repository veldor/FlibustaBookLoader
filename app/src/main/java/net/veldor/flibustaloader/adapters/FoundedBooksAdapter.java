package net.veldor.flibustaloader.adapters;

import android.os.Handler;
import android.util.Log;
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
import net.veldor.flibustaloader.interfaces.MyAdapterInterface;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.ui.OPDSActivity;
import net.veldor.flibustaloader.utils.MyPreferences;
import net.veldor.flibustaloader.utils.SortHandler;

import java.util.ArrayList;

public class FoundedBooksAdapter extends RecyclerView.Adapter<FoundedBooksAdapter.ViewHolder> implements MyAdapterInterface {
    private ArrayList<FoundedBook> mBooks = new ArrayList<>();
    private LayoutInflater mLayoutInflater;
    private final DownloadedBooksDao mDao;
    private final ReadedBooksDao mReadDao;

    private DownloadLink mCurrentLink;

    public FoundedBooksAdapter(ArrayList<FoundedBook> arrayList) {
        if (arrayList != null && arrayList.size() > 0) {
            mBooks = arrayList;
            OPDSActivity.sBooksForDownload = mBooks;
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
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.cancelImageLoad();
    }

    @Override
    public int getItemCount() {
        if (mBooks != null) {
            return mBooks.size();
        }
        return 0;
    }

    public void sort() {
        SortHandler.sortBooks(mBooks);
        notifyDataSetChanged();
        Toast.makeText(App.getInstance(), "Книги отсортированы!", Toast.LENGTH_SHORT).show();
    }

    public void setContent(ArrayList<FoundedBook> newData, boolean addToLoaded) {
        if (newData == null) {
            if (!addToLoaded) {
                mBooks = new ArrayList<>();
                OPDSActivity.sBooksForDownload = mBooks;
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
                OPDSActivity.sBooksForDownload = mBooks;
                notifyItemRangeInserted(previousArrayLen, newData.size());
            } else {
                mBooks = newData;
                OPDSActivity.sBooksForDownload = mBooks;
                notifyDataSetChanged();
            }
        }
    }

    public void bookDownloaded(String bookId) {
        for (FoundedBook f :
                mBooks) {
            if (f != null && f.id.equals(bookId)) {
                f.downloaded = true;
                if(MyPreferences.getInstance().isDownloadedHide()){
                    mBooks.remove(f);
                    notifyItemRemoved(mBooks.lastIndexOf(f));
                }
                else{
                    notifyItemChanged(mBooks.lastIndexOf(f));
                }
                break;
            }
        }
    }

    public void setBookReaded(FoundedBook book) {
        book.read = true;
        if (mBooks.contains(book)) {
            int bookIndex = mBooks.lastIndexOf(book);
            // если выбрано скрытие прочитанных книг- удалю её
            if (App.getInstance().isHideRead()) {
                mBooks.remove(bookIndex);
                notifyItemRemoved(bookIndex);
            } else {
                notifyItemChanged(bookIndex);
            }
        }
    }

    public void hideReaded() {
        ArrayList<FoundedBook> newList = new ArrayList<>();
        // пройдусь по списку и удалю все прочитанные книги
        if (mBooks.size() > 0) {
            for (FoundedBook book : mBooks) {
                if (!book.read) {
                    newList.add(book);
                }
            }
            mBooks = newList;
            notifyDataSetChanged();
        }
    }


    public void hideDownloaded() {
        ArrayList<FoundedBook> newList = new ArrayList<>();
        // пройдусь по списку и удалю все прочитанные книги
        if (mBooks.size() > 0) {
            for (FoundedBook book : mBooks) {
                if (!book.downloaded) {
                    newList.add(book);
                }
            }
            mBooks = newList;
            notifyDataSetChanged();
        }
    }

    public ArrayList<FoundedBook> getItems() {
        return mBooks;
    }

    @Override
    public void clearList() {
        // удалю книги, если не было подгрузки результатов
        if (App.getInstance().isDownloadAll()) {
            mBooks = new ArrayList<>();
            notifyDataSetChanged();
        }
    }

    public void showReaded(ArrayList<FoundedBook> booksList) {
        mBooks = booksList;
        notifyDataSetChanged();
    }

    public void showDownloaded(ArrayList<FoundedBook> booksList) {
        mBooks = booksList;
        notifyDataSetChanged();
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
            TextView bookNameView = mRoot.findViewById(R.id.blacklist_item_name);
            if (bookNameView != null) {
                bookNameView.setOnClickListener(v -> App.getInstance().mSelectedBook.postValue(mBook));
            }


            // обработаю нажатие на кнопку меню
            ImageButton menuButton = mRoot.findViewById(R.id.menuButton);
            if (menuButton != null) {
                menuButton.setOnClickListener(view -> {
                    // отправлю событие контекстного меню для книги
                    App.getInstance().mContextBook.postValue(mBook);
                });
            }

            Button downloadButton = mRoot.findViewById(R.id.downloadBookBtn);
            if (downloadButton != null) {
                downloadButton.setOnClickListener(view -> {
                    Log.d("surprise", "ViewHolder ViewHolder 227: founded links for download " + mBook.downloadLinks);
                    // если ссылка на скачивание одна- скачаю книгу, если несколько- выдам диалоговое окно со списком форматов для скачивания
                    if (mBook.downloadLinks.size() > 1) {
                        String savedMime = App.getInstance().getFavoriteMime();
                        if (savedMime != null && !savedMime.isEmpty()) {
                            // проверю, нет ли в списке выбранного формата
                            for (DownloadLink dl : mBook.downloadLinks) {
                                mCurrentLink = dl;
                                if (dl.mime.contains(savedMime)) {
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
            if (authorsView != null) {
                authorsView.setOnClickListener(v -> {
                    // если автор один- вывожу диалог выбора отображения, если несколько- вывожу диалог выбора автора
                    if (mBook.authors.size() > 1) {
                        App.getInstance().mSelectedAuthors.postValue(mBook.authors);
                    } else if(mBook.authors.size() == 1){
                        App.getInstance().mSelectedAuthor.postValue(mBook.authors.get(0));
                    }
                });
            }
            // добавлю действие поиска по серии
            TextView sequenceView = mRoot.findViewById(R.id.sequence);
            if (sequenceView != null) {
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
            if (imageContainer != null) {
                imageContainer.setOnClickListener(view -> App.getInstance().mShowCover.postValue(mBook));
            }

            ImageButton downloadedView = mRoot.findViewById(R.id.book_downloaded);
            if (downloadedView != null) {
                downloadedView.setVisibility(View.INVISIBLE);
            }

            ImageButton readView = mRoot.findViewById(R.id.book_read);
            if (readView != null) {
                readView.setVisibility(View.INVISIBLE);
            }
        }

        void bind(final FoundedBook foundedBook) {
            mBook = foundedBook;
            mBinding.setVariable(BR.book, mBook);
            mBinding.executePendingBindings();

            ImageView imageContainer = mRoot.findViewById(R.id.previewImage);
            if (imageContainer != null) {
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

        void cancelImageLoad() {
            if (App.getInstance().isPreviews() && mBook.previewUrl != null) {
                ImageView imageContainer = mRoot.findViewById(R.id.previewImage);
                if (imageContainer != null) {
                    Glide.with(imageContainer)
                            .clear(imageContainer);
                }
            }
        }
    }
}
