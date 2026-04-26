package com.example.myapplication.Responses;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserHistoryResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;
    @SerializedName("data")
    private List<userInfo> data;


    public String getStatus() { return status; }
    public List<userInfo> getData() { return data; }
    public String getMessage() { return message; }
}
