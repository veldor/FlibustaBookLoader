package net.veldor.flibustaloader.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity
public class Bookmark{
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NotNull
    public String name = "";

    @NotNull
    public String link = "";
}
