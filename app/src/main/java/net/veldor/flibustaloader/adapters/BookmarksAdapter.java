package net.veldor.flibustaloader.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BR;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.database.dao.BookmarksDao;
import net.veldor.flibustaloader.database.entity.Bookmark;
import net.veldor.flibustaloader.databinding.BookmarkItemBinding;
import net.veldor.flibustaloader.ui.OPDSActivity;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.ViewHolder> {
    private List<Bookmark> mBookmarks;
    private LayoutInflater mLayoutInflater;
    private final BookmarksDao mDao;

    public BookmarksAdapter(List<Bookmark> bookmarksList) {
        mBookmarks = bookmarksList;
        mDao = App.getInstance().mDatabase.bookmarksDao();
    }

    @NonNull
    @Override
    public BookmarksAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        BookmarkItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.bookmark_item, viewGroup, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarksAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind(mBookmarks.get(i));
    }

    @Override
    public int getItemCount() {
        return mBookmarks.size();
    }


    public void setContent(ArrayList<Bookmark> arrayList) {
        mBookmarks = arrayList;
        notifyDataSetChanged();
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(final Bookmark bookmark) {
            mBinding.setVariable(BR.bookmark, bookmark);
            mBinding.executePendingBindings();
            // добавлю действие при клике на кнопку скачивания
            View container = mBinding.getRoot();
            View itemName = container.findViewById(R.id.bookmark_name);
            itemName.setOnClickListener(v -> {
                Intent intent = new Intent(App.getInstance(), OPDSActivity.class);
                intent.putExtra(OPDSActivity.TARGET_LINK, bookmark.link);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK|FLAG_ACTIVITY_CLEAR_TOP);
                App.getInstance().startActivity(intent);
            });

            View deleteItemBtn = container.findViewById(R.id.deleteItemBtn);
            deleteItemBtn.setOnClickListener(v -> {
                mDao.delete(bookmark);
                mBookmarks.remove(bookmark);
                notifyDataSetChanged();
                Toast.makeText(App.getInstance(), "Закладка удалена", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
