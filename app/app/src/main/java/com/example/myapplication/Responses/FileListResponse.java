package com.example.myapplication.Responses;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FileListResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("files")
    private List<FileMetadata> files;

    public String getStatus() {
        return status;
    }

    public List<FileMetadata> getFiles() {
        return files;
    }
}