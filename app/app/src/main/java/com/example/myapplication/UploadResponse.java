package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

public class UploadResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    public UploadResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
