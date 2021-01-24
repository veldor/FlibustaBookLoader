package net.veldor.flibustaloader.selections;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.Serializable;

public class Book implements FoundedItem, Serializable {
    public String name;
    public String author;
    public String size;
    public String extension;
    public DocumentFile file;
    public File fileCompat;
}
