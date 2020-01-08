package net.veldor.flibustaloader.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BR;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.databinding.SubscriptionItemBinding;
import net.veldor.flibustaloader.selections.SubscriptionItem;

import java.util.ArrayList;

public class SubscribesAdapter extends RecyclerView.Adapter<SubscribesAdapter.ViewHolder> {
    private ArrayList<SubscriptionItem> mItems;
    private LayoutInflater mLayoutInflater;

    public SubscribesAdapter(ArrayList<SubscriptionItem> itemsList) {
        mItems = itemsList;
    }

    @NonNull
    @Override
    public SubscribesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        SubscriptionItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.subscription_item, viewGroup, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SubscribesAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind(mItems.get(i));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void changeList(ArrayList<SubscriptionItem> autocompleteValues) {
        mItems = autocompleteValues;
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }


        void bind(final SubscriptionItem item) {
            mBinding.setVariable(BR.subscriptions, item);
            mBinding.executePendingBindings();
            View container = mBinding.getRoot();
            View deleteBtn = container.findViewById(R.id.deleteItemBtn);
            if(deleteBtn != null){
                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        App.getInstance().getBooksSubscribe().deleteValue(item.name);
                    }
                });
            }
        }
    }
}
