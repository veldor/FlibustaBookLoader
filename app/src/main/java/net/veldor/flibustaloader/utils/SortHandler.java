package net.veldor.flibustaloader.utils;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SortHandler {
    public static void sortBooks(ArrayList<FoundedBook> books) {
        if (App.getInstance().mBookSortOption != -1 && books != null && books.size() > 0) {
            Collections.sort(books, (lhs, rhs) -> {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                switch (App.getInstance().mBookSortOption) {
                    case 1:
                        int size1 = 0;
                        int size2 = 0;
                        // сортирую по размеру
                        String size_1 = lhs.size.replaceAll("[^\\d]", "");
                        String size_2 = rhs.size.replaceAll("[^\\d]", "");
                        if (!size_1.isEmpty())
                            size1 = Integer.parseInt(size_1);
                        if (!size_2.isEmpty())
                            size2 = Integer.parseInt(size_2);
                        if (size1 == size2)
                            return 0;
                        return size1 < size2 ? 1 : -1;
                    case 2:
                        // сортирую по количеству загрузок
                        String quantity1 = lhs.downloadsCount;
                        String quantity2 = rhs.downloadsCount;
                        int downloads1 = 0;
                        int downloads2 = 0;
                        if (quantity1 != null && !quantity1.isEmpty()) {
                            downloads1 = Integer.parseInt(quantity1.replaceAll("[^\\d]", ""));
                        }
                        if (quantity2 != null && !quantity2.isEmpty()) {
                            downloads2 = Integer.parseInt(quantity2.replaceAll("[^\\d]", ""));
                        }
                        if (downloads1 == downloads2)
                            return 0;
                        return downloads1 < downloads2 ? 1 : -1;
                    case 3:
                        // сортировка по серии
                        if (lhs.sequenceComplex.isEmpty()) {
                            return 1;
                        }
                        if (rhs.sequenceComplex.isEmpty()) {
                            return -1;
                        }
                        if (lhs.sequenceComplex.equals(rhs.sequenceComplex))
                            return 0;
                        return lhs.sequenceComplex.compareTo(rhs.sequenceComplex) > 0 ? 1 : -1;
                    case 4:
                        // сортировка по серии
                        if (lhs.genreComplex == null || lhs.genreComplex.isEmpty()) {
                            return 1;
                        }
                        if (rhs.genreComplex == null || rhs.genreComplex.isEmpty()) {
                            return -1;
                        }
                        if(lhs.genreComplex.equals(rhs.genreComplex))
                            return 0;
                        return lhs.genreComplex.compareTo(rhs.genreComplex) > 0 ? 1 : -1;
                    case 5:
                        // сортировка по серии
                        if (lhs.author.isEmpty()) {
                            return 1;
                        }
                        if (rhs.author.isEmpty()) {
                            return -1;
                        }
                        if(lhs.author.equals(rhs.author))
                            return 0;
                        return lhs.author.compareTo(rhs.author) > 0 ? 1 : -1;
                    default:
                    case 0:
                        // сортирую по названию книги
                        if(lhs.name.equals(rhs.name))
                            return 0;
                        return lhs.name.compareTo(rhs.name) > 0 ? 1 : -1;
                }
            });
        }
    }

    public static void sortAuthors(ArrayList<Author> mAuthors) {
        // отсортирую результат
        if (App.getInstance().mAuthorSortOptions != -1 && mAuthors != null && mAuthors.size() > 0) {
            Collections.sort(mAuthors, (lhs, rhs) -> {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                switch (App.getInstance().mAuthorSortOptions) {
                    case 1:
                        if(lhs.name.equals(rhs.name))
                            return 0;
                        return lhs.name.compareTo(rhs.name) > 0 ? -1 : 1;
                    case 2:
                        // сортирую по размеру
                        int size1 = Integer.parseInt(lhs.content.replaceAll("[^\\d]", ""));
                        int size2 = Integer.parseInt(rhs.content.replaceAll("[^\\d]", ""));
                        if(size1 == size2)
                            return 0;
                        return size1 < size2 ? 1 : -1;
                    case 3:
                        // сортирую по размеру
                        size1 = Integer.parseInt(lhs.content.replaceAll("[^\\d]", ""));
                        size2 = Integer.parseInt(rhs.content.replaceAll("[^\\d]", ""));
                        if(size1 == size2)
                            return 0;
                        return size1 < size2 ? -1 : 1;
                    default:
                    case 0:
                        // сортирую по названию книги
                        if((lhs).name.equals(rhs.name))
                            return 0;
                        return lhs.name.compareTo(rhs.name);
                }
            });
        }
    }

    public static void sortGenres(ArrayList<Genre> genres) {
        // отсортирую результат
        if (App.getInstance().mOtherSortOptions != -1 && genres != null && genres.size() > 0) {

            Collections.sort(genres, (lhs, rhs) -> {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                switch (App.getInstance().mOtherSortOptions) {
                    case 1:
                        if(lhs.label.equals(rhs.label))
                            return 0;
                        return lhs.label.compareTo(rhs.label) > 0 ? -1 : 1;
                    case 0:
                    default:
                        if(lhs.label.equals(rhs.label))
                            return 0;
                        // сортирую по названию книги
                        return lhs.label.compareTo(rhs.label);
                }
            });
        }
    }

    public static void sortSequences(ArrayList<FoundedSequence> sequences) {
        // отсортирую результат
        if (App.getInstance().mOtherSortOptions != -1 && sequences != null && sequences.size() > 0) {
            Collections.sort(sequences, (lhs, rhs) -> {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                switch (App.getInstance().mOtherSortOptions) {
                    case 1:
                        if(lhs.title.equals(rhs.title))
                            return 0;
                        return lhs.title.compareTo(rhs.title) > 0 ? -1 : 1;
                    case 0:
                    default:
                        if(lhs.title.equals(rhs.title))
                            return 0;
                        // сортирую по названию книги
                        return lhs.title.compareTo(rhs.title);
                }
            });
        }
    }
}
