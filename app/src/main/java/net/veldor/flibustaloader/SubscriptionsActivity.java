package net.veldor.flibustaloader;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.veldor.flibustaloader.adapters.SubscribeResultsAdapter;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.view_models.MainViewModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import static net.veldor.flibustaloader.utils.MyFileReader.SUBSCRIPTIONS_FILE;

public class SubscriptionsActivity extends AppCompatActivity {
    private SubscribeResultsAdapter mAdapter;
    private MainViewModel mMyViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribtions);
        // добавлю viewModel
        mMyViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        RecyclerView recycler = findViewById(R.id.resultsList);
        recycler.setLayoutManager(new LinearLayoutManager(SubscriptionsActivity.this));
        mAdapter = new SubscribeResultsAdapter();
        recycler.setAdapter(mAdapter);

        // попробую рассериализировать объект
        deserealizeSubscriptions();
    }

    private void deserealizeSubscriptions() {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        ArrayList<FoundedBook> arraylist = new ArrayList<>();
        try {
            File autocompleteFile = new File(App.getInstance().getFilesDir(), SUBSCRIPTIONS_FILE);
            if (autocompleteFile.exists()) {
                fis = new FileInputStream(autocompleteFile);
                ois = new ObjectInputStream(fis);
                ArrayList list = (ArrayList) ois.readObject();
                if (list.size() > 0) {
                    for (Object book :
                            list) {
                        if (book instanceof FoundedBook) {
                            arraylist.add((FoundedBook) book);
                        }
                    }
                    mAdapter.setContent(arraylist);
                }
                ois.close();
                fis.close();
            } else {
                Log.d("surprise", "SubscriptionsActivity deserealizeSubscriptions: serializable file not exists");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
