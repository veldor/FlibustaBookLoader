package net.veldor.flibustaloader.utils;

import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SortHandler {
    public static void sortBooks(ArrayList<FoundedItem> books) {
        if (App.getInstance().mBookSortOption != -1 && books != null && books.size() > 0) {
            Collections.sort(books, new Comparator<FoundedItem>() {
                @Override
                public int compare(FoundedItem lhs, FoundedItem rhs) {
                    FoundedBook fb1 = (FoundedBook) lhs;
                    FoundedBook fb2 = (FoundedBook) rhs;
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    switch (App.getInstance().mBookSortOption) {
                        case 1:
                            int size1 = 0;
                            int size2 = 0;
                            // сортирую по размеру
                            String size_1 = fb1.size.replaceAll("[^\\d]", "");
                            String size_2 = fb2.size.replaceAll("[^\\d]", "");
                            if (!size_1.isEmpty())
                                size1 = Integer.parseInt(size_1);
                            if (!size_2.isEmpty())
                                size2 = Integer.parseInt(size_2);
                            return size1 < size2 ? 1 : -1;
                        case 2:
                            // сортирую по количеству загрузок
                            String quantity1 = fb1.downloadsCount;
                            String quantity2 = fb2.downloadsCount;
                            int downloads1 = 0;
                            int downloads2 = 0;
                            if(quantity1 != null && !quantity1.isEmpty()){
                                downloads1 = Integer.parseInt(quantity1.replaceAll("[^\\d]", ""));
                            }
                            if(quantity2 != null && !quantity2.isEmpty()){
                                downloads2 = Integer.parseInt(quantity2.replaceAll("[^\\d]", ""));
                            }
                            return downloads1 < downloads2 ? 1 : -1;
                        case 3:
                            // сортировка по серии
                            if (fb1.sequenceComplex.isEmpty()) {
                                return 1;
                            }
                            if (fb2.sequenceComplex.isEmpty()) {
                                return -1;
                            }
                            return fb1.sequenceComplex.compareTo(fb2.sequenceComplex) > 0 ? 1 : -1;
                        case 4:
                            // сортировка по серии
                            if (fb1.genreComplex.isEmpty()) {
                                return 1;
                            }
                            if (fb2.genreComplex.isEmpty()) {
                                return -1;
                            }
                            return fb1.genreComplex.compareTo(fb2.genreComplex) > 0 ? 1 : -1;
                        case 5:
                            // сортировка по серии
                            if (fb1.author.isEmpty()) {
                                return 1;
                            }
                            if (fb2.author.isEmpty()) {
                                return -1;
                            }
                            return fb1.author.compareTo(fb2.author) > 0 ? 1 : -1;
                        default:
                        case 0:
                            // сортирую по названию книги
                            return fb1.name.compareTo(fb2.name);
                    }
                }
            });
        }
    }

    public static void sortAuthors(ArrayList mAuthors) {
        // отсортирую результат
        if (App.getInstance().mAuthorSortOptions != -1 && mAuthors != null && mAuthors.size() > 0) {
            Collections.sort(mAuthors, new Comparator<FoundedItem>() {
                @Override
                public int compare(FoundedItem lhs, FoundedItem rhs) {
                    Author fa1 = (Author) lhs;
                    Author fa2 = (Author) rhs;
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    switch (App.getInstance().mAuthorSortOptions) {
                        case 1:
                            return fa1.name.compareTo(fa2.name) > 0 ? -1 : 1;
                        case 2:
                            // сортирую по размеру
                            int size1 = Integer.parseInt(fa1.content.replaceAll("[^\\d]", ""));
                            int size2 = Integer.parseInt(fa2.content.replaceAll("[^\\d]", ""));
                            return size1 < size2 ? 1 : -1;
                        case 3:
                            // сортирую по размеру
                            size1 = Integer.parseInt(fa1.content.replaceAll("[^\\d]", ""));
                            size2 = Integer.parseInt(fa2.content.replaceAll("[^\\d]", ""));
                            return size1 < size2 ? -1 : 1;
                        default:
                        case 0:
                            // сортирую по названию книги
                            return fa1.name.compareTo(fa2.name);
                    }
                }
            });
        }
    }

    public static void sortGenres(ArrayList genres) {
        // отсортирую результат
        if (App.getInstance().mOtherSortOptions != -1 && genres != null && genres.size() > 0) {

            Collections.sort(genres, new Comparator<FoundedItem>() {
                @Override
                public int compare(FoundedItem lhs, FoundedItem rhs) {
                    Genre fa1 = (Genre) lhs;
                    Genre fa2 = (Genre) rhs;
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    switch (App.getInstance().mOtherSortOptions) {
                        case 1:
                            return fa1.label.compareTo(fa2.label) > 0 ? -1 : 1;
                        case 0:
                        default:
                            // сортирую по названию книги
                            return fa1.label.compareTo(fa2.label);
                    }
                }
            });
        }
    }

    public static void sortSequences(ArrayList sequences) {
        // отсортирую результат
        if (App.getInstance().mOtherSortOptions != -1 && sequences != null && sequences.size() > 0) {
            Collections.sort(sequences, new Comparator<FoundedItem>() {
                @Override
                public int compare(FoundedItem lhs, FoundedItem rhs) {
                    FoundedSequence fa1 = (FoundedSequence) lhs;
                    FoundedSequence fa2 = (FoundedSequence) rhs;
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    switch (App.getInstance().mOtherSortOptions) {
                        case 1:
                            return fa1.title.compareTo(fa2.title) > 0 ? -1 : 1;
                        case 0:
                        default:
                            // сортирую по названию книги
                            return fa1.title.compareTo(fa2.title);
                    }
                }
            });
        }
    }
}
