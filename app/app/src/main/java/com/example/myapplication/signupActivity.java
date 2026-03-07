package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
import java.util.ArrayList;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;

public class signupActivity extends AppCompatActivity {
    Button signup;
    TextView login, text_isStrongPassword;
    EditText input_username, input_fullname, input_email, input_password, input_password2;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        input_username = findViewById(R.id.input_username);
        input_fullname = findViewById(R.id.input_fullname);
        input_email = findViewById(R.id.input_email);
        input_password = findViewById(R.id.input_password);
        input_password2 = findViewById(R.id.input_password2);

        signup = findViewById(R.id.button_signup);
        login = findViewById(R.id.text_login);
        text_isStrongPassword = findViewById(R.id.text_isStrongPassword);

        url =  getResources().getString(R.string.server_url) + "/signup/?username=";


        input_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();
                ArrayList<String> errors = isStrongPassword(password);
                text_isStrongPassword.setText("");

                if (password.isEmpty()) {
                    text_isStrongPassword.setVisibility(View.GONE);
                } else if (!errors.isEmpty()) {
                    text_isStrongPassword.setVisibility(View.VISIBLE);
                    text_isStrongPassword.setTextColor(Color.RED);
                    for (String error : errors) {
                        text_isStrongPassword.append("• " + error + "\n");
                    }
                } else {
                    text_isStrongPassword.setVisibility(View.VISIBLE);
                    text_isStrongPassword.setText("Password is strong");
                    text_isStrongPassword.setTextColor(Color.GREEN);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

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
    private ArrayList<String> isStrongPassword(String password) {
        String containsNumber = ".*[0-9].*";
        String containsSpecialChar = ".*[@#$%^&+=!].*";
        String containsUpperCase = ".*[A-Z].*";
        String containsLowerCase = ".*[a-z].*";
        String containsEightChar = ".{8,}";
        String containsSpace = "^[^\\s]+$";

        ArrayList<String> result = new ArrayList<>();

        Pattern numberPattern = Pattern.compile(containsNumber);
        Pattern specialCharPattern = Pattern.compile(containsSpecialChar);
        Pattern upperCasePattern = Pattern.compile(containsUpperCase);
        Pattern lowerCasePattern = Pattern.compile(containsLowerCase);
        Pattern eightCharPattern = Pattern.compile(containsEightChar);
        Pattern spacePattern = Pattern.compile(containsSpace);

        if (!numberPattern.matcher(password).matches()) {
            result.add("Password must contain at least one number");
        }
        if (!specialCharPattern.matcher(password).matches()) {
            result.add("Password must contain at least one special character");
        }
        if (!upperCasePattern.matcher(password).matches()) {
            result.add("Password must contain at least one uppercase letter");
        }
        if (!lowerCasePattern.matcher(password).matches()) {
            result.add("Password must contain at least one lowercase letter");
        }
        if (!eightCharPattern.matcher(password).matches()) {
            result.add("Password must contain at least 8 characters");
        }
        if (!spacePattern.matcher(password).matches()) {
            result.add("Password cannot contain spaces");
        }
        return result;
    }
    public void startSignupProcess() {
        String username = input_username.getText().toString();
        String fullname = input_fullname.getText().toString();
        String email = input_email.getText().toString();
        String password = input_password.getText().toString();
        String password2 = input_password2.getText().toString();

        if (username.isEmpty() || password.isEmpty() || password2.isEmpty()) {
            Toast.makeText(this, "please fill all feilds", Toast.LENGTH_SHORT).show();
            signup.setAlpha(1f);
            signup.setEnabled(true);
            return;
        }
        ArrayList<String> result = isStrongPassword(password);
        if (!result.isEmpty()) {
            text_isStrongPassword.setText("");
            for (String s : result) {
                text_isStrongPassword.append("• " + s + "\n");
            }
            text_isStrongPassword.setVisibility(TextView.VISIBLE);
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

        RequestBody fullnameBody = RequestBody.create(MediaType.parse("text/plain"), fullname);
        RequestBody emailBody = RequestBody.create(MediaType.parse("text/plain"), email);
        RequestBody usernameBody = RequestBody.create(MediaType.parse("text/plain"), username);
        RequestBody passwordBody = RequestBody.create(MediaType.parse("text/plain"), password);

        Retrofit retrofit = NetworkClient.getRetrofit(getApplicationContext());
        UploadApis uploadApis = retrofit.create(UploadApis.class);

        retrofit2.Call<UploadResponse> call = uploadApis.signup(fullnameBody, usernameBody, emailBody, passwordBody);
        call.enqueue(new retrofit2.Callback<UploadResponse>() {
            @Override
            public void onResponse(retrofit2.Call<UploadResponse> call, retrofit2.Response<UploadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UploadResponse ServerResponse = response.body();
                    if (ServerResponse.getStatus().equals("success")) {
                        UserLocalStore userLocalStore = new UserLocalStore(getApplicationContext());
                        userLocalStore.setUser(username, password);
                        runOnUiThread(() -> {
                            Toast.makeText(getApplicationContext(), "Account Created Successfully", Toast.LENGTH_LONG).show();
                            launchMainActivity();
                        });

                    } else {
                        String message = ServerResponse.getMessage();
                        runOnUiThread(()->{
                            signup.setAlpha(1f);
                            signup.setEnabled(true);
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        });
                    }
                }
                else {
                    runOnUiThread(() -> {
                        signup.setAlpha(1f);
                        signup.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "No Server Response", Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onFailure (retrofit2.Call < UploadResponse > call, Throwable t){
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Failed to Signup " + t.getMessage(), Toast.LENGTH_LONG).show();
                    signup.setAlpha(1f);
                    signup.setEnabled(true);
                });
            }
        });
    }

    public void launchMainActivity() {
        Intent mainPage = new Intent(signupActivity.this, MainActivity.class);
        startActivity(mainPage);
    }

}