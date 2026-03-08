package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

public class userInfo {
    @SerializedName("sender")
    private String sender;

    @SerializedName("receiver")
    private String receiver;

    @SerializedName("unique_text")
    private String uniqueText;

    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getUniqueText() { return uniqueText; }
}
