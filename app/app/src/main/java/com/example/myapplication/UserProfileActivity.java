package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class UserProfileActivity extends AppCompatActivity {
    TextView tv_fullname, tv_username, tv_email;
    MaterialButton btn_user_history, btn_received_files;
    UserLocalStore userLocalStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        init_works();
        exqListener();
    }

    void init_works(){
        tv_fullname = findViewById(R.id.tv_fullname);
        tv_username = findViewById(R.id.tv_username);
        tv_email = findViewById(R.id.tv_email);
        btn_user_history = findViewById(R.id.btn_user_history);
        btn_received_files = findViewById(R.id.btn_received_files);

        userLocalStore = new UserLocalStore(UserProfileActivity.this);
        tv_fullname.setText(userLocalStore.getFullname());
        tv_username.setText(userLocalStore.getUsername());
        tv_email.setText(userLocalStore.getEmail());
    }

    void exqListener(){
        btn_user_history.setOnClickListener(view -> {
            Intent intent = new Intent(UserProfileActivity.this, UserHistoryActivity.class);
            startActivity(intent);
        });
        btn_received_files.setOnClickListener(view -> {

        });
    }
}