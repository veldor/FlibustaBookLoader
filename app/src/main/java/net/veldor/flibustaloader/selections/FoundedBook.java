package net.veldor.flibustaloader.selections;

import net.veldor.flibustaloader.database.entity.ReadedBooks;

import java.util.ArrayList;

public class FoundedBook implements FoundedItem {
    public String name;
    public String author;
    public String downloadsCount;
    public String translate;
    public ArrayList<DownloadLink> downloadLinks = new ArrayList<>();
    public String size;
    public String format;
    public String genreComplex;
    public ArrayList<Genre> genres = new ArrayList<>();
    public String sequenceComplex;
    public ArrayList<FoundedSequence> sequences = new ArrayList<>();
    public ArrayList<Author> authors = new ArrayList<>();
    public String bookInfo;
    public String id;
    public boolean readed;
}
