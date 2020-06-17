package net.veldor.flibustaloader.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BR;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.databinding.SearchedAuthorItemBinding;
import net.veldor.flibustaloader.interfaces.MyAdapterInterface;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.ui.OPDSActivity;
import net.veldor.flibustaloader.utils.SortHandler;

import java.util.ArrayList;

public class FoundedAuthorsAdapter extends RecyclerView.Adapter<FoundedAuthorsAdapter.ViewHolder> implements MyAdapterInterface {
    private ArrayList<Author> mAuthors = new ArrayList<>();
    private LayoutInflater mLayoutInflater;

    public FoundedAuthorsAdapter(ArrayList<Author> arrayList) {
        if (arrayList != null && arrayList.size() > 0) {
            mAuthors = arrayList;
        }
    }

    @NonNull
    @Override
    public FoundedAuthorsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        SearchedAuthorItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.searched_author_item, viewGroup, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FoundedAuthorsAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind(mAuthors.get(i));
    }

    @Override
    public int getItemCount() {
        if (mAuthors != null) {
            return mAuthors.size();
        }
        return 0;
    }

    public void setContent(ArrayList<Author> newData) {
        if(newData == null){
            mAuthors = new ArrayList<>();
            notifyDataSetChanged();
        }
        else if(newData.size() == 0 && mAuthors.size() == 0){
            Toast.makeText(App.getInstance(), "Авторы не найдены",Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
        }
        else{
            int previousArrayLen = mAuthors.size();
            mAuthors.addAll(newData);
            notifyItemRangeInserted(previousArrayLen, newData.size());
        }
    }

    public void sort() {
        SortHandler.sortAuthors(mAuthors);
        notifyDataSetChanged();
        Toast.makeText(App.getInstance(), "Авторы отсортированы!",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void clearList() {
        mAuthors = new ArrayList<>();
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;
        private final View mRootView;
        private Author mAuthor;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            View container = mBinding.getRoot();

            mRootView = container.findViewById(R.id.rootView);

            container.setOnClickListener(view -> {
                if (mAuthor.uri != null) {
                    App.getInstance().mSelectedAuthor.postValue(mAuthor);
                } else {
                    // поиск новых книг автора
                    App.getInstance().mAuthorNewBooks.postValue(mAuthor);
                }
                // сообщу, по какому именно элементу был клик
                OPDSActivity.sClickedItemIndex = mAuthors.indexOf(mAuthor);
            });
        }

        void bind(final Author foundedAuthor) {
            mBinding.setVariable(BR.author, foundedAuthor);
            mBinding.executePendingBindings();
            mAuthor = foundedAuthor;
            if(OPDSActivity.sElementForSelectionIndex >= 0 && mAuthors.size() > OPDSActivity.sElementForSelectionIndex && mAuthors.indexOf(mAuthor) == OPDSActivity.sElementForSelectionIndex){
                mRootView.setBackgroundColor(App.getInstance().getResources().getColor(R.color.selected_item_background));
            }
            else{
                mRootView.setBackground(App.getInstance().getResources().getDrawable(R.drawable.author_layout));
            }
        }
    }
}
