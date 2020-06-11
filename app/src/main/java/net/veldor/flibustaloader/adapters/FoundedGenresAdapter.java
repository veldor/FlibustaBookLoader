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
import net.veldor.flibustaloader.databinding.SearchedGenreItemBinding;
import net.veldor.flibustaloader.interfaces.MyAdapterInterface;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.ui.OPDSActivity;
import net.veldor.flibustaloader.utils.SortHandler;

import java.util.ArrayList;

public class FoundedGenresAdapter extends RecyclerView.Adapter<FoundedGenresAdapter.ViewHolder> implements MyAdapterInterface {
    private ArrayList<Genre> mGenres;
    private LayoutInflater mLayoutInflater;

    private DownloadLink mCurrentLink;

    public FoundedGenresAdapter(ArrayList<Genre> arrayList) {

        if (arrayList != null && arrayList.size() > 0) {
            mGenres = arrayList;
        }
    }

    @NonNull
    @Override
    public FoundedGenresAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        SearchedGenreItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_genre_item, viewGroup, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FoundedGenresAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind(mGenres.get(i));
    }

    @Override
    public int getItemCount() {
        if (mGenres != null) {
            return mGenres.size();
        }
        return 0;
    }

    public void setContent(ArrayList<Genre> newData) {
        if(newData == null){
            mGenres = new ArrayList<>();
            notifyDataSetChanged();
        }
        else if(newData.size() == 0 && mGenres.size() == 0){
            Toast.makeText(App.getInstance(), "Жанры не найдены",Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
        }
        else{
            int previousArrayLen = mGenres.size();
            mGenres.addAll(newData);
            notifyItemRangeInserted(previousArrayLen, newData.size());
        }
    }

    public void sort() {
        SortHandler.sortGenres(mGenres);
        notifyDataSetChanged();
        Toast.makeText(App.getInstance(), "Серии отсортированы!",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void clearList() {
        mGenres = new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;
        private Genre mGenre;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            View container = mBinding.getRoot();

            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    OPDSActivity.sLiveSearchLink.postValue(mGenre.term);
                }
            });

        }

        void bind(final Genre foundedGenre) {
            mGenre = foundedGenre;
            mBinding.setVariable(BR.genre, foundedGenre);
            mBinding.executePendingBindings();
        }
    }
}