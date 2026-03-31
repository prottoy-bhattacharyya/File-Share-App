package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UserHistoryActivity extends AppCompatActivity {

    TableLayout file_transfer_table;
    TextView empty_message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_history);
        file_transfer_table = findViewById(R.id.file_transfer_table);
        empty_message = findViewById(R.id.empty_message);

        UserLocalStore userLocalStore = new UserLocalStore(this);
        String username = userLocalStore.getUsername();

        RequestBody usernameBody = RequestBody.create(MediaType.parse("text/plain"), username);

        Retrofit retrofit = NetworkClient.getRetrofit(this);

        UploadApis uploadApis = retrofit.create(UploadApis.class);

        Call<UserDataResponse> call = uploadApis.user_info(usernameBody);

        call.enqueue(new Callback<UserDataResponse>() {
            @Override
            public void onResponse(Call<UserDataResponse> call, Response<UserDataResponse> response) {
                findViewById(R.id.fetching_history).setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    UserDataResponse userDataResponse = response.body();
                    if ("success".equals(userDataResponse.getStatus()) && userDataResponse.getData() != null) {
                        addUserDataRows(file_transfer_table, userDataResponse.getData());
                    }
                    else {
                        Toast.makeText(UserHistoryActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(UserHistoryActivity.this, "Server Error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserDataResponse> call, Throwable t) {
                findViewById(R.id.fetching_history).setVisibility(View.GONE);
                runOnUiThread(()->{
                    Toast.makeText(UserHistoryActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                });

            }
        });

    }

    public void addUserDataRows(TableLayout file_transfer_table, List<userInfo> dataList){
        if (dataList == null || dataList.isEmpty()) {
            empty_message.setVisibility(View.VISIBLE);
            return;
        }
        file_transfer_table.setVisibility(View.VISIBLE);

        for (int i = 0; i < dataList.size(); i++) {
            userInfo info = dataList.get(i);
            TableRow dataRow = new TableRow(this);

            if (i % 2 == 0) {
                dataRow.setBackgroundColor(Color.WHITE);
            } else {
                dataRow.setBackgroundColor(Color.parseColor("#E1F5FE"));
            }
            dataRow.setPadding(0, 15, 0, 15);

            dataRow.addView(createTransferTextView(info.getSender()));
            dataRow.addView(createTransferTextView(info.getUniqueText()));
            dataRow.addView(createTransferTextView(info.getReceiver()));

            file_transfer_table.addView(dataRow);
        }
    }

    private View createTransferTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(10, 5, 10, 5);

        textView.setTextColor(Color.BLACK);

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                1f
        );
        textView.setLayoutParams(params);

        return textView;
    }
}