package com.example.myapplication.Responses;

import com.google.gson.annotations.SerializedName;

public class FileMetadata {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
}