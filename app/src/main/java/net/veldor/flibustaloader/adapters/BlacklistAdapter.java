package net.veldor.flibustaloader.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BR;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.databinding.BlacklistItemBinding;
import net.veldor.flibustaloader.selections.BlacklistItem;

import java.util.ArrayList;

public class BlacklistAdapter extends RecyclerView.Adapter<BlacklistAdapter.ViewHolder> {
    private ArrayList<BlacklistItem> mItems;
    private LayoutInflater mLayoutInflater;

    public BlacklistAdapter(ArrayList<BlacklistItem> itemsList) {
        mItems = itemsList;
    }

    @NonNull
    @Override
    public BlacklistAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        BlacklistItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.blacklist_item, viewGroup, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BlacklistAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind(mItems.get(i));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void changeList(ArrayList<BlacklistItem> autocompleteValues) {
        mItems = autocompleteValues;
    }


    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }


        void bind(final BlacklistItem item) {
            mBinding.setVariable(BR.blacklists, item);
            mBinding.executePendingBindings();
            View container = mBinding.getRoot();
            TextView name = container.findViewById(R.id.blacklist_item_name);
                    switch (item.type){
                        case "book":
                        name.setTextColor(App.getInstance().getResources().getColor(R.color.book_name_color));
                        break;
                        case "author":
                        name.setTextColor(App.getInstance().getResources().getColor(R.color.author_text_color));
                        break;
                        case "sequence":
                        name.setTextColor(App.getInstance().getResources().getColor(R.color.sequences_text));
                        break;
                        case "genre":
                        name.setTextColor(App.getInstance().getResources().getColor(R.color.genre_text_color));
                        break;
                    }
            View deleteBtn = container.findViewById(R.id.deleteItemBtn);
            if(deleteBtn != null){
                deleteBtn.setOnClickListener(v -> {
                    switch (item.type) {
                        case "book":
                            App.getInstance().getBooksBlacklist().deleteValue(item.name);
                            break;
                        case "author":
                            App.getInstance().getAuthorsBlacklist().deleteValue(item.name);
                            break;
                        case "sequence":
                            App.getInstance().getSequencesBlacklist().deleteValue(item.name);
                            break;
                        case "genre":
                            App.getInstance().getGenresBlacklist().deleteValue(item.name);
                            break;
                    }

                });
            }
        }
    }
}
