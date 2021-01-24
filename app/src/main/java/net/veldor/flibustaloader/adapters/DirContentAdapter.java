package net.veldor.flibustaloader.adapters;

import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BR;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.databinding.FileItemBinding;
import net.veldor.flibustaloader.selections.Book;
import net.veldor.flibustaloader.utils.SortHandler;

import java.util.ArrayList;

public class DirContentAdapter extends RecyclerView.Adapter<DirContentAdapter.ViewHolder> {

    private final ArrayList<Book> mItems;
    private LayoutInflater mLayoutInflater;
    private int position;
    private int lastWhich;

    public DirContentAdapter(ArrayList<Book> books) {
        mItems = books;
    }

    private void setPosition(int position) {
        this.position = position;
    }
    public int getPosition() {
        return position;
    }

    public Book getItem(int position) {
        return mItems.get(position);
    }

    @NonNull
    @Override
    public DirContentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        FileItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.file_item, viewGroup, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DirContentAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind(mItems.get(i));

        viewHolder.itemView.setOnLongClickListener(v -> {
            setPosition(viewHolder.getAdapterPosition());
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void delete(Book book) {
        mItems.remove(position);
        notifyDataSetChanged();
        book.file.delete();
    }

    public void sort(int which) {
        SortHandler.sortLoadedBooks(mItems, which, which == lastWhich);
        if(which != lastWhich){
            lastWhich = which;
        }
        else{
            lastWhich = -1;
        }
        notifyDataSetChanged();
    }


    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        private final ViewDataBinding mBinding;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            View mRoot = mBinding.getRoot();
            mRoot.setOnCreateContextMenuListener(this);
        }


        void bind(final Book item) {
            mBinding.setVariable(BR.book, item);
            mBinding.executePendingBindings();
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle(App.getInstance().getString(R.string.books_options_message));
            menu.add(0, v.getId(), 0, App.getInstance().getString(R.string.share_link_message));
            menu.add(0, v.getId(), 0, App.getInstance().getString(R.string.open_with_menu_item));
            menu.add(0, v.getId(), 0, App.getInstance().getString(R.string.delete_item_message));
        }
    }
}
