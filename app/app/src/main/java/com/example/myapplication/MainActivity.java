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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Button send_buton, receive_buton, direct_transfer_btn;
    ImageButton logout_btn;
    TextView username;
    LinearLayout user_info_btn;
    UserLocalStore userLocalStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        RequestPermissions();
        init_works();
        exqListener();
    }

    public void init_works() {


        username = findViewById(R.id.text_username);
        send_buton = findViewById(R.id.send_btn);
        direct_transfer_btn = findViewById(R.id.direct_transfer_btn);
        receive_buton = findViewById(R.id.receive_btn);
        logout_btn = findViewById(R.id.button_logout);
        user_info_btn = findViewById(R.id.user_info_btn);

        userLocalStore = new UserLocalStore(MainActivity.this);

        username.setText(userLocalStore.getUsername());

        if(!userLocalStore.isLoggedIn()){
            username.setText(getResources().getString(R.string.guest));
        }
    }

    public void exqListener() {
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
        direct_transfer_btn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, directTransferActivity.class);
            startActivity(intent);
        });
    }

    private void RequestPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), 100);
        }
    }
}