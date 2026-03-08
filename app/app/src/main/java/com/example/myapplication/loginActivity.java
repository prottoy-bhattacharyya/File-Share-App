package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class loginActivity extends AppCompatActivity {

    Button login, guest_btn;
    TextView signup;
    EditText input_username;
    EditText input_password;
    String url;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        // The callback can be enabled or disabled here or in handleOnBackPressed()
        login = findViewById(R.id.button_login);
        guest_btn = findViewById(R.id.button_guest);
        signup = findViewById(R.id.text_signup);
        input_username = findViewById(R.id.input_username);
        input_password = findViewById(R.id.input_password);
        url = getResources().getString(R.string.server_url) + "/check_login/?username=";

        login.setOnClickListener(v -> {
            login.setEnabled(false);
            login.setAlpha(0.5f);
            startLoginProcess();
        });

        signup.setOnClickListener(v -> {
            Intent signupPage = new Intent(loginActivity.this, signupActivity.class);
            startActivity(signupPage);
        });

        guest_btn.setOnClickListener(v -> {
            Intent mainPage = new Intent(loginActivity.this, MainActivity.class);
            startActivity(mainPage);
        });
    }



    public void launchMainActivity() {
        Intent mainPage = new Intent(loginActivity.this, MainActivity.class);
        startActivity(mainPage);
    }

    public void startLoginProcess() {
        String username = input_username.getText().toString().trim();
        String password = input_password.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            login.setEnabled(true);
            login.setAlpha(1f);
            return;
        }

        RequestBody usernameBody = RequestBody.create(MediaType.parse("text/plain"), username);
        RequestBody passwordBody = RequestBody.create(MediaType.parse("text/plain"), password);

        Retrofit retrofit = NetworkClient.getRetrofit(getApplicationContext());
        UploadApis uploadApis = retrofit.create(UploadApis.class);

        retrofit2.Call<LoginResponse> call = uploadApis.login(usernameBody, passwordBody);
        call.enqueue(new retrofit2.Callback<LoginResponse>() {
            @Override
            public void onResponse(retrofit2.Call<LoginResponse> call, retrofit2.Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null){
                    LoginResponse ServerResponse = response.body();
                    if (ServerResponse.getStatus().equals("success")){
                        String username = ServerResponse.getUsername();
                        String fullname = ServerResponse.getFullname();
                        String email = ServerResponse.getEmail();
                        String message = ServerResponse.getMessage();
                        String password = input_password.getText().toString();
                        UserLocalStore userLocalStore = new UserLocalStore(getApplicationContext());

                        userLocalStore.setUser(fullname, email, username, password);
                        runOnUiThread(()->{
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                            launchMainActivity();
                        });
                    }
                    else {
                        String message = ServerResponse.getMessage();
                        runOnUiThread(()->{
                            login.setEnabled(true);
                            login.setAlpha(1f);
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        });
                    }
                }
                else {
                    runOnUiThread(()->{
                        login.setEnabled(true);
                        login.setAlpha(1f);
                        Toast.makeText(getApplicationContext(), "No Server Response", Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onFailure(retrofit2.Call<LoginResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Failed to Login " + t.getMessage(), Toast.LENGTH_LONG).show();
                    login.setEnabled(true);
                    login.setAlpha(1f);
                });
            }
        });
    }
}