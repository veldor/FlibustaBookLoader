package net.veldor.flibustaloader.utils

import android.util.Log
import java.util.*

class History private constructor() {
    private val mHistory = Stack<String>()
    private val clickHistory = Stack<Int>()
    fun addToClickHistory(position: Int) {
        clickHistory.push(position)
    }
    fun addToHistory(url: String) {
        Log.d("surprise", "addToHistory: add to history $url")
        mHistory.push(url)
    }

    val isEmpty: Boolean
        get() = mHistory.size == 0

    //        Log.d("surprise", "History getLastPage 34: first backward? " + firstBackward + ", history size is " + mHistory.size());
//        if (mHistory.size() > 1) {
//            mHistory.pop();
//            Log.d("surprise", "History getLastPage 38: delete item from history");
//            String item = mHistory.pop();
//            Log.d("surprise", "History getLastPage 36: left history size " + mHistory.size() + " " + firstBackward);
//            return item;
//        } else if (mHistory.size() == 1 && !firstBackward) {
//            return mHistory.peek();
//        }
//        else if(mHistory.size() > 0){
//            return mHistory.pop();
//        }
//        return null;
    val lastPage: String?
        get() = if (mHistory.size > 0) {
            mHistory.pop()
        } else null
    val lastClickedElementIndex: Int
        get() = if (clickHistory.size > 0) {
            clickHistory.pop()
        } else -1

    //        Log.d("surprise", "History getLastPage 34: first backward? " + firstBackward + ", history size is " + mHistory.size());
//        if (mHistory.size() > 1) {
//            mHistory.pop();
//            Log.d("surprise", "History getLastPage 38: delete item from history");
//            String item = mHistory.pop();
//            Log.d("surprise", "History getLastPage 36: left history size " + mHistory.size() + " " + firstBackward);
//            return item;
//        } else if (mHistory.size() == 1 && !firstBackward) {
//            return mHistory.peek();
//        }
//        else if(mHistory.size() > 0){
//            return mHistory.pop();
//        }
//        return null;
    fun showLastPage(): String {
        return mHistory.peek()
    }

    fun isOneElementInQueue(): Boolean {
        return mHistory.size == 1
    }

    companion object {
        @kotlin.jvm.JvmStatic
        var instance: History? = null
            get() {
                if (field == null) {
                    field = History()
                }
                return field
            }
            private set
    }
}