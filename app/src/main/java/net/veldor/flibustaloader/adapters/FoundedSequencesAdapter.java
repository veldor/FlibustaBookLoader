package net.veldor.flibustaloader.adapters;

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
import net.veldor.flibustaloader.databinding.SearchedSequenceItemBinding;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.utils.SortHandler;

import java.util.ArrayList;

public class FoundedSequencesAdapter extends RecyclerView.Adapter<FoundedSequencesAdapter.ViewHolder> {
    private ArrayList<FoundedSequence> mSequences;
    private LayoutInflater mLayoutInflater;


    public FoundedSequencesAdapter(ArrayList<FoundedSequence> arrayList) {

        if (arrayList != null && arrayList.size() > 0) {
            mSequences = arrayList;
        }
    }

    @NonNull
    @Override
    public FoundedSequencesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        SearchedSequenceItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_sequence_item, viewGroup, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FoundedSequencesAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind(mSequences.get(i));
    }

    @Override
    public int getItemCount() {
        if (mSequences != null) {
            return mSequences.size();
        }
        return 0;
    }

    public void setContent(ArrayList<FoundedSequence> arrayList) {
        mSequences = arrayList;
        notifyDataSetChanged();
    }

    public void sort() {
        SortHandler.sortSequences(mSequences);
        notifyDataSetChanged();
        Toast.makeText(App.getInstance(), "Серии отсортированы!",Toast.LENGTH_SHORT).show();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;
        private FoundedSequence mSequence;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            View container = mBinding.getRoot();
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    App.getInstance().mSelectedSequence.postValue(mSequence);
                }
            });

        }
        void bind(final FoundedSequence sequence) {
            mSequence = sequence;
            mBinding.setVariable(BR.sequence, sequence);
            mBinding.executePendingBindings();
        }
    }
}
