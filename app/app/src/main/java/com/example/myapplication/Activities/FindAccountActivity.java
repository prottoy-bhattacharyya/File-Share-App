package com.example.myapplication.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Apis.NetworkClient;
import com.example.myapplication.Apis.UploadApis;
import com.example.myapplication.R;
import com.example.myapplication.Responses.FindAccoundResponse;
import com.google.android.material.textfield.TextInputEditText;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class FindAccountActivity extends AppCompatActivity {
    EditText etIdentifier;
    Button btnSendOtp;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_account);

        etIdentifier = findViewById(R.id.et_find_identifier);
        btnSendOtp = findViewById(R.id.btn_send_otp);
        progressBar = findViewById(R.id.recovery_progress);

        btnSendOtp.setOnClickListener(v -> {
            String identifier = etIdentifier.getText().toString().trim();
            if (identifier.isEmpty()) {
                Toast.makeText(this, "Enter your email or username", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            Retrofit retrofit = NetworkClient.getRetrofit(this);
            UploadApis api = retrofit.create(UploadApis.class);

            api.findAccoundAndSendOtp(identifier).enqueue(new Callback<FindAccoundResponse>() {
                @Override
                public void onResponse(Call<FindAccoundResponse> call, Response<FindAccoundResponse> response) {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body().getStatus().equals("success")) {

                        Intent intent = new Intent(FindAccountActivity.this, CheckOtpActivity.class);
                        intent.putExtra("user_email", response.body().getEmail());
                        startActivity(intent);

                    } else {
                        Toast.makeText(FindAccountActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<FindAccoundResponse> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(FindAccountActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}