package com.example.myapplication.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Apis.NetworkClient;
import com.example.myapplication.Apis.UploadApis;
import com.example.myapplication.R;
import com.example.myapplication.Responses.VarifyOtpResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CheckOtpActivity extends AppCompatActivity {
    EditText etOtp;
    TextView tvEmail;
    Button btnVerify;
    String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_otp);

        userEmail = getIntent().getStringExtra("user_email");
        etOtp = findViewById(R.id.et_otp_code);
        btnVerify = findViewById(R.id.btn_verify_otp);
        tvEmail = findViewById(R.id.tv_email);

        tvEmail.append(" " + userEmail);


        btnVerify.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();

            Retrofit retrofit = NetworkClient.getRetrofit(this);
            UploadApis api = retrofit.create(UploadApis.class);

            api.verifyOtp(otp, userEmail).enqueue(new Callback<VarifyOtpResponse>() {
                @Override
                public void onResponse(Call<VarifyOtpResponse> call, Response<VarifyOtpResponse> response) {
                    if (response.isSuccessful()) {
                        // Pass both Email and OTP to the final step
                        Intent intent = new Intent(CheckOtpActivity.this, NewPasswordActivity.class);
                        intent.putExtra("user_email", userEmail);
                        intent.putExtra("otp_code", otp);
                        startActivity(intent);
                    } else {
                        Toast.makeText(CheckOtpActivity.this, "Invalid OTP code", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<VarifyOtpResponse> call, Throwable t) {
                    Toast.makeText(CheckOtpActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}