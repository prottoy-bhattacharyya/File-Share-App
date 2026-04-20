package com.example.myapplication.LocalDb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_files")
public class LocalFile {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String fileName;
    public String filePath;
    public String uniqueText; // e.g., "abcd"
    public long uploadTimestamp;
}