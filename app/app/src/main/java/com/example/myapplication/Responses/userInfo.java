package com.example.myapplication.Responses;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class userInfo {
    @SerializedName("sender")
    private String sender;

    @SerializedName("receiver")
    private String receiver;

    @SerializedName("unique_text")
    private String uniqueText;
    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("file_names")
    private List<String> file_names;

    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getUniqueText() { return uniqueText; }
    public String getTimestamp() { return  timestamp; }
    public List<String> getFileNames() { return file_names; }
}
