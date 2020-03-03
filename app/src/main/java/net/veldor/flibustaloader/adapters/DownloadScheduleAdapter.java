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
import net.veldor.flibustaloader.databinding.DownloadScheduleBookItemBinding;
import net.veldor.flibustaloader.selections.FoundedBook;

import java.util.ArrayList;

public class DownloadScheduleAdapter extends RecyclerView.Adapter<DownloadScheduleAdapter.ViewHolder> {
    public ArrayList mBooks;
    private LayoutInflater mLayoutInflater;

    public DownloadScheduleAdapter(ArrayList<FoundedBook> value) {
        mBooks = value;
    }

    @NonNull
    @Override
    public DownloadScheduleAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        DownloadScheduleBookItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.download_schedule_book_item, viewGroup, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadScheduleAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind((FoundedBook) mBooks.get(i));
    }

    @Override
    public int getItemCount() {
        return mBooks.size();
    }


    public void setContent(ArrayList<FoundedBook> arrayList) {
        mBooks = arrayList;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(final FoundedBook foundedBook) {
            mBinding.setVariable(BR.book, foundedBook);
            mBinding.executePendingBindings();
            // добавлю действие при клике на кнопку скачивания
            View container = mBinding.getRoot();
            View deleteItem = container.findViewById(R.id.deleteItemBtn);
            if(deleteItem != null){
                deleteItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // найду в очереди данную книгу и удалю её из очереди
                        ArrayList<FoundedBook> books = App.getInstance().mDownloadSchedule.getValue();
                        if(books != null && books.size() > 0){
                            books.remove(foundedBook);
                            DownloadScheduleAdapter.this.notifyDataSetChanged();
                            Toast.makeText(App.getInstance(), "Книга удалена из очереди скачивания", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }
    }
}
