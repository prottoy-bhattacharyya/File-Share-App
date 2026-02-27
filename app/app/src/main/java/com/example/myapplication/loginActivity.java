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

        String local_url = url + username + "&password=" + password;

        final String[] status = new String[2];


        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(local_url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Failed to Login " + e.getMessage(), Toast.LENGTH_LONG).show();
                    login.setEnabled(true);
                    login.setAlpha(1f);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    status[0] = jsonObject.getString("status");
                    status[1] = jsonObject.getString("message");

                }

                catch (JSONException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Failed to get status" + e.getMessage(), Toast.LENGTH_LONG).show();
                        login.setEnabled(true);
                        login.setAlpha(1f);
                    });
                }
                if (status[0].equals("success")) {
                    UserLocalStore userLocalStore = new UserLocalStore(getApplicationContext());
                    userLocalStore.setUser(username, password);
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_LONG).show();
                        launchMainActivity();
                    });
                }
                else {
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), status[1], Toast.LENGTH_LONG).show();
                        login.setEnabled(true);
                        login.setAlpha(1f);
                    });
                }
            }
        });
    }
}