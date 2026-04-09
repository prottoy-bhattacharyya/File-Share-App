package com.example.myapplication.Activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.Apis.NetworkClient;
import com.example.myapplication.Apis.UploadApis;
import com.example.myapplication.R;
import com.example.myapplication.Responses.ProfilePicResponse;
import com.example.myapplication.UriWorks;
import com.example.myapplication.UserLocalStore;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UserProfileActivity extends AppCompatActivity {
    TextView tv_fullname, tv_username, tv_email, error_msg;
    ImageView profile_image;
    MaterialButton btn_user_history, btn_received_files;
    UserLocalStore userLocalStore;
    UriWorks uriWorks = new UriWorks();

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
        profile_image  = findViewById(R.id.profile_image);
        error_msg = findViewById(R.id.error_msg);

        userLocalStore = new UserLocalStore(UserProfileActivity.this);
        tv_fullname.setText(userLocalStore.getFullname());
        tv_username.setText(userLocalStore.getUsername());
        tv_email.setText(userLocalStore.getEmail());

        updateProfileUI();
    }

    void exqListener(){
        btn_user_history.setOnClickListener(view -> {
            Intent intent = new Intent(UserProfileActivity.this, UserHistoryActivity.class);
            startActivity(intent);
        });
        btn_received_files.setOnClickListener(view -> {
            Intent intent = new Intent(UserProfileActivity.this, ReceivedFilesActivity.class);
            startActivity(intent);
        });

        profile_image.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 100);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null){
            Uri profileImageUri = data.getData();

            Context context = getApplicationContext();
            uriWorks.saveProfileImage(context, profileImageUri);

            UpdateProfileToServer();

            updateProfileUI();
        }
    }

    private void updateProfileUI() {
        File profile_pic = new File(getFilesDir(), "profile_image.jpg");
        Glide.with(this)
                .load(profile_pic)
                .signature(new com.bumptech.glide.signature.ObjectKey(profile_pic.lastModified()))
                .circleCrop()
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_account_circle)
                .into(profile_image);
    }

    private void UpdateProfileToServer() {
        UserLocalStore userLocalStore = new UserLocalStore(getApplicationContext());
        String username = userLocalStore.getUsername();

        File profilePic = new File(getFilesDir(), "profile_image.jpg");
        if (!profilePic.exists()) {
            Toast.makeText(this, "No image found to upload", Toast.LENGTH_SHORT).show();
            error_msg.setText("No image found to upload");
            return;
        }

        RequestBody username_body = RequestBody.create(MediaType.parse("text/plain"), username);
        RequestBody profileFileBody = RequestBody.create(MediaType.parse("image/jpeg"), profilePic);

        MultipartBody.Part parts = MultipartBody.Part.createFormData("profilePicture",
                                                                            profilePic.getName(),
                                                                            profileFileBody
                                                                    );

        Retrofit retrofit = NetworkClient.getRetrofit(getApplicationContext());
        UploadApis uploadApis = retrofit.create(UploadApis.class);

        Call<ProfilePicResponse> call = uploadApis.setUserProfilePicture(parts, username_body);
        call.enqueue(new Callback<ProfilePicResponse>() {
            @Override
            public void onResponse(Call<ProfilePicResponse> call, Response<ProfilePicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProfilePicResponse serverResponse = response.body();
                    String message = serverResponse.getMessage();
                    String status = serverResponse.getStatus();
                    if (status.equals("success")) {
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                    }
                    else {
                        error_msg.setVisibility(View.VISIBLE);
                        error_msg.setText("error: " + message);
                        error_msg.setTextColor(getResources().getColor(R.color.red));
                    }
                } else {
                    Log.e("ProfilePic", "Server error code: " + response.code());
                    error_msg.setVisibility(View.VISIBLE);
                    error_msg.setText("Server error: " + response.code());
                    error_msg.setTextColor(getResources().getColor(R.color.red));
                }
            }

            @Override
            public void onFailure(Call<ProfilePicResponse> call, Throwable t) {
                error_msg.setVisibility(View.VISIBLE);
                error_msg.setText("Network failure: " + t.getMessage());
                Log.e("ProfilePic", "Failure", t);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileUI();
    }
}