package com.example.myapplication.Activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.Apis.NetworkClient;
import com.example.myapplication.R;
import com.example.myapplication.Apis.UploadApis;
import com.example.myapplication.Responses.UserHistoryResponse;
import com.example.myapplication.UserLocalStore;
import com.example.myapplication.Apis.userInfo;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UserHistoryActivity extends AppCompatActivity {

    TableLayout file_transfer_table;
    SwipeRefreshLayout swipeRefreshLayout;
    TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_history);
        file_transfer_table = findViewById(R.id.file_transfer_table);
        message = findViewById(R.id.message);

        fetchUserHistory();
        exqListener();
    }

    private void exqListener(){
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchUserHistory();
        });
    }




    private void fetchUserHistory(){
        UserLocalStore userLocalStore = new UserLocalStore(this);
        String username = userLocalStore.getUsername();
        RequestBody usernameBody = RequestBody.create(MediaType.parse("text/plain"), username);

        Retrofit retrofit = NetworkClient.getRetrofit(this);

        UploadApis uploadApis = retrofit.create(UploadApis.class);

        Call<UserHistoryResponse> call = uploadApis.user_history(usernameBody);

        call.enqueue(new Callback<UserHistoryResponse>() {
            @Override
            public void onResponse(Call<UserHistoryResponse> call, Response<UserHistoryResponse> response) {

                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    UserHistoryResponse userHistoryResponse = response.body();
                    if ("success".equals(userHistoryResponse.getStatus()) &&
                            userHistoryResponse.getData() != null) {

                        addUserDataRows(file_transfer_table, userHistoryResponse.getData());
                    }
                    else {
                        message.setText("Request Unsuccessful: " + userHistoryResponse.getMessage());
                    }
                }
                else {
                    UserHistoryResponse errorData = null;
                    try {
                        errorData = new Gson().fromJson(
                                response.errorBody().string(),
                                UserHistoryResponse.class
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    message.setText("Request Unsuccessful: " + errorData.getMessage());
                }
            }

            @Override
            public void onFailure(Call<UserHistoryResponse> call, Throwable t) {
                runOnUiThread(()->{
                    swipeRefreshLayout.setRefreshing(false);
                    message.setText("Failed to fetch data. Check your internet connection.");
                    message.setTextColor(getResources().getColor(R.color.red));
                });
            }
        });
    }
    public void addUserDataRows(TableLayout file_transfer_table, List<userInfo> dataList){
        if (dataList == null || dataList.isEmpty()) {
            message.setVisibility(View.VISIBLE);
            message.setText("No Data Found");
            return;
        }
        message.setVisibility(View.GONE);
        file_transfer_table.setVisibility(View.VISIBLE);
        file_transfer_table.removeAllViews();

        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#E1F5FE"));
        headerRow.setPadding(0, 15, 0, 15);

        headerRow.addView(createTransferTextView("Sender"));
        headerRow.addView(createTransferTextView("Unique Text"));
        headerRow.addView(createTransferTextView("Receiver"));

        file_transfer_table.addView(headerRow);

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