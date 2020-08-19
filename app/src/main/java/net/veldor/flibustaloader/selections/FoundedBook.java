package net.veldor.flibustaloader.selections;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;

public class FoundedBook implements FoundedItem, Serializable {
    public String name;
    public String author;
    public String downloadsCount;
    public String translate;
    public final ArrayList<DownloadLink> downloadLinks = new ArrayList<>();
    public String size;
    public String format;
    public String genreComplex;
    public final ArrayList<Genre> genres = new ArrayList<>();
    public String sequenceComplex;
    public final ArrayList<FoundedSequence> sequences = new ArrayList<>();
    public final ArrayList<Author> authors = new ArrayList<>();
    public String bookInfo;
    public String id;
    public boolean read;
    public boolean downloaded;
    public String previewUrl;
    public String preferredFormat;
    public String bookLink;
    public String bookLanguage;

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }
}
