package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("username")
    private String username;

    @SerializedName("fullname")
    private String fullname;

    @SerializedName("email")
    private String email;

    @SerializedName("message")
    private String message;

    public LoginResponse(String status, String username, String fullname, String email, String message){
        this.status = status;
        this.username = username;
        this.fullname = fullname;
        this.email = email;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }
    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
    public String getFullname() {
        return fullname;
    }
    public String getMessage() {
        return message;
    }
}
