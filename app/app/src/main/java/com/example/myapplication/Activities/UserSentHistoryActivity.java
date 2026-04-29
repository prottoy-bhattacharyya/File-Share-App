package com.example.myapplication.Activities;

import android.os.Bundle;
import android.util.Log;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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

public class UserSentHistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecycler;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View stateContainer;
    private ProgressBar pbLoading;
    private ImageView stateIcon;
    private TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_sent_history);

        historyRecycler    = findViewById(R.id.history_recycler);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        stateContainer     = findViewById(R.id.state_container);
        pbLoading          = findViewById(R.id.loading_spinner);
        stateIcon          = findViewById(R.id.state_icon);
        message            = findViewById(R.id.message);

        historyRecycler.setLayoutManager(new LinearLayoutManager(this));

        fetchUserHistory();
        swipeRefreshLayout.setOnRefreshListener(this::fetchUserHistory);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Network fetch
    // ══════════════════════════════════════════════════════════════════════════

    private void fetchUserHistory() {
        showLoading();

        UserLocalStore store = new UserLocalStore(this);
        String username = store.getUsername();
        RequestBody usernameBody = RequestBody.create(MediaType.parse("text/plain"), username);

        Retrofit retrofit = NetworkClient.getRetrofit(this);
        UploadApis api = retrofit.create(UploadApis.class);

        api.user_history(usernameBody).enqueue(new Callback<UserHistoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserHistoryResponse> call,
                                   @NonNull Response<UserHistoryResponse> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    UserHistoryResponse body = response.body();
                    if ("success".equals(body.getStatus())
                            && body.getData() != null
                            && !body.getData().isEmpty()) {
                        buildAndDisplayCards(body.getData());
                    } else {
                        showEmpty("No transfer history found");
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
    // Grouping: server rows (flat list) → SentGroups (grouped by unique_text)
    //           then cross-reference local DB for file names
    // ══════════════════════════════════════════════════════════════════════════

    private void buildAndDisplayCards(List<userInfo> serverRows) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            // Get the logged-in username so we can filter to only rows where THIS
            // user is the sender. Rows where they appear as a receiver are skipped —
            // those belong to someone else's "YOU SENT" card.
            String currentUsername = new UserLocalStore(this).getUsername();

            // Group rows by unique_text, preserving insertion order (most recent first
            // as the server presumably returns them newest-first)
            Map<String, SentGroup> grouped = new LinkedHashMap<>();

            for (userInfo row : serverRows) {
                String code   = row.getUniqueText();
                String sender = row.getSender();
                String sendingTime = row.getTimestamp();
                List<String> filenames = row.getFileNames();

                Log.d("sent filenames: ", filenames.toString());

                if (code == null || code.isEmpty()) continue;

                // ── Key fix: skip rows where the current user is NOT the sender ──
                if (currentUsername == null || !currentUsername.equals(sender)) continue;

                SentGroup group = grouped.get(code);
                if (group == null) {
                    group = new SentGroup(code, sendingTime, filenames);
                    grouped.put(code, group);
                }

                // Add receiver if non-null and not already in list
                String receiver = row.getReceiver();
                if (receiver != null && !receiver.isEmpty()
                        && !group.receivers.contains(receiver)) {
                    group.receivers.add(receiver);
                }
            }

            // For every group, look up local DB for file names stored under that code
//            for (SentGroup group : grouped.values()) {
//                List<LocalFile> localFiles = db.fileDao().getFilesByCode(group.uniqueText);
//                for (LocalFile lf : localFiles) {
//                    if (lf.fileName != null && !lf.fileName.isEmpty()) {
//                        group.fileNames.add(lf.fileName);
//                    }
//                }
//            }

            final List<SentGroup> groups = new ArrayList<>(grouped.values());

            runOnUiThread(() -> {
                if (groups.isEmpty()) {
                    showEmpty("No transfer history found");
                } else {
                    showContent();
                    historyRecycler.setAdapter(new HistoryAdapter(groups));
                }
            });
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Data model
    // ══════════════════════════════════════════════════════════════════════════

    static class SentGroup {
        final String uniqueText;
        final List<String> receivers = new ArrayList<>();
        final List<String> fileNames;
        final String sendingTime;

        SentGroup(String uniqueText, String sendingTime, List<String> fileNames) {
            this.uniqueText = uniqueText;
            this.sendingTime = sendingTime;
            this.fileNames = fileNames;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RecyclerView Adapter
    // ══════════════════════════════════════════════════════════════════════════

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        private final List<SentGroup> items;

        HistoryAdapter(List<SentGroup> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sent_history, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            SentGroup g = items.get(position);

            // ── Unique code ───────────────────────────────────────────────
            h.tvUniqueText.setText(g.uniqueText);
            h.tvSendingTime.setText(g.sendingTime);

            // ── Receiver count chip ───────────────────────────────────────
            int count = g.receivers.size();
            if (count == 0)       h.tvReceiverCount.setText("Not received yet");
            else if (count == 1)  h.tvReceiverCount.setText("1 receiver");
            else                  h.tvReceiverCount.setText(count + " receivers");

            // ── File names from local DB ──────────────────────────────────
            if (g.fileNames.isEmpty()) {
                h.tvFiles.setText("Files not found locally");

            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < g.fileNames.size(); i++) {
                    if (i > 0) sb.append("\n");
                    sb.append("• ").append(g.fileNames.get(i));
                }
                h.tvFiles.setText(sb.toString());
            }

            // ── Receivers as Material chips ───────────────────────────────
            h.chipGroup.removeAllViews();
            if (g.receivers.isEmpty()) {
                h.tvNoReceivers.setVisibility(View.VISIBLE);
                h.chipGroup.setVisibility(View.GONE);
            } else {
                h.tvNoReceivers.setVisibility(View.GONE);
                h.chipGroup.setVisibility(View.VISIBLE);
                for (String receiver : g.receivers) {
                    Chip chip = new Chip(UserSentHistoryActivity.this);
                    chip.setText(receiver);
                    chip.setCheckable(false);
                    chip.setClickable(false);
                    chip.setFocusable(false);
                    h.chipGroup.addView(chip);
                }
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView  tvUniqueText, tvReceiverCount, tvFiles, tvNoReceivers, tvSendingTime;
            final ChipGroup chipGroup;

            VH(@NonNull View v) {
                super(v);
                tvUniqueText    = v.findViewById(R.id.tvUniqueText);
                tvReceiverCount = v.findViewById(R.id.tvReceiverCount);
                tvSendingTime   = v.findViewById(R.id.tvSendingTime);
                tvFiles         = v.findViewById(R.id.tvFiles);
                tvNoReceivers   = v.findViewById(R.id.tvNoReceivers);
                chipGroup       = v.findViewById(R.id.chipGroupReceivers);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI state helpers
    // ══════════════════════════════════════════════════════════════════════════

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