package com.example.myapplication.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Apis.NetworkClient;
import com.example.myapplication.Apis.UploadApis;
import com.example.myapplication.R;
import com.example.myapplication.Responses.ResetPasswordResponse;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class NewPasswordActivity extends AppCompatActivity {
    EditText etNewPassword;
    Button btnReset;
    String userEmail, otpCode;
    TextView text_Password_strength;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);

        signupActivity signupActivity = new signupActivity();

        // Get everything passed from the previous steps
        userEmail = getIntent().getStringExtra("user_email");
        otpCode = getIntent().getStringExtra("otp_code");

        etNewPassword = findViewById(R.id.et_final_password);
        btnReset = findViewById(R.id.btn_reset_final);
        text_Password_strength = findViewById(R.id.text_password_strength);

        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newPass = s.toString();
                text_Password_strength.setVisibility(TextView.VISIBLE);
                text_Password_strength.setText("");
                ArrayList<String> errors = signupActivity.isStrongPassword(newPass);
                if (!errors.isEmpty()) {
                    text_Password_strength.setTextColor(Color.RED);
                    for (String error : errors) {
                        text_Password_strength.append("• " + error + "\n");
                    }

                    btnReset.setAlpha(0.5f);
                    btnReset.setEnabled(false);
                }
                else {
                    btnReset.setAlpha(1f);
                    btnReset.setEnabled(true);
                    text_Password_strength.setText("Password is strong");
                    text_Password_strength.setTextColor(Color.GREEN);
                }
            }
        });

        btnReset.setOnClickListener(v -> {
            btnReset.setAlpha(0.5f);
            btnReset.setEnabled(false);

            String newPass = etNewPassword.getText().toString().trim();

            Retrofit retrofit = NetworkClient.getRetrofit(this);
            UploadApis api = retrofit.create(UploadApis.class);

            api.resetPassword(userEmail, newPass).enqueue(new Callback<ResetPasswordResponse>() {
                @Override
                public void onResponse(Call<ResetPasswordResponse> call, Response<ResetPasswordResponse> response) {

                    btnReset.setAlpha(1f);
                    btnReset.setEnabled(true);

                    if (response.isSuccessful() && response.body().getStatus().equals("success")) {
                        Toast.makeText(NewPasswordActivity.this, "Success! Login with your new password.", Toast.LENGTH_LONG).show();

                        // Clear backstack so they can't go "back" into recovery
                        Intent intent = new Intent(NewPasswordActivity.this, loginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                    else{
                        Toast.makeText(NewPasswordActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResetPasswordResponse> call, Throwable t) {
                    btnReset.setAlpha(1f);
                    btnReset.setEnabled(true);
                    Toast.makeText(NewPasswordActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}