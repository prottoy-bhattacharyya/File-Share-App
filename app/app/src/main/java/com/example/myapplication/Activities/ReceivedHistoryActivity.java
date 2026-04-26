package com.example.myapplication.Activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.Apis.NetworkClient;
import com.example.myapplication.Apis.UploadApis;
import com.example.myapplication.Responses.userInfo;
import com.example.myapplication.LocalDb.AppDatabase;
import com.example.myapplication.LocalDb.LocalFile;
import com.example.myapplication.R;
import com.example.myapplication.Responses.UserHistoryResponse;
import com.example.myapplication.UserLocalStore;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ReceivedHistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecycler;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View stateContainer;
    private ProgressBar pbLoading;
    private ImageView stateIcon;
    private TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_received_history);

        historyRecycler    = findViewById(R.id.history_recycler);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        stateContainer     = findViewById(R.id.state_container);
        pbLoading          = findViewById(R.id.loading_spinner);
        stateIcon          = findViewById(R.id.state_icon);
        message            = findViewById(R.id.message);

        historyRecycler.setLayoutManager(new LinearLayoutManager(this));

        fetchReceiveHistory();
        swipeRefreshLayout.setOnRefreshListener(this::fetchReceiveHistory);
    }


    private void fetchReceiveHistory() {
        showLoading();

        String username = new UserLocalStore(this).getUsername();
        RequestBody usernameBody = RequestBody.create(MediaType.parse("text/plain"), username);

        Retrofit retrofit = NetworkClient.getRetrofit(this);
        UploadApis api = retrofit.create(UploadApis.class);

        api.user_history(usernameBody).enqueue(new Callback<UserHistoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserHistoryResponse> call,
                                   @NonNull Response<UserHistoryResponse> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {

                    try {
                        UserHistoryResponse body = response.body();
                        if ("success".equals(body.getStatus())
                                && body.getData() != null
                                && !body.getData().isEmpty()) {
                            buildAndDisplayCards(body.getData(), username);
                        } else {
                            showEmpty("You haven't received any files yet");
                        }
                    } catch (Exception e) {
                        showError(e.getMessage());
                    }

                } else {
                    String msg = "Request failed";
                    try {
                        if (response.errorBody() != null) {
                            UserHistoryResponse err = new Gson().fromJson(
                                    response.errorBody().string(), UserHistoryResponse.class);
                            if (err != null && err.getMessage() != null) msg = err.getMessage();
                        }
                    } catch (IOException ignored) {}
                    showError(msg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserHistoryResponse> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showError("No internet connection");
                });
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Grouping:
    //   - Keep only rows where receiver == currentUsername
    //   - Group by unique_text (one card per code)
    //   - Each card shows: sender name + file names from the sender's local DB
    //   - entries stored under that unique_text
    // ══════════════════════════════════════════════════════════════════════════

    private void buildAndDisplayCards(List<userInfo> serverRows, String currentUsername) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            // unique_text → ReceivedGroup
            Map<String, ReceivedGroup> grouped = new LinkedHashMap<>();

            for (userInfo row : serverRows) {
                String code     = row.getUniqueText();
                String receiver = row.getReceiver();
                String sender   = row.getSender();
                if (code == null || code.isEmpty()) continue;


                if (currentUsername == null || !currentUsername.equals(receiver)) continue;


                if (!grouped.containsKey(code)) {
                    grouped.put(code, new ReceivedGroup(code, sender != null ? sender : "Unknown"));
                }
            }

            // Look up file names from the local DB for each code.
            for (ReceivedGroup group : grouped.values()) {
                List<LocalFile> localFiles = db.fileDao().getFilesByCode(group.uniqueText);
                for (LocalFile lf : localFiles) {
                    if (lf.fileName != null && !lf.fileName.isEmpty()) {
                        group.fileNames.add(lf.fileName);
                    }
                }
            }

            final List<ReceivedGroup> groups = new ArrayList<>(grouped.values());

            runOnUiThread(() -> {
                if (groups.isEmpty()) {
                    showEmpty("You haven't received any files yet");
                } else {
                    showContent();
                    historyRecycler.setAdapter(new ReceiveHistoryAdapter(groups));
                }
            });
        });
    }


    static class ReceivedGroup {
        final String uniqueText;
        final String senderName;
        final List<String> fileNames = new ArrayList<>();

        ReceivedGroup(String uniqueText, String senderName) {
            this.uniqueText = uniqueText;
            this.senderName = senderName;
        }
    }


    private class ReceiveHistoryAdapter extends RecyclerView.Adapter<ReceiveHistoryAdapter.VH> {
        private final List<ReceivedGroup> items;

        ReceiveHistoryAdapter(List<ReceivedGroup> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_receive_history, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ReceivedGroup g = items.get(position);


            h.tvUniqueCode.setText(g.uniqueText);


            h.tvSenderName.setText(g.senderName);

            if (g.fileNames.isEmpty()) {
                h.tvFilesReceived.setText("Files not found locally");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < g.fileNames.size(); i++) {
                    if (i > 0) sb.append("\n");
                    sb.append("• ").append(g.fileNames.get(i));
                }
                h.tvFilesReceived.setText(sb.toString());
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView tvUniqueCode, tvSenderName, tvFilesReceived;

            VH(@NonNull View v) {
                super(v);
                tvUniqueCode    = v.findViewById(R.id.tvUniqueCode);
                tvSenderName    = v.findViewById(R.id.tvSenderName);
                tvFilesReceived = v.findViewById(R.id.tvFilesReceived);
            }
        }
    }


    private void showLoading() {
        stateContainer.setVisibility(View.VISIBLE);
        pbLoading.setVisibility(View.VISIBLE);
        stateIcon.setVisibility(View.GONE);
        message.setVisibility(View.VISIBLE);
        message.setText("Fetching history…");
        message.setTextColor(ContextCompat.getColor(this, android.R.color.tab_indicator_text));
        historyRecycler.setVisibility(View.GONE);
    }

    private void showContent() {
        stateContainer.setVisibility(View.GONE);
        historyRecycler.setVisibility(View.VISIBLE);
    }

    private void showEmpty(String text) {
        stateContainer.setVisibility(View.VISIBLE);
        pbLoading.setVisibility(View.GONE);
        stateIcon.setVisibility(View.VISIBLE);
        message.setVisibility(View.VISIBLE);
        message.setText(text);
        message.setTextColor(ContextCompat.getColor(this, android.R.color.tab_indicator_text));
        historyRecycler.setVisibility(View.GONE);
    }

    private void showError(String text) {
        showEmpty(text);
        message.setTextColor(ContextCompat.getColor(this, R.color.red));
    }
}