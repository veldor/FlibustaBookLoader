package net.veldor.flibustaloader.database.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity
public class ReadedBooks {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NotNull
    public String bookId = "";
}
