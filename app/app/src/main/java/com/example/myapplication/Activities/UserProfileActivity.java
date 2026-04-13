package com.example.myapplication.Activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import com.example.myapplication.Responses.FindAccoundResponse;
import com.example.myapplication.Responses.ProfilePicUploadResponse;
import com.example.myapplication.Responses.VerifyEmailResponse;
import com.example.myapplication.UriWorks;
import com.example.myapplication.UserLocalStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UserProfileActivity extends AppCompatActivity {
    TextView tv_fullname, tv_username, tv_email;
    ImageView profile_image;
    MaterialButton btn_user_history, btn_received_files, btn_verify_email;
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
        btn_verify_email = findViewById(R.id.btn_verify_email);

        userLocalStore = new UserLocalStore(UserProfileActivity.this);
        tv_fullname.setText(userLocalStore.getFullname());
        tv_username.setText(userLocalStore.getUsername());
        tv_email.setText(userLocalStore.getEmail());

        btn_verify_email.setEnabled(true);
        btn_verify_email.setAlpha(1f);

        updateProfileUI();

        updateEmailVarification();
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

        btn_verify_email.setOnClickListener(view -> {
            btn_verify_email.setEnabled(false);
            btn_verify_email.setAlpha(0.5f);
            sendVerificationEmail();

        });
    }

    private void sendVerificationEmail() {
        Retrofit retrofit = NetworkClient.getRetrofit(getApplicationContext());
        UploadApis uploadApis = retrofit.create(UploadApis.class);

        uploadApis.findAccoundAndSendOtp(userLocalStore.getEmail()).enqueue(new Callback<FindAccoundResponse>() {
            @Override
            public void onResponse(Call<FindAccoundResponse> call, Response<FindAccoundResponse> response) {
                try {
                    if (response.isSuccessful() && response.body().getStatus().equals("success")) {
                        Intent intent = new Intent(UserProfileActivity.this, CheckOtpActivity.class);
                        intent.putExtra("user_email", userLocalStore.getEmail());
                        intent.putExtra("type", "email_verify");
                        startActivity(intent);
                    }
                    else {
                        showError(response.body().getMessage());
                    }
                }
                catch (Exception e){
                    showError(e.getMessage());
                }

            }

            @Override
            public void onFailure(Call<FindAccoundResponse> call, Throwable t) {
                showError(t.getMessage());
            }
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
            showError("No profile picture found");
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

        Call<ProfilePicUploadResponse> call = uploadApis.setUserProfilePicture(parts, username_body);
        call.enqueue(new Callback<ProfilePicUploadResponse>() {
            @Override
            public void onResponse(Call<ProfilePicUploadResponse> call, Response<ProfilePicUploadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProfilePicUploadResponse serverResponse = response.body();
                    String message = serverResponse.getMessage();
                    String status = serverResponse.getStatus();
                    if (status.equals("success")) {
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                    }
                    else {
                        showError(response.body().getMessage());
                    }
                } else {
                    Log.e("ProfilePic", "Server error code: " + response.code());
                    showError("Server error code: " + response.code() + "\n" + response.message());
                }
            }

            @Override
            public void onFailure(Call<ProfilePicUploadResponse> call, Throwable t) {
                showError(t.getMessage());
                Log.e("ProfilePic", "Failure", t);
            }
        });
    }

    private void updateEmailVarification() {
        if (userLocalStore.getIsVerified()) {
            btn_verify_email.setText("Verified");
            btn_verify_email.setIcon(getDrawable(R.drawable.ic_check));
            btn_verify_email.setBackgroundColor(Color.GREEN);
            btn_verify_email.setEnabled(false);

            return;
        }

        UploadApis uploadApis = NetworkClient.getRetrofit(this).create(UploadApis.class);
        uploadApis.checkEmailVerification(userLocalStore.getEmail()).enqueue(new Callback<VerifyEmailResponse>() {
            @Override
            public void onResponse(Call<VerifyEmailResponse> call, Response<VerifyEmailResponse> response) {
                // SAFE CHECK
                if (response.isSuccessful() && response.body() != null) {
                    if ("success".equals(response.body().getStatus())) {
                        userLocalStore.setIsVerified(true);
                    } else {
                        userLocalStore.setIsVerified(false);

                    }
                } else {
                    userLocalStore.setIsVerified(false);
                    showError("Verification check failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<VerifyEmailResponse> call, Throwable t) {
                userLocalStore.setIsVerified(false);
                showError("Verification check failed: " + t.getMessage());
            }
        });
    }

    private void showError(String message) {
        MaterialCardView errorCard = findViewById(R.id.error_card);
        TextView errorText = findViewById(R.id.error_msg);

        errorText.setText(message);

        // Smooth Slide Down Animation
        errorCard.setVisibility(View.VISIBLE);
        errorCard.setAlpha(0f);
        errorCard.setTranslationY(-100f);

        errorCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .start();

        // Auto-hide after 3 seconds
        new android.os.Handler().postDelayed(() -> {
            errorCard.animate()
                    .alpha(0f)
                    .translationY(-100f)
                    .setDuration(400)
                    .withEndAction(() -> errorCard.setVisibility(View.GONE))
                    .start();
        }, 3000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileUI();
        updateEmailVarification();
    }
}