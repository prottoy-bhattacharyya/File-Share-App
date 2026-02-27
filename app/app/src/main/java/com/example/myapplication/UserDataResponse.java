package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserDataResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private List<userInfo> data;


    public String getStatus() { return status; }
    public List<userInfo> getData() { return data; }
}
