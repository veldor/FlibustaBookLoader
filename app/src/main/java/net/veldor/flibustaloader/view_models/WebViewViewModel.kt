package net.veldor.flibustaloader.view_models

import android.app.Application
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

class WebViewViewModel(application: Application) : OPDSViewModel(application),
    MyViewModelInterface {

    val pageParseResult: MutableLiveData<WebViewParseResult?> = MutableLiveData()

    fun parseText() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = XMLParser.searchDownloadLinks(pageText.value)
            pageParseResult.postValue(result)
        }
    }

    fun webViewDownload(i: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // найду все ссылки выбранного типа
            val selectedType = pageParseResult.value?.types?.get(i)
            val listOfDownloadLinks = ArrayList<String>()
            if(selectedType != null){
                // пройдусь по списку ссылок и найду подходящие
                pageParseResult.value?.linksList?.forEach {
                    if(it.value == selectedType){
                        listOfDownloadLinks.add(it.key)
                    }
                }
            }
            var link: BooksDownloadSchedule
            if(listOfDownloadLinks.size > 0){
                // добавлю каждую ссылку в очередь скачивания
                listOfDownloadLinks.forEach {
                    link = BooksDownloadSchedule()
                    link.link = it
                    DownloadLinkHandler().addDownloadSchedule(link)
                    BaseActivity.sLiveDownloadScheduleCountChanged.postValue(true)
                }
                App.instance.requestDownloadBooksStart()
            }
        }
    }

    companion object {
        val pageText: MutableLiveData<InputStream> = MutableLiveData()
    }
}