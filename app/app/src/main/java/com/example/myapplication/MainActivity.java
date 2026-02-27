package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    Button send_buton, receive_buton;
    ImageButton logout_btn;
    TextView username;
    LinearLayout user_info_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        RequestPermissions();

        UserLocalStore userLocalStore = new UserLocalStore(this);

        username = findViewById(R.id.text_username);
        send_buton = findViewById(R.id.send_btn);
        receive_buton = findViewById(R.id.receive_btn);
        logout_btn = findViewById(R.id.button_logout);
        user_info_btn = findViewById(R.id.user_info_btn);

        username.setText(userLocalStore.getUsername());

        if(!userLocalStore.isLoggedIn()){
            username.setText(getResources().getString(R.string.guest));
        }


        user_info_btn.setOnClickListener(view -> {
            if(!userLocalStore.isLoggedIn()){
                Intent intent = new Intent(MainActivity.this, loginActivity.class);
                startActivity(intent);
                return;
            }

            Intent intent = new Intent(MainActivity.this, userInfoActivity.class);
            startActivity(intent);
        });

        logout_btn.setOnClickListener(view -> {
            userLocalStore.clearData();
            Intent loginIntent = new Intent(MainActivity.this, loginActivity.class);
            startActivity(loginIntent);
            Toast.makeText(getApplicationContext(), "Logged Out", Toast.LENGTH_SHORT).show();
        });


        send_buton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, sendActivity.class);
            startActivity(intent);
        });

        receive_buton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, receiveActivity.class);
            startActivity(intent);
        });


    }

    public void RequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES
            }, 1);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
        }
    }
}