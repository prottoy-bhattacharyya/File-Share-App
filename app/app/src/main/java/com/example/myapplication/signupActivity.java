package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class signupActivity extends AppCompatActivity {
    Button signup;
    TextView login;
    EditText input_username, input_password, input_password2;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        input_username = findViewById(R.id.input_username);
        input_password = findViewById(R.id.input_password);
        input_password2 = findViewById(R.id.input_password2);

        signup = findViewById(R.id.button_signup);
        login = findViewById(R.id.text_login);

        url =  getResources().getString(R.string.server_url) + "/signup/?username=";

        login.setOnClickListener(v -> {
            Intent loginPage = new Intent(signupActivity.this, loginActivity.class);
            startActivity(loginPage);
        });

        signup.setOnClickListener(v -> {
            signup.setAlpha(0.5f);
            signup.setEnabled(false);
            startSignupProcess();
        });

    }

    public void startSignupProcess() {
        String username = input_username.getText().toString();
        String password = input_password.getText().toString();
        String password2 = input_password2.getText().toString();

        if (username.isEmpty() || password.isEmpty() || password2.isEmpty()) {
            Toast.makeText(this, "please fill all feilds", Toast.LENGTH_SHORT).show();
            signup.setAlpha(1f);
            signup.setEnabled(true);
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            signup.setAlpha(1f);
            signup.setEnabled(true);
            return;
        }
        if (!password.equals(password2)) {
            Toast.makeText(this, "passwords do not match", Toast.LENGTH_SHORT).show();
            signup.setAlpha(1f);
            signup.setEnabled(true);
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
                    Toast.makeText(getApplicationContext(), "Failed to Signup " + e.getMessage(), Toast.LENGTH_LONG).show();
                    signup.setAlpha(1f);
                    signup.setEnabled(true);
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
                        signup.setAlpha(1f);
                        signup.setEnabled(true);
                    });
                }
                if (status[0].equals("success")) {
                    UserLocalStore userLocalStore = new UserLocalStore(getApplicationContext());
                    userLocalStore.setUser(username, password);
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Account Created Successfully", Toast.LENGTH_LONG).show();
                        launchMainActivity();
                    });
                }
                else {
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), status[1], Toast.LENGTH_LONG).show();
                        signup.setAlpha(1f);
                        signup.setEnabled(true);
                    });
                }
            }
        });

    }

    public void launchMainActivity() {
        Intent mainPage = new Intent(signupActivity.this, MainActivity.class);
        startActivity(mainPage);
    }

}