package com.example.myapplication.Responses;

import com.google.gson.annotations.SerializedName;

public class getFileCountResponse {

    @SerializedName("file_count")
    private Integer fileCount;
    @SerializedName("status")
    private String status;
    @SerializedName("message")
    private String message;

    public Integer getFileCount() {
        return fileCount;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

}
