package net.veldor.flibustaloader.ui.fragments

import net.veldor.flibustaloader.utils.MimeTypes.getMime
import net.veldor.flibustaloader.utils.MimeTypes.getFullMime
import net.veldor.flibustaloader.view_models.SubscriptionsViewModel
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibustaloader.R
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibustaloader.App
import androidx.appcompat.widget.SwitchCompat
import android.widget.CompoundButton
import android.widget.Toast
import net.veldor.flibustaloader.adapters.SubscribeResultsAdapter
import android.widget.TextView
import net.veldor.flibustaloader.selections.DownloadLink
import android.annotation.SuppressLint
import android.content.DialogInterface
import net.veldor.flibustaloader.selections.FoundedBook
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import net.veldor.flibustaloader.utils.MyFileReader.SUBSCRIPTIONS_FILE
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.util.ArrayList

class SubscriptionsResultFragment : Fragment() {
    private lateinit var mRoot: View
    private lateinit var mAdapter: SubscribeResultsAdapter
    private lateinit var mViewModel: SubscriptionsViewModel
    private lateinit var mNothingFoundView: TextView
    private var mFragmentVisible = true
    private lateinit var mContext: Context
    private lateinit var mRecycler: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        mRoot = inflater.inflate(R.layout.fragment_subscribe_results, container, false)
        setupUI()
        mContext = requireContext()
        mViewModel = ViewModelProvider(this).get(SubscriptionsViewModel::class.java)

        // попробую рассериализировать объект
        deserealizeSubscriptions()

        // буду отслеживать появление книг по подпискам
        addObservers()
        return mRoot
    }

    override fun onPause() {
        super.onPause()
        mFragmentVisible = false
    }

    override fun onResume() {
        super.onResume()
        mFragmentVisible = true
    }

    private fun addObservers() {
        val data = mViewModel.checkData
        data.observe(viewLifecycleOwner, { aBoolean: Boolean ->
            if (aBoolean) {
                deserealizeSubscriptions()
            }
        })

        // добавлю отслеживание получения списка ссылок на скачивание
        val downloadLinks: LiveData<ArrayList<DownloadLink>> = App.instance.mDownloadLinksList
        downloadLinks.observe(viewLifecycleOwner, { downloadLinks1: ArrayList<DownloadLink>? ->
            if (downloadLinks1 != null && downloadLinks1.size > 0 && mFragmentVisible) {
                if (downloadLinks1.size == 1) {
                    // добавлю книгу в очередь скачивания
                    mViewModel.addToDownloadQueue(downloadLinks1[0])
                    Toast.makeText(
                        context,
                        R.string.book_added_to_schedule_message,
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // покажу диалог для выбора ссылки для скачивания
                    showDownloadsDialog(downloadLinks1)
                }
                App.instance.mDownloadLinksList.value = null
            }
        })


        // отслеживание загруженной книги
        val downloadedBook: LiveData<String> = App.instance.mLiveDownloadedBookId
        downloadedBook.observe(viewLifecycleOwner, { downloadedBookId: String? ->
            if (downloadedBookId != null && mRecycler.adapter is SubscribeResultsAdapter) {
                (mRecycler.adapter as SubscribeResultsAdapter?)!!.bookDownloaded(downloadedBookId)
                App.instance.mLiveDownloadedBookId.postValue(null)
            }
        })
    }

    private fun showDownloadsDialog(downloadLinks: ArrayList<DownloadLink>) {
        val inflate = layoutInflater
        @SuppressLint("InflateParams") val view =
            inflate.inflate(R.layout.confirm_book_type_select, null)
        var checker: SwitchCompat? = view.findViewById(R.id.save_only_selected)
        if (checker != null) {
            checker.isChecked = PreferencesHandler.instance.saveOnlySelected
            checker.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    Toast.makeText(
                        context,
                        "Выбранный формат будет запомнен и в последующем, если книга будет недоступна в данном формате- она будет пропущена. Опцию можно сбросить в настройках",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
        checker = view.findViewById(R.id.reDownload)
        if (checker != null) {
            checker.isChecked = PreferencesHandler.instance.isReDownload
            checker.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    Toast.makeText(
                        context,
                        "Ранее загруженные книги будут перезаписаны. Опцию можно сбросить в настройках",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
        checker = view.findViewById(R.id.save_type_selection)
        checker?.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                Toast.makeText(
                    context,
                    "Формат будет запомнен и окно с предложением выбора больше не будет выводиться. Опцию можно сбросить в настройках",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        val dialogBuilder = AlertDialog.Builder(
            mContext
        )
        dialogBuilder.setTitle(R.string.downloads_dialog_header)
        // получу список типов данных
        val linksLength = downloadLinks.size
        val linksArray = arrayOfNulls<String>(linksLength)
        var counter = 0
        var mime: String?
        while (counter < linksLength) {
            mime = downloadLinks[counter].mime
            linksArray[counter] = getMime(mime!!)
            counter++
        }
        dialogBuilder.setItems(linksArray) { dialogInterface: DialogInterface, i: Int ->
            // проверю, выбрано ли сохранение формата загрузки
            val dialog = dialogInterface as Dialog
            var switcher: SwitchCompat = dialog.findViewById(R.id.save_type_selection)
            if (switcher.isChecked) {
                // запомню выбор формата
                PreferencesHandler.instance.favoriteMime = linksArray[i]
            }
            switcher = dialog.findViewById(R.id.save_only_selected)
            PreferencesHandler.instance.saveOnlySelected = switcher.isChecked
            switcher = dialog.findViewById(R.id.reDownload)
            PreferencesHandler.instance.isReDownload = switcher.isChecked
            // получу сокращённый MIME
            val shortMime = linksArray[i]
            val longMime = getFullMime(shortMime)
            var counter1 = 0
            val linksLength1 = downloadLinks.size
            var item: DownloadLink
            while (counter1 < linksLength1) {
                item = downloadLinks[counter1]
                if (item.mime == longMime) {
                    mViewModel.addToDownloadQueue(item)
                    break
                }
                counter1++
            }
            Toast.makeText(context, "Книга добавлена в очередь загрузок", Toast.LENGTH_SHORT).show()
        }
            .setView(view)
        val preparedDialog = dialogBuilder.create()
        if (mFragmentVisible) {
            preparedDialog.show()
        }
    }

    private fun setupUI() {
        mRecycler = mRoot.findViewById(R.id.resultsList)
        mRecycler.setLayoutManager(LinearLayoutManager(context))
        mAdapter = SubscribeResultsAdapter()
        mRecycler.setAdapter(mAdapter)
        mNothingFoundView = mRoot.findViewById(R.id.booksNotFoundText)
    }

    private fun deserealizeSubscriptions() {
        var fis: FileInputStream? = null
        var ois: ObjectInputStream? = null
        val arraylist = ArrayList<FoundedBook>()
        try {
            val autocompleteFile = File(App.instance.filesDir, SUBSCRIPTIONS_FILE)
            if (autocompleteFile.exists()) {
                fis = FileInputStream(autocompleteFile)
                ois = ObjectInputStream(fis)
                val list = ois.readObject() as ArrayList<*>
                if (list.size > 0) {
                    for (book in list) {
                        if (book is FoundedBook) {
                            arraylist.add(book)
                        }
                    }
                    mAdapter.setContent(arraylist)
                    mNothingFoundView.visibility = View.GONE
                } else {
                    mAdapter.setContent(arraylist)
                    mNothingFoundView.visibility = View.VISIBLE
                }
                ois.close()
                fis.close()
            } else {
                Log.d(
                    "surprise",
                    "SubscriptionsActivity deserealizeSubscriptions: serializable file not exists"
                )
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        } catch (c: ClassNotFoundException) {
            println("Class not found")
            c.printStackTrace()
        } finally {
            try {
                ois?.close()
                fis?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}