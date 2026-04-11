package com.example.myapplication.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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

public class FindAccountActivity extends AppCompatActivity {

    TextInputEditText input_identifier;
    Button btn_send_otp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_find_account);

        initWorks();
        exqListener();
    }

    private void initWorks() {
        input_identifier = findViewById(R.id.et_find_identifier);
        btn_send_otp = findViewById(R.id.btn_send_otp);
    }

    private void exqListener() {
        btn_send_otp.setOnClickListener(v -> {
            sendOtp();
        });
    }


    private void sendOtp(){
        String identifier = input_identifier.getText().toString();
        UploadApis api = NetworkClient.getRetrofit(this).create(UploadApis.class);
        api.findAccoundAndSendOtp(identifier).enqueue(new Callback<FindAccoundResponse>() {
            @Override
            public void onResponse(Call<FindAccoundResponse> call, Response<FindAccoundResponse> response) {
                if (response.isSuccessful() && response.body() != null){
                    FindAccoundResponse ServerResponse = response.body();
                    if (ServerResponse.getStatus().equals("success")){
                        Intent intent = new Intent(FindAccountActivity.this, otpActivity.class);
                        intent.putExtra("identifier", identifier);
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onFailure(Call<FindAccoundResponse> call, Throwable t) {

            }
        });

    }
}