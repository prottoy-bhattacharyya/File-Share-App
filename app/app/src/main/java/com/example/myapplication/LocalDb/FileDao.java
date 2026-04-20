package com.example.myapplication.LocalDb;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FileDao {
    @Insert
    void insertFile(LocalFile file);

    @Query("SELECT * FROM local_files WHERE uniqueText = :code")
    List<LocalFile> getFilesByCode(String code);

    @Query("SELECT * FROM local_files ORDER BY uploadTimestamp DESC")
    List<LocalFile> getAllFiles();

    @Delete
    void deleteFile(LocalFile file);
}