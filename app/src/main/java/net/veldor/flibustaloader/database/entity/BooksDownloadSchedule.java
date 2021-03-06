package net.veldor.flibustaloader.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity
public class BooksDownloadSchedule{
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NotNull
    public String bookId = "";
    @NotNull
    public String link = "";
    @NotNull
    public String name = "";
    @NotNull
    public String size = "";
    @NotNull
    public String author = "";
    @NotNull
    public String format = "";

    public String authorDirName = "";

    public String sequenceDirName = "";

    public String reservedSequenceName = "";

    @Ignore
    public boolean loaded;
}
