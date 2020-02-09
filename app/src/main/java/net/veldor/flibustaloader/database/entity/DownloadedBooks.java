package net.veldor.flibustaloader.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity
public class DownloadedBooks {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NotNull
    public String bookId = "";
}
