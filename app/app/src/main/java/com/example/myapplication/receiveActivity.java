package com.example.myapplication;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;

public class receiveActivity extends AppCompatActivity {

    Button scan_button, go_button;
    private final HashSet<Long> downloadIDs = new HashSet<>();
    TextView title_text;
    String DOWNLOAD_LINK_BASE;
    String FILE_COUNT_LINK_BASE;

    EditText type_text;
    String unique_text_value = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_receive);

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        ContextCompat.registerReceiver(
                this,
                onDownloadComplete,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );

        scan_button = findViewById(R.id.scan_button);
        go_button = findViewById(R.id.go_button);
        title_text = findViewById(R.id.title_text);
        type_text = findViewById(R.id.type_text);

        DOWNLOAD_LINK_BASE = getResources().getString(R.string.server_url)+ "/download?unique_text=";
        FILE_COUNT_LINK_BASE =getResources().getString(R.string.server_url)+ "/get_file_count?unique_text=";

        scan_button.setOnClickListener(view -> qr_scanner());

        go_button.setOnClickListener(view -> {
            String text = type_text.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please scan QR code or type identifier", Toast.LENGTH_SHORT).show();
                return;
            }
            unique_text_value = text;
            start_download_process();
        });
    }


    void qr_scanner(){
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_AZTEC)
                .enableAutoZoom()
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);
        scanner
                .startScan()
                .addOnSuccessListener(
                        barcode -> {
                            unique_text_value = barcode.getRawValue(); // Store value globally
                            type_text.setText(unique_text_value);
                            start_download_process();
                        })
                .addOnCanceledListener(
                        () -> {
                            Toast.makeText(getApplicationContext(), "Scan canceled", Toast.LENGTH_SHORT).show();
                        }
                )

                .addOnFailureListener(
                        e -> {
                            Toast.makeText(getApplicationContext(), "Scan failed", Toast.LENGTH_SHORT).show();
                        }
                );
    }

    void start_download_process(){
        if(unique_text_value.isEmpty()){
            unique_text_value = type_text.getText().toString().trim();

            if (unique_text_value.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please Scan QR code or type identifier", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String file_count_url = FILE_COUNT_LINK_BASE + unique_text_value;
        //http://<address>/get_file_count?unique_text=<unique_text>

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(file_count_url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Failed to get file count: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                int count = 0;
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    count = jsonObject.getInt("file_count");
                }

                catch (JSONException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Failed to parse file count JSON.", Toast.LENGTH_LONG).show();
                    });

                }

                save_receiver();

                final int finalCount = count;
                runOnUiThread(()->{
                    Toast.makeText(getApplicationContext(), "Downloading " + finalCount + " files", Toast.LENGTH_SHORT).show();
                    next_download_process(finalCount);
                });

            }
        });
    }

    public void save_receiver() {
        UserLocalStore userLocalStore = new UserLocalStore(getApplicationContext());
        String username = userLocalStore.getUsername();

        RequestBody unique_text_body = RequestBody.create(MediaType.parse("text/plain"), unique_text_value);
        RequestBody username_body = RequestBody.create(MediaType.parse("text/plain"), username);
        Retrofit retrofit = NetworkClient.getRetrofit(getApplicationContext());
        UploadApis uploadApis = retrofit.create(UploadApis.class);

        retrofit2.Call<UploadResponse> call = uploadApis.save_receiver(unique_text_body, username_body);
        call.enqueue(new retrofit2.Callback<UploadResponse>() {
            @Override
            public void onResponse(retrofit2.Call<UploadResponse> call, retrofit2.Response<UploadResponse> response) {

            }

            @Override
            public void onFailure(retrofit2.Call<UploadResponse> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Failed to save receiver: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

    }

    public void next_download_process(int file_count) {
        if (file_count == 0) {
            Toast.makeText(getApplicationContext(), "No files found", Toast.LENGTH_LONG).show();
            return;
        }
        for (int i = 0; i < file_count; i++) {
            String current_download_url = DOWNLOAD_LINK_BASE + unique_text_value + "&file_index=" + i;
            //http://<address>/download?unique_text=<unique_text>&file_index=<index>

            getFilenameAndEnqueueDownload(current_download_url);
        }
    }

    private void getFilenameAndEnqueueDownload(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .head()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Failed to get file info: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                String contentDisposition = response.header("Content-Disposition");

                String Filename = extractFilenameFromContentDisposition(contentDisposition);

                if (Filename == null || Filename.isEmpty()) {
                    Filename = "downloaded_file_" + System.currentTimeMillis() + ".dat";

                    if (contentDisposition == null) {
                        runOnUiThread(() -> {
                            Toast.makeText(getApplicationContext(), "Warning: Content-Disposition header not found. Using default filename.", Toast.LENGTH_LONG).show();
                        });
                    }
                }

                String finalFilename = Filename;
                runOnUiThread(() -> {
                    enqueueDownload(url, finalFilename);
                }); 
            }
        });
    }


    private String extractFilenameFromContentDisposition(String header) {
        if (header == null) return null;


        String filename = null;
        Pattern pattern = Pattern.compile("filename=\"?([^\"\\n;]+)\"?;?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(header);

        if (matcher.find()) {
            filename = matcher.group(1).replaceAll("^\"|\"$", "");
        }
        return filename;
    }


    private void enqueueDownload(String url, String filename) {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                Toast.makeText(getApplicationContext(), "Error: Download Manager service not available.", Toast.LENGTH_LONG).show();
                return;
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

            request.setTitle(filename);
            request.setDescription("Downloading file...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            downloadIDs.add(downloadManager.enqueue(request));

        } catch (Exception e) {
            android.util.Log.e("DownloadError", "Error in enqueueDownload", e);
            Toast.makeText(getApplicationContext(), "Failed to start download: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadIDs.contains(id)) {
                downloadIDs.remove(id);
                Toast.makeText(getApplicationContext(), "Download Complete", Toast.LENGTH_SHORT).show();
            }
        }
    };

}