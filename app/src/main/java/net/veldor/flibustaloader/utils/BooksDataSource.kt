package net.veldor.flibustaloader.utils

import android.util.Log
import androidx.paging.PositionalDataSource
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.veldor.flibustaloader.selections.Book
import java.util.ArrayList

internal class BooksDataSource(val list: ArrayList<Book>) :
    PositionalDataSource<Book>() {
    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<Book>
    ) {
        GlobalScope.launch {
            if (list.isNotEmpty()) {
                if (params.requestedLoadSize + params.requestedStartPosition < list.size) {
                    callback.onResult(
                        list, 0, list.size
                    )
                } else {
                    callback.onResult(
                        list.subList(
                            params.requestedStartPosition,
                            params.requestedLoadSize
                        ), 0, list.size
                    )
                }
            } else {
                callback.onResult(listOf(), 0)
            }
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Book>) {
        Log.d("surprise", "loadRange: start position is ${params.startPosition}")
        Log.d("surprise", "loadRange: load size is ${params.loadSize}")
        if (list.size > (params.startPosition + params.loadSize)) {
            callback.onResult(
                list.subList(
                    params.startPosition,
                    params.startPosition + params.loadSize
                )
            )
        } else {
            Log.d("surprise", "loadRange: load end of list")
            callback.onResult(
                list.subList(
                    params.startPosition,
                    list.size
                )
            )
        }
    }
}