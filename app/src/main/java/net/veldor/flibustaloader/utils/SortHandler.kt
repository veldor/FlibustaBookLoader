package net.veldor.flibustaloader.utils

import android.os.Build
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.selections.*
import java.util.*

object SortHandler {
    @kotlin.jvm.JvmStatic
    fun sortBooks(books: ArrayList<FoundedBook>) {
        if (App.instance.mBookSortOption != -1 && books.size > 0) {
            books.sortWith(Comparator sort@{ lhs: FoundedBook, rhs: FoundedBook ->
                when (App.instance.mBookSortOption) {
                    1 -> {
                        var size1 = 0
                        var size2 = 0
                        // сортирую по размеру
                        val size11 = lhs.size!!.replace("[^\\d]".toRegex(), "")
                        val size21 = rhs.size!!.replace("[^\\d]".toRegex(), "")
                        if (size11.isNotEmpty()) size1 = size11.toInt()
                        if (size21.isNotEmpty()) size2 = size21.toInt()
                        if (size1 == size2) return@sort 0
                        return@sort if (size1 < size2) 1 else -1
                    }
                    2 -> {
                        // сортирую по количеству загрузок
                        val quantity1 = lhs.downloadsCount
                        val quantity2 = rhs.downloadsCount
                        var downloads1 = 0
                        var downloads2 = 0
                        if (quantity1 != null && quantity1.isNotEmpty()) {
                            downloads1 = quantity1.replace("[^\\d]".toRegex(), "").toInt()
                        }
                        if (quantity2 != null && quantity2.isNotEmpty()) {
                            downloads2 = quantity2.replace("[^\\d]".toRegex(), "").toInt()
                        }
                        if (downloads1 == downloads2) return@sort 0
                        return@sort if (downloads1 < downloads2) 1 else -1
                    }
                    3 -> {
                        // сортировка по серии
                        if (lhs.sequenceComplex!!.isEmpty()) {
                            return@sort 1
                        }
                        if (rhs.sequenceComplex!!.isEmpty()) {
                            return@sort -1
                        }
                        if (lhs.sequenceComplex == rhs.sequenceComplex) return@sort 0
                        return@sort if (lhs.sequenceComplex!! > rhs.sequenceComplex!!) 1 else -1
                    }
                    4 -> {
                        // сортировка по серии
                        if (lhs.genreComplex == null || lhs.genreComplex.isNullOrEmpty()) {
                            return@sort 1
                        }
                        if (rhs.genreComplex == null || rhs.genreComplex.isNullOrEmpty()) {
                            return@sort -1
                        }
                        if (lhs.genreComplex == rhs.genreComplex) return@sort 0
                        return@sort if (lhs.genreComplex!! > rhs.genreComplex!!) 1 else -1
                    }
                    5 -> {
                        // сортировка по серии
                        if (lhs.author!!.isEmpty()) {
                            return@sort 1
                        }
                        if (rhs.author!!.isEmpty()) {
                            return@sort -1
                        }
                        if (lhs.author == rhs.author) return@sort 0
                        return@sort if (lhs.author!! > rhs.author!!) 1 else -1
                    }
                    0 -> {
                        // сортирую по названию книги
                        if (lhs.name == rhs.name) return@sort 0
                        return@sort if (lhs.name!! > rhs.name!!) 1 else -1
                    }
                    else -> {
                        if (lhs.name == rhs.name) return@sort 0
                        return@sort if (lhs.name!! > rhs.name!!) 1 else -1
                    }
                }
            })
        }
    }

    @kotlin.jvm.JvmStatic
    fun sortAuthors(mAuthors: ArrayList<Author>) {
        // отсортирую результат
        if (App.instance.mAuthorSortOptions != -1 && mAuthors.size > 0) {
            mAuthors.sortWith(Comparator sort@{ lhs: Author, rhs: Author ->
                when (App.instance.mAuthorSortOptions) {
                    1 -> {
                        if (lhs.name == rhs.name) return@sort 0
                        return@sort if (lhs.name!! > rhs.name!!) -1 else 1
                    }
                    2 -> {
                        // сортирую по размеру
                        val size1 = lhs.content!!.replace("[^\\d]".toRegex(), "").toInt()
                        val size2 = rhs.content!!.replace("[^\\d]".toRegex(), "").toInt()
                        if (size1 == size2) return@sort 0
                        return@sort if (size1 < size2) 1 else -1
                    }
                    3 -> {
                        // сортирую по размеру
                        val size1 = lhs.content!!.replace("[^\\d]".toRegex(), "").toInt()
                        val size2 = rhs.content!!.replace("[^\\d]".toRegex(), "").toInt()
                        if (size1 == size2) return@sort 0
                        return@sort if (size1 < size2) -1 else 1
                    }
                    0 -> {
                        // сортирую по названию книги
                        if (lhs.name == rhs.name) return@sort 0
                        return@sort lhs.name!!.compareTo(rhs.name!!)
                    }
                    else -> {
                        if (lhs.name == rhs.name) return@sort 0
                        return@sort lhs.name!!.compareTo(rhs.name!!)
                    }
                }
            })
        }
    }

    @kotlin.jvm.JvmStatic
    fun sortGenres(genres: ArrayList<Genre>) {
        // отсортирую результат
        if (App.instance.mOtherSortOptions != -1 && genres.size > 0) {
            genres.sortWith sort@{ lhs: Genre, rhs: Genre ->
                when (App.instance.mOtherSortOptions) {
                    1 -> {
                        if (lhs.label == rhs.label) return@sort 0
                        return@sort if (lhs.label!! > rhs.label!!) -1 else 1
                    }
                    0 -> {
                        if (lhs.label == rhs.label) return@sort 0
                        // сортирую по названию книги
                        return@sort lhs.label!!.compareTo(rhs.label!!)
                    }
                    else -> {
                        if (lhs.label == rhs.label) return@sort 0
                        return@sort lhs.label!!.compareTo(rhs.label!!)
                    }
                }
            }
        }
    }

    @kotlin.jvm.JvmStatic
    fun sortSequences(sequences: ArrayList<FoundedSequence>) {
        // отсортирую результат
        if (App.instance.mOtherSortOptions != -1 && sequences.size > 0) {
            sequences.sortWith(Comparator sort@{ lhs: FoundedSequence, rhs: FoundedSequence ->
                when (App.instance.mOtherSortOptions) {
                    1 -> {
                        if (lhs.title == rhs.title) return@sort 0
                        return@sort if (lhs.title!! > rhs.title!!) -1 else 1
                    }
                    0 -> {
                        if (lhs.title == rhs.title) return@sort 0
                        // сортирую по названию книги
                        return@sort lhs.title!!.compareTo(rhs.title!!)
                    }
                    else -> {
                        if (lhs.title == rhs.title) return@sort 0
                        return@sort lhs.title!!.compareTo(rhs.title!!)
                    }
                }
            })
        }
    }

    @kotlin.jvm.JvmStatic
    fun sortLoadedBooks(mItems: ArrayList<Book>, which: Int, isInvert: Boolean) {
        mItems.sortWith sort@{ lhs: Book, rhs: Book ->
            var firstBook = lhs
            var secondBook = rhs
            if (isInvert) {
                // поменяю переменные местами
                firstBook = rhs
                secondBook = lhs
            }
            when (which) {
                0 ->                     // сортирую по названию книги
                    return@sort firstBook.name!!.compareTo(secondBook.name!!)
                1 -> {
                    // сортирую по размеру
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        return@sort if (firstBook.file!!.length()  < secondBook.file!!.length()) 1 else -1
                    }
                    return@sort if (firstBook.fileCompat!!.length() < secondBook.fileCompat!!.length()) 1 else -1
                }
                3 ->                     // отсортирую по формату
                    return@sort firstBook.extension!!.compareTo(secondBook.extension!!)
                4 ->                     // отсортирую по формату
                    return@sort firstBook.author!!.compareTo(secondBook.author!!)
                5 -> {
                    // отсортирую по времени добавления
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        return@sort if (firstBook.file!!.lastModified() < secondBook.file!!.lastModified()) 1 else -1
                    }
                    return@sort if (firstBook.fileCompat!!.lastModified() < secondBook.fileCompat!!.lastModified()) 1 else -1
                }
            }
            0
        }
    }
}