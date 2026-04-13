package com.example.myapplication.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.myapplication.Apis.NetworkClient;
import com.example.myapplication.Apis.UploadApis;
import com.example.myapplication.R;
import com.example.myapplication.UserLocalStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    Button send_buton, receive_buton, direct_transfer_btn;
    ImageButton logout_btn;
    TextView username;
    ImageView profile_image;
    ConstraintLayout user_profile_btn;
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
        profile_image = findViewById(R.id.profile_image);
        send_buton = findViewById(R.id.send_btn);
        direct_transfer_btn = findViewById(R.id.direct_transfer_btn);
        receive_buton = findViewById(R.id.receive_btn);
        logout_btn = findViewById(R.id.button_logout);
        user_profile_btn = findViewById(R.id.user_profile_btn);

        userLocalStore = new UserLocalStore(MainActivity.this);

        if (userLocalStore.isLoggedIn()) {
            username.setText(userLocalStore.getFullname());
            getProfileFromServer();
            updateProfileUI();
        }

        else {
            username.setText(getResources().getString(R.string.guest));
            send_buton.setEnabled(false);
            receive_buton.setEnabled(false);
            send_buton.setAlpha(0.5f);
            receive_buton.setAlpha(0.5f);

            removeProfileImage();
            updateProfileUI();

        }
    }

    public void exqListener() {
        user_profile_btn.setOnClickListener(view -> {
            if(!userLocalStore.isLoggedIn()){
                Intent intent = new Intent(MainActivity.this, loginActivity.class);
                startActivity(intent);
                return;
            }

            Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
            startActivity(intent);
        });

        logout_btn.setOnClickListener(view -> {
            userLocalStore.clearData();
            removeProfileImage();
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (userLocalStore.isLoggedIn()){
                    androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle("Exit the app ?")

                            .setPositiveButton("Exit", (d, w) -> {
                                setEnabled(false);
                                finishAffinity();
                            })
                            .setNegativeButton("Cancel", null)
                            .create();

                    dialog.show();

                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                            .setTextColor(android.graphics.Color.RED);
                }
                else {
                    finish();
                }
            }
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

    private void updateProfileUI() {

        File profile_pic = new File(getFilesDir(), "profile_image.jpg");
        if (profile_pic.exists()){
            Glide.with(this)
                    .load(profile_pic)
                    .signature(new com.bumptech.glide.signature.ObjectKey(profile_pic.lastModified()))
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(profile_image);
        }
    }

    private void removeProfileImage() {
        File profile_pic = new File(getFilesDir(), "profile_image.jpg");
        profile_pic.delete();
    }

    private void getProfileFromServer() {

        String username = userLocalStore.getUsername();
        UploadApis api = NetworkClient.getRetrofit(this).create(UploadApis.class);

        api.getUserProfilePicture(username).enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> {
                        try (InputStream inputStream = response.body().byteStream();
                             FileOutputStream outputStream = new FileOutputStream(new File(getFilesDir(),
                                                                            "profile_image.jpg"))) {

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            outputStream.flush();

                            runOnUiThread(()->{
                                updateProfileUI();
                            });
                        } catch (IOException e) {
                            runOnUiThread(() -> {
                                Toast.makeText(getApplicationContext(),
                                                "Server error: " + e.getMessage(),
                                                Toast.LENGTH_SHORT)
                                                .show();
                            });
                        }
                    }).start();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                runOnUiThread(()->{
                    Toast.makeText(getApplicationContext(),
                                    "Network failure: " + t.getMessage(),
                                    Toast.LENGTH_SHORT)
                                    .show();
                });
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileUI();
    }
}