package net.veldor.flibustaloader.view_models

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.handlers.DownloadLinkHandler
import net.veldor.flibustaloader.interfaces.MyViewModelInterface
import net.veldor.flibustaloader.selections.WebViewParseResult
import net.veldor.flibustaloader.ui.BaseActivity
import net.veldor.flibustaloader.utils.XMLParser
import java.io.InputStream

class DownloadScheduleViewModel(application: Application) : OPDSViewModel(application),
    MyViewModelInterface {
    fun loadDownloadQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            schedule.postValue(ArrayList(App.instance.mDatabase.booksDownloadScheduleDao().allBooks!!.toMutableList()))
        }
    }
    companion object {
        val schedule: MutableLiveData<ArrayList<BooksDownloadSchedule?>> = MutableLiveData()
        val downloadState: MutableLiveData<Boolean> = MutableLiveData(false)
    }
}