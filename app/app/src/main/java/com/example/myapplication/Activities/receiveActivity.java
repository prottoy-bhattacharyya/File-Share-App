package com.example.myapplication.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Apis.NetworkClient;
import com.example.myapplication.Apis.UploadApis;
import com.example.myapplication.Responses.FileMetadata;
import com.example.myapplication.R;
import com.example.myapplication.Responses.FileListResponse;
import com.example.myapplication.Responses.UploadResponse;
import com.example.myapplication.UserLocalStore;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class receiveActivity extends AppCompatActivity {

    Button scan_button, go_button, browse_files_button;
    EditText type_text;
    String unique_text_value = "";

    MaterialCardView download_progress_container;
    TextView download_status_text, download_percent;
    LinearProgressIndicator downloadProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        // Initialize UI
        scan_button = findViewById(R.id.scan_button);
        go_button = findViewById(R.id.go_button);

        type_text = findViewById(R.id.type_text);
        download_progress_container = findViewById(R.id.download_progress_container);
        download_status_text = findViewById(R.id.download_status_text);
        download_percent = findViewById(R.id.download_percent);
        downloadProgress = findViewById(R.id.download_progress);
        browse_files_button = findViewById(R.id.btn_browse_files);


        scan_button.setOnClickListener(view -> qr_scanner());

        go_button.setOnClickListener(view -> {
            unique_text_value = type_text.getText().toString().trim();
            if (unique_text_value.isEmpty()) {
                Toast.makeText(this, "Please scan QR code or type identifier", Toast.LENGTH_SHORT).show();
                return;
            }
            start_download_process();
        });

        browse_files_button.setOnClickListener(view -> {
            Intent intent = new Intent(receiveActivity.this, ReceivedFilesActivity.class);
            startActivity(intent);
        });
    }

    void qr_scanner() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
                .enableAutoZoom()
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);
        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    unique_text_value = barcode.getRawValue();
                    type_text.setText(unique_text_value);
                    start_download_process();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Scan failed", Toast.LENGTH_SHORT).show());
    }

    void start_download_process() {
        download_progress_container.setVisibility(View.VISIBLE);
        browse_files_button.setAlpha(0.5f);
        browse_files_button.setEnabled(false);

        // Optional: Update server that this user is receiving files
        save_receiver();

        // Step 1: Get the list of files for this identifier
        fetchFileList(unique_text_value);
    }

    private void fetchFileList(String uniqueText) {
        download_status_text.setText("Fetching file list...");

        UploadApis api = NetworkClient.getRetrofit(this).create(UploadApis.class);
        api.getFileList(uniqueText).enqueue(new Callback<FileListResponse>() {
            @Override
            public void onResponse(Call<FileListResponse> call, Response<FileListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getFiles() != null) {
                    List<FileMetadata> files = response.body().getFiles();
                    if (files.isEmpty()) {
                        Toast.makeText(receiveActivity.this, "No files found for this code", Toast.LENGTH_SHORT).show();
                        download_progress_container.setVisibility(View.GONE);
                        return;
                    }

                    // Setup progress bar
                    downloadProgress.setMax(files.size());
                    downloadProgress.setProgress(0);

                    // Step 2: Start sequential download
                    downloadFilesSequentially(files, 0);
                } else {
                    Toast.makeText(receiveActivity.this, "Error: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FileListResponse> call, Throwable t) {
                Toast.makeText(receiveActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadFilesSequentially(List<FileMetadata> list, int index) {
        if (index >= list.size()) {
            download_status_text.setText("All files downloaded!");
            download_percent.setText("100%");
            downloadProgress.setProgress(list.size());

            browse_files_button.setEnabled(true);
            browse_files_button.setAlpha(1f);

            Toast.makeText(this, "Download finished!", Toast.LENGTH_SHORT).show();
            return;
        }

        FileMetadata currentFile = list.get(index);

        // Update UI Progress
        int displayIndex = index + 1;
        download_status_text.setText("Downloading: \n" +  currentFile.getName() + " (" + displayIndex + "/" + list.size() + ")");
        int percent = (int) (((float) index / list.size()) * 100);
        download_percent.setText(percent + "%");
        downloadProgress.setProgress(index);

        UploadApis api = NetworkClient.getRetrofit(this).create(UploadApis.class);
        api.downloadFile(currentFile.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> {
                        saveFileToDisk(response.body(), currentFile.getName());
                        // Trigger next download on Main Thread
                        runOnUiThread(() -> downloadFilesSequentially(list, index + 1));
                    }).start();
                } else {
                    // Log error and try next file
                    runOnUiThread(() -> downloadFilesSequentially(list, index + 1));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                runOnUiThread(() -> downloadFilesSequentially(list, index + 1));
            }
        });
    }

    private void saveFileToDisk(ResponseBody body, String fileName) {
        try {
            // Target: /storage/emulated/0/Download/File Share App
            File downloadsRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File customFolder = new File(downloadsRoot, "File Share App");

            // Create the "File Share App" folder if it doesn't exist
            if (!customFolder.exists()) {
                customFolder.mkdirs();
            }

            File file = new File(customFolder, fileName);

            try (InputStream is = body.byteStream();
                 FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();
            }
            Log.d("SAVE_FILE", "Saved: " + fileName);
        } catch (IOException e) {
            Log.e("SAVE_FILE", "Error: " + e.getMessage());
        }
    }

    public void save_receiver() {
        UserLocalStore userLocalStore = new UserLocalStore(this);
        String username = userLocalStore.getUsername();

        RequestBody unique_text_body = RequestBody.create(MediaType.parse("text/plain"), unique_text_value);
        RequestBody username_body = RequestBody.create(MediaType.parse("text/plain"), username);

        UploadApis uploadApis = NetworkClient.getRetrofit(this).create(UploadApis.class);
        uploadApis.save_receiver(unique_text_body, username_body).enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                // Background update, no toast needed unless it fails critical logic
            }

            @Override
            public void onFailure(Call<UploadResponse> call, Throwable t) {
                Log.e("RECEIVER_SAVE", "Failed: " + t.getMessage());
            }
        });
    }
}