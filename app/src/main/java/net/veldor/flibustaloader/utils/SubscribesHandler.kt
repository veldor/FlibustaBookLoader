package net.veldor.flibustaloader.utils

import android.util.Log
import androidx.lifecycle.MutableLiveData
import net.veldor.flibustaloader.http.UniversalWebClient
import net.veldor.flibustaloader.parsers.TestParser
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.view_models.SubscriptionsViewModel

object SubscribesHandler {

    fun checkSubscribes(foundedEntitiesContainer: MutableLiveData<FoundedEntity>?, isIncremental: Boolean): String? {
        val lastCheckedBookId = PreferencesHandler.instance.lastCheckedBookId
        var lastBookFound = false
        Log.d("surprise", "checkSubscribes: last checked book: $lastCheckedBookId")
        var lastBookId: String? = null
        val resultListContainer = SubscriptionsViewModel.foundedSubscribes
        var previousValue: ArrayList<FoundedEntity>
        val subscribeResults: ArrayList<FoundedEntity> = arrayListOf()
        val bookSubscribes = SubscribeBooks.instance.getSubscribes()
        val authorSubscribes = SubscribeAuthors.instance.getSubscribes()
        val sequenceSubscribes = SubscribeSequences.instance.getSubscribes()
        val genreSubscribes = SubscribeGenre.instance.getSubscribes()
        var textValue: String
        var nextPageLink: String? = "/opds/new/0/new"
        while (nextPageLink != null) {
            Log.d("surprise", "checkSubscribes: check next page")
            val response = UniversalWebClient().rawRequest(nextPageLink)
            val answer = UniversalWebClient().responseToString(response.inputStream)
            if (answer != null) {
                val parser = TestParser(answer)
                val results = parser.parse()
                nextPageLink = parser.nextPageLink
                results.forEach { book ->
                    if(lastBookFound){
                        return@forEach
                    }
                    if(lastBookId == null) {
                        lastBookId = book.id!!
                    }
                    textValue = book.name!!.lowercase()
                    bookSubscribes.forEach {
                        if(textValue.contains(it.name)){
                            book.description = "Подписка по названию книги: ${it.name}"
                            Log.d("surprise", "checkSubscribes:book find $textValue in ${it.name}")
                            subscribeResults.add(book)
                            foundedEntitiesContainer?.postValue(book)
                            previousValue = resultListContainer.value!!
                            previousValue.add(book)
                            resultListContainer.postValue(previousValue)
                        }
                    }
                    textValue = book.author!!.lowercase()
                    authorSubscribes.forEach {
                        if(textValue.contains(it.name)){
                            book.description = "Подписка по автору: ${it.name}"
                            Log.d("surprise", "checkSubscribes:author find $textValue in ${it.name}")
                            subscribeResults.add(book)
                            foundedEntitiesContainer?.postValue(book)
                            previousValue = resultListContainer.value!!
                            previousValue.add(book)
                            resultListContainer.postValue(previousValue)
                        }
                    }
                    textValue = book.sequencesComplex.lowercase()
                    sequenceSubscribes.forEach {
                        if(textValue.contains(it.name)){
                            book.description = "Подписка по серии: ${it.name}"
                            Log.d("surprise", "checkSubscribes:sequence find $textValue in ${it.name}")
                            subscribeResults.add(book)
                            foundedEntitiesContainer?.postValue(book)
                            previousValue = resultListContainer.value!!
                            previousValue.add(book)
                            resultListContainer.postValue(previousValue)
                        }
                    }
                    textValue = book.genreComplex!!.lowercase()
                    genreSubscribes.forEach {
                        if(textValue.contains(it.name)){
                            book.description = "Подписка по жанру: ${it.name}"
                            Log.d("surprise", "checkSubscribes:genre find $textValue in ${it.name}")
                            subscribeResults.add(book)
                            foundedEntitiesContainer?.postValue(book)
                            previousValue = resultListContainer.value!!
                            previousValue.add(book)
                            resultListContainer.postValue(previousValue)
                        }
                    }
                    if(isIncremental && lastCheckedBookId == book.id){
                        Log.d("surprise", "checkSubscribes: scanned for last book")
                        // дальше сканировать не надо
                        nextPageLink = null
                        lastBookFound = true
                        return@forEach

                    }
                }
            } else {
                break
            }
        }
        SubscriptionsViewModel.liveCheckInProgress.postValue(false)
        if(subscribeResults.size > 0){
            Log.d("surprise", "checkSubscribes: found ${subscribeResults.size} books by subscribe")
        }
        else{
            Log.d("surprise", "checkSubscribes: no found books for subscribe")
        }
        return lastBookId
    }
}