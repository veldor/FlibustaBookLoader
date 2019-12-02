package net.veldor.flibustaloader.database.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class ReadedBooks {
    @PrimaryKey
    public long id;

    public String bookId;
}
