package com.example.myapplication.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Apis.NetworkClient;
import com.example.myapplication.Apis.UploadApis;
import com.example.myapplication.R;
import com.example.myapplication.Responses.ResetPasswordResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class NewPasswordActivity extends AppCompatActivity {
    EditText etNewPassword;
    Button btnReset;
    String userEmail, otpCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);

        // Get everything passed from the previous steps
        userEmail = getIntent().getStringExtra("user_email");
        otpCode = getIntent().getStringExtra("otp_code");

        etNewPassword = findViewById(R.id.et_final_password);
        btnReset = findViewById(R.id.btn_reset_final);

        btnReset.setOnClickListener(v -> {
            String newPass = etNewPassword.getText().toString().trim();

            Retrofit retrofit = NetworkClient.getRetrofit(this);
            UploadApis api = retrofit.create(UploadApis.class);

            api.resetPassword(userEmail, newPass).enqueue(new Callback<ResetPasswordResponse>() {
                @Override
                public void onResponse(Call<ResetPasswordResponse> call, Response<ResetPasswordResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(NewPasswordActivity.this, "Success! Login with your new password.", Toast.LENGTH_LONG).show();

                        // Clear backstack so they can't go "back" into recovery
                        Intent intent = new Intent(NewPasswordActivity.this, loginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<ResetPasswordResponse> call, Throwable t) {
                    Toast.makeText(NewPasswordActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}