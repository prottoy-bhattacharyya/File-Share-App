package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class UserLocalStore {
    private static final String USER_KEY = "user";
    SharedPreferences sharedPreferences;

    public UserLocalStore(Context context) {
        sharedPreferences = context.getSharedPreferences(USER_KEY, Context.MODE_PRIVATE);
    }

    public boolean setUser(String username, String password) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("username", username);
            editor.putString("password", password);
            editor.putBoolean("checkLoggedIn",true);
            editor.apply();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public String getUsername() {
        return sharedPreferences.getString("username", null);
    }
    public String getPassword() {
        return sharedPreferences.getString("password", null);
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean("checkLoggedIn", false);
    }
    public void clearData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
