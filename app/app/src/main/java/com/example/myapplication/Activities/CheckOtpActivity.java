package com.example.myapplication.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Apis.NetworkClient;
import com.example.myapplication.Apis.UploadApis;
import com.example.myapplication.R;
import com.example.myapplication.Responses.FindAccoundResponse;
import com.example.myapplication.Responses.VarifyOtpResponse;
import com.example.myapplication.UserLocalStore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CheckOtpActivity extends AppCompatActivity {
    EditText etOtp;
    TextView tvEmail;
    Button btnVerify, resendOtp;
    String userEmail, type;
    ProgressBar recoveryProgress;
    private CountDownTimer countDownTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_otp);

        userEmail = getIntent().getStringExtra("user_email");
        type = getIntent().getStringExtra("type");

        etOtp = findViewById(R.id.et_otp_code);
        btnVerify = findViewById(R.id.btn_verify_otp);
        tvEmail = findViewById(R.id.tv_email);
        recoveryProgress = findViewById(R.id.recovery_progress);
        resendOtp = findViewById(R.id.btn_resend_otp);


        if (userEmail != null) {
            tvEmail.append(" " + userEmail);
        }
        else {
            tvEmail.append(" " + "Unknown");
        }



        startTimer();




        btnVerify.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.isEmpty()) return; // Don't send empty OTP

            recoveryProgress.setVisibility(ProgressBar.VISIBLE);

            Retrofit retrofit = NetworkClient.getRetrofit(this);
            UploadApis api = retrofit.create(UploadApis.class);

            api.verifyOtp(otp, userEmail).enqueue(new Callback<VarifyOtpResponse>() {
                @Override
                public void onResponse(Call<VarifyOtpResponse> call, Response<VarifyOtpResponse> response) {
                    recoveryProgress.setVisibility(ProgressBar.GONE);

                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().getStatus().equals("success")) {

                            if ("email_verify".equals(type)) {

                                UserLocalStore userLocalStore = new UserLocalStore(CheckOtpActivity.this);
                                userLocalStore.setIsVerified(true);

                                Intent intent = new Intent(CheckOtpActivity.this, UserProfileActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            } else {
                                Intent intent = new Intent(CheckOtpActivity.this, NewPasswordActivity.class);
                                intent.putExtra("user_email", userEmail);
                                startActivity(intent);
                            }
                        } else {
                            Toast.makeText(CheckOtpActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(CheckOtpActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<VarifyOtpResponse> call, Throwable t) {
                    recoveryProgress.setVisibility(ProgressBar.GONE);
                    Toast.makeText(CheckOtpActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        resendOtp.setOnClickListener(v -> {
            recoveryProgress.setVisibility(ProgressBar.VISIBLE);

            startTimer();


            Retrofit retrofit = NetworkClient.getRetrofit(this);
            UploadApis api = retrofit.create(UploadApis.class);

            api.findAccoundAndSendOtp(userEmail).enqueue(new Callback<FindAccoundResponse>() {
                @Override
                public void onResponse(Call<FindAccoundResponse> call, Response<FindAccoundResponse> response) {

                    recoveryProgress.setVisibility(ProgressBar.GONE);

                    if (response.isSuccessful() && response.body().getStatus().equals("success")) {
                        Toast.makeText(CheckOtpActivity.this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CheckOtpActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<FindAccoundResponse> call, Throwable t) {

                    recoveryProgress.setVisibility(ProgressBar.GONE);
                    Toast.makeText(CheckOtpActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendOtp.setEnabled(false);

                resendOtp.setText("Resend in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                resendOtp.setEnabled(true);
                resendOtp.setText("Resend OTP");
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Prevent memory leaks
        }
    }
}