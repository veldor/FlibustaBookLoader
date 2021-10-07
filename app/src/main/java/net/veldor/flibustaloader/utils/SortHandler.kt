package net.veldor.flibustaloader.utils

import android.os.Build
import net.veldor.flibustaloader.selections.*
import java.util.*

object SortHandler {
    @kotlin.jvm.JvmStatic
    fun sortBooks(books: ArrayList<FoundedEntity>, sortOption: Int) {
        books.sortWith(Comparator sort@{ lhs: FoundedEntity, rhs: FoundedEntity ->
            when (sortOption) {
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
                    if (lhs.sequencesComplex.isEmpty()) {
                        return@sort 1
                    }
                    if (rhs.sequencesComplex.isEmpty()) {
                        return@sort -1
                    }
                    if (lhs.sequencesComplex == rhs.sequencesComplex) return@sort 0
                    return@sort if (lhs.sequencesComplex > rhs.sequencesComplex) 1 else -1
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
                6 -> {
                    // сортирую по названию книги
                    if (lhs.downloaded == rhs.downloaded) return@sort 0
                    return@sort if (lhs.downloaded) 1 else -1
                }
                7 -> {
                    // сортирую по названию книги
                    if (lhs.read == rhs.read) return@sort 0
                    return@sort if (lhs.read) 1 else -1
                }
                else -> {
                    if (lhs.name == rhs.name) return@sort 0
                    return@sort if (lhs.name!! > rhs.name!!) 1 else -1
                }
            }
        })
    }

    @kotlin.jvm.JvmStatic
    fun sortAuthors(mAuthors: ArrayList<FoundedEntity>, sortOption: Int) {
        // отсортирую результат
        mAuthors.sortWith(Comparator sort@{ lhs: FoundedEntity, rhs: FoundedEntity ->
            when (sortOption) {
                1 -> {
                    if (lhs.name == rhs.name) return@sort 0
                    return@sort if (lhs.name!! > rhs.name!!) -1 else 1
                }
                2 -> {
                    // сортирую по размеру
                    val size1 = lhs.name!!.replace("[^\\d]".toRegex(), "").toInt()
                    val size2 = rhs.name!!.replace("[^\\d]".toRegex(), "").toInt()
                    if (size1 == size2) return@sort 0
                    return@sort if (size1 < size2) 1 else -1
                }
                3 -> {
                    // сортирую по размеру
                    val size1 = lhs.name!!.replace("[^\\d]".toRegex(), "").toInt()
                    val size2 = rhs.name!!.replace("[^\\d]".toRegex(), "").toInt()
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

    @kotlin.jvm.JvmStatic
    fun sortList(list: ArrayList<FoundedEntity>, sortOption: Int) {
        // отсортирую результат
        list.sortWith sort@{ lhs: FoundedEntity, rhs: FoundedEntity ->
            when (sortOption) {
                1 -> {
                    if (lhs.name == rhs.name) return@sort 0
                    return@sort if (lhs.name!! > rhs.name!!) -1 else 1
                }
                0 -> {
                    if (lhs.name == rhs.name) return@sort 0
                    // сортирую по названию книги
                    return@sort lhs.name!!.compareTo(rhs.name!!)
                }
                else -> {
                    if (lhs.name == rhs.name) return@sort 0
                    return@sort lhs.name!!.compareTo(rhs.name!!)
                }
            }
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
                        return@sort if (firstBook.file!!.length() < secondBook.file!!.length()) 1 else -1
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