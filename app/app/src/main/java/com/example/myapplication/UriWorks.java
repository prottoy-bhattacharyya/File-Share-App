package com.example.myapplication;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class UriWorks {
    public String getFileNameFromUri(Uri uri, ContentResolver contentResolver) {
        String fileName = null;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameColumnIndex >= 0) {
                fileName = cursor.getString(nameColumnIndex);
            }
        }
        return fileName;
    }

    public File create_temp_file(Context context, Uri uri, String file_name){
        try {
            // Use getCacheDir() to store a temporary file
            File tempFile = File.createTempFile(file_name, "", context.getCacheDir());

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            return tempFile;
        } catch (Exception e) {
            Toast.makeText(context, "Error processing file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }
}
