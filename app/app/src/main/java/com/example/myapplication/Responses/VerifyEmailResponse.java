package com.example.myapplication.Responses;

import com.google.gson.annotations.SerializedName;

public class VerifyEmailResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    // Getters
    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
