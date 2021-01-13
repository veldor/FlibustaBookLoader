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
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.databinding.DownloadScheduleBookItemBinding;
import net.veldor.flibustaloader.workers.DownloadBooksWorker;

import java.util.List;

public class DownloadScheduleAdapter extends RecyclerView.Adapter<DownloadScheduleAdapter.ViewHolder> {
    private List<BooksDownloadSchedule> mBooks;
    private LayoutInflater mLayoutInflater;

    public DownloadScheduleAdapter(List<BooksDownloadSchedule> value) {
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
        viewHolder.bind(mBooks.get(i));
    }

    @Override
    public int getItemCount() {
        return mBooks.size();
    }

    public void setData(List<BooksDownloadSchedule> booksDownloadSchedules) {
        mBooks = booksDownloadSchedules;
    }


    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(final BooksDownloadSchedule scheduleItem) {
            mBinding.setVariable(BR.book, scheduleItem);
            mBinding.executePendingBindings();
            // добавлю действие при клике на кнопку скачивания
            View container = mBinding.getRoot();
            View deleteItem = container.findViewById(R.id.deleteItemBtn);
            if(deleteItem != null){
                deleteItem.setOnClickListener(view -> {
                    // найду в очереди данную книгу и удалю её из очереди
                        DownloadBooksWorker.removeFromQueue(scheduleItem);
                        //DownloadScheduleAdapter.this.notifyDataSetChanged();
                        Toast.makeText(App.getInstance(), "Книга удалена из очереди скачивания", Toast.LENGTH_LONG).show();
                });
            }
        }
    }
}
