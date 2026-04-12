package com.example.myapplication.Activities;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.DragAndDropPermissions;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.myapplication.Apis.NetworkClient;
import com.example.myapplication.R;
import com.example.myapplication.SmallFunctions;
import com.example.myapplication.Apis.UploadApis;
import com.example.myapplication.Responses.UploadResponse;
import com.example.myapplication.UriWorks;
import com.example.myapplication.UserLocalStore;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class sendActivity extends AppCompatActivity {
    Button select_files_button, send_button;
    TextView file_count_text, progress_percent;
    LinearLayout fileListContainer;
    CardView file_list_card;
    String unique_text;
    Iterator it;
    int unique_text_length = 6;
    boolean isUserNameSent = false;

    int total_selected_files = 0;
    int successful_upload_files;
    LinearProgressIndicator upload_progress;
    UserLocalStore userLocalStore;
    private final Set<Uri> selectedFileUris = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_send);

        select_files_button = findViewById(R.id.select_files_button);
        send_button = findViewById(R.id.send_button);
        file_list_card = findViewById(R.id.file_list_card);
        file_count_text = findViewById(R.id.file_count_text);
        progress_percent = findViewById(R.id.progress_percent);
        fileListContainer = findViewById(R.id.file_list_container);
        upload_progress = findViewById(R.id.upload_progress);

        userLocalStore = new UserLocalStore(this);

        select_files_button.setOnClickListener(view -> selectFiles());

        send_button.setOnClickListener(view -> {
            if (!userLocalStore.isLoggedIn()) {
                Toast.makeText(getApplicationContext(), "Please Login First", Toast.LENGTH_SHORT).show();
                return;
            }

            if(selectedFileUris.isEmpty()){
                Toast.makeText(getApplicationContext(), "Please select files", Toast.LENGTH_SHORT).show();
            }
            else{
                total_selected_files = selectedFileUris.size();
                successful_upload_files = 0;

                Toast toast = Toast.makeText(getApplicationContext(), "Sending files", Toast.LENGTH_SHORT);
                toast.show();

                send_button.setEnabled(false);
                send_button.setAlpha(0.5f);
                progress_percent.setVisibility(View.VISIBLE);

                unique_text = SmallFunctions.generateUniqueText(unique_text_length);
                it = selectedFileUris.iterator();
                Uri uri = (Uri) it.next();
                sendFiles(uri, unique_text);

            }
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (total_selected_files == successful_upload_files){
                    finish();
                }

                else if (total_selected_files > 0) {
                    androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(sendActivity.this)
                            .setTitle("Cancel Upload ?")
                            .setMessage("If you go back, the files upload will be canceled.")
                            .setPositiveButton("Cancel Upload", (d, w) -> {
                                setEnabled(false);
                                finish();
                            })
                            .setNegativeButton("Stay", null)
                            .create();

                    dialog.show();

                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                            .setTextColor(android.graphics.Color.RED);
                }
                else {
                    finish();
                }
            }
        });

        file_list_card.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // Return true if it's a file/URI list, otherwise Android won't send the DROP event
                    return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST)
                            || event.getClipDescription().hasMimeType("text/uri-list");

                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundColor(Color.parseColor("#E0E0E0")); // Visual hint
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackgroundColor(Color.TRANSPARENT);
                    return true;

                case DragEvent.ACTION_DROP:
                    v.setBackgroundColor(Color.TRANSPARENT);

                    // 1. Request permissions (Required for Android 7.0+)
                    DragAndDropPermissions permissions = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        permissions = requestDragAndDropPermissions(event);
                    }

                    // 2. Extract the data
                    ClipData clipData = event.getClipData();
                    if (clipData != null) {
                        boolean itemsAdded = false;
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            Uri uri = clipData.getItemAt(i).getUri();
                            if (uri != null) {
                                // Add to your set and update UI
                                if (selectedFileUris.add(uri)) {
                                    itemsAdded = true;
                                    loadAndShowFile(uri);
                                }
                            }
                        }

                        if (itemsAdded) {
                            file_list_card.setVisibility(View.VISIBLE);
                            updateFileCount();
                        }

                        return true;
                    }

                    return false;

                default:
                    return false;
            }
        });

        handleIncomingIntent();
    }

    private void sendFiles(Uri uri, String unique_text) {
        upload_progress.setVisibility(View.VISIBLE);
        upload_progress.setMax(total_selected_files);

        if(!isUserNameSent){
            RequestBody unique_text_body = RequestBody.create(MediaType.parse("text/plain"), unique_text);
            UserLocalStore userLocalStore = new UserLocalStore(getApplicationContext());
            String username = userLocalStore.getUsername();
            if(username == null){
                Toast.makeText(getApplicationContext(), "Please Login First", Toast.LENGTH_SHORT).show();
                return;
            }
            RequestBody username_body = RequestBody.create(MediaType.parse("text/plain"), username);
            Retrofit retrofit = NetworkClient.getRetrofit(getApplicationContext());
            UploadApis uploadApis = retrofit.create(UploadApis.class);

            Call<UploadResponse> call = uploadApis.save_sender(unique_text_body, username_body);
            call.enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                    isUserNameSent = true;
                }

                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    isUserNameSent = false;
                    Toast.makeText(getApplicationContext(), "Username not sent", Toast.LENGTH_SHORT).show();
                }
            });
        }

        ContentResolver contentResolver = getContentResolver();
        UriWorks uriWorks = new UriWorks();
        String file_name = uriWorks.getFileNameFromUri(uri, contentResolver);
        File temp_file = uriWorks.create_temp_file(this, uri, file_name);

        if(temp_file == null){
            Toast.makeText(getApplicationContext(), "Error processing file", Toast.LENGTH_SHORT).show();
            return;
        }
        String path = temp_file.getAbsolutePath();
        File file = new File(path);


        try {
            RequestBody requestBody = RequestBody.create(MediaType.parse("*/*"), file);
            MultipartBody.Part parts = MultipartBody.Part.createFormData("file", file_name, requestBody);

            RequestBody unique_text_body = RequestBody.create(MediaType.parse("text/plain"), unique_text);
            RequestBody file_name_body = RequestBody.create(MediaType.parse("text/plain"), file_name);



            Retrofit retrofit = NetworkClient.getRetrofit(getApplicationContext());
            UploadApis uploadApis = retrofit.create(UploadApis.class);


            Call<UploadResponse> call = uploadApis.uploadFile(parts, unique_text_body, file_name_body);
            call.enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {

                    successful_upload_files++;
                    file.delete();

                    runOnUiThread(() -> {
                        upload_progress.setProgress(successful_upload_files);
                        progress_percent.setText(successful_upload_files * 100 / total_selected_files + "%");
                        file_count_text.setText(successful_upload_files + " files uploaded");
                    });


                    if(it.hasNext()){
                        Uri uri = (Uri) it.next();
                        sendFiles(uri, unique_text);
                    }
                    else {
                        launchQrActivity();
                    }
                }

                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    file.delete();

                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "upload failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    });

                    if(it.hasNext()){
                        Uri uri = (Uri) it.next();
                        sendFiles(uri, unique_text);
                    }
                    else {
                        launchQrActivity();
                    }
                }
            });
        }
        catch (Exception e){
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            SmallFunctions dialogueBox = new SmallFunctions("catch: " + e.getMessage());
            dialogueBox.show(getSupportFragmentManager(), "dialog");
        }
    }

    private void launchQrActivity() {
        Intent intent = new Intent(sendActivity.this, qrActivity.class);
        intent.putExtra("unique_text", unique_text);
        intent.putExtra("total_files_count", total_selected_files);
        intent.putExtra("successful_send_count", successful_upload_files);
        startActivity(intent);
    }

    private void selectFiles(){
        String s = Intent.ACTION_GET_CONTENT;
        Intent intent = new Intent(s);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select files"), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {

            if (data.getClipData() == null) {
                Uri uri = data.getData();
                if (uri != null && selectedFileUris.add(uri)) {
                    file_list_card.setVisibility(View.VISIBLE);
                    updateFileCount();
                    loadAndShowFile(uri);
                }
            }

            else {
                int count = data.getClipData().getItemCount();
                boolean fileAdded = false;
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    if (uri != null && selectedFileUris.add(uri)) {
                        fileAdded = true;
                        loadAndShowFile(uri);
                    }
                }
                if (fileAdded) {
                    file_list_card.setVisibility(View.VISIBLE);
                    updateFileCount();
                }
            }
        }
    }

    private void updateFileCount() {
        int count = selectedFileUris.size();
        if (count == 1) {
            file_count_text.setText("1 File selected");
        } else {
            file_count_text.setText(count + " Files selected");
        }
    }

    private void loadAndShowFile(Uri uri) {
        Handler handler = new Handler();
        Thread thread = new Thread(() -> {
            ContentResolver contentResolver = getContentResolver();

            Cursor cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {

                int nameColumnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeColumnIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

                if (nameColumnIndex >= 0 && sizeColumnIndex >= 0) {
                    String fileName = cursor.getString(nameColumnIndex);
                    long fileSize = cursor.getLong(sizeColumnIndex);
                    Bitmap thumbnailBitmap = null;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            Size thumbnailSize = new Size(150, 150);
                            thumbnailBitmap = getContentResolver().loadThumbnail(uri, thumbnailSize, null);
                        } catch (IOException e) {
                            Log.d("send activity", "loadAndShowFile: " + e.getMessage());
                        }
                    }

                    final Bitmap finalThumbnailBitmap = thumbnailBitmap;

                    handler.post(() ->
                            showFileInUi(uri, fileName, fileSize, finalThumbnailBitmap)
                    );
                }
                cursor.close();
            }
        });

        thread.start();
    }

    private void showFileInUi(Uri fileUri, String fileName, long fileSize, Bitmap thumbnailBitmap) {
        DecimalFormat df = new DecimalFormat("0.00");

        // Container for the row
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(16, 16, 16, 16);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);

        // 1. Thumbnail
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(140, 140); // Slightly smaller for better fit
        imageView.setLayoutParams(imageParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (thumbnailBitmap != null) {
            imageView.setImageBitmap(thumbnailBitmap);
        } else {
            // Simple extension check
            int resId = R.drawable.ic_docs; // Default
            if (fileName.endsWith(".pdf")) resId = R.drawable.ic_pdf;
            else if (fileName.contains(".mp3") || fileName.contains(".wav")) resId = R.drawable.ic_audio;
            imageView.setImageResource(resId);
        }

        // 2. Text Info Container
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textContainerParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textContainer.setLayoutParams(textContainerParams);
        textContainer.setPadding(24, 0, 8, 0);

        TextView nameText = new TextView(this);
        nameText.setText(fileName);
        nameText.setTextSize(15);
        nameText.setTypeface(null, Typeface.BOLD);
        nameText.setEllipsize(TextUtils.TruncateAt.END);
        nameText.setSingleLine(true);

        TextView sizeText = new TextView(this);
        String formattedSize = (fileSize < 1024 * 1024) ? df.format(fileSize / 1024.0) + " KB" : df.format(fileSize / (1024.0 * 1024.0)) + " MB";
        sizeText.setText(formattedSize);
        sizeText.setTextSize(13);
        sizeText.setAlpha(0.6f);

        textContainer.addView(nameText);
        textContainer.addView(sizeText);

        // 3. Modern Delete Button
        ImageButton deleteBtn = new ImageButton(this);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(100, 100);
        deleteBtn.setLayoutParams(btnParams);
        deleteBtn.setImageResource(R.drawable.ic_close); // Use a 'close' or 'delete' icon
        deleteBtn.setBackgroundResource(android.R.color.transparent);
        deleteBtn.setColorFilter(Color.RED); // Subtle red tint

        // THE LOGIC: Remove from UI and from your data list
        deleteBtn.setOnClickListener(v -> {
            // Remove the view from the UI
            fileListContainer.removeView(itemLayout);

            // Remove from your upload list (assuming you have a list named selectedFileUris)
            if (selectedFileUris != null) {
                selectedFileUris.remove(fileUri);
            }

            // Update the file count text
            updateFileCount();
        });

        // Assemble
        itemLayout.addView(imageView);
        itemLayout.addView(textContainer);
        itemLayout.addView(deleteBtn);

        fileListContainer.addView(itemLayout);
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Uri uri;
            // API 33+ safe way to get the file
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
            } else {
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            }

            if (uri != null && selectedFileUris.add(uri)) {
                loadAndShowFile(uri);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            ArrayList<Uri> uris;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri.class);
            } else {
                uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            }

            if (uris != null) {
                for (Uri u : uris) {
                    if (u != null && selectedFileUris.add(u)) {
                        loadAndShowFile(u);
                    }
                }
            }
        }

        if (!selectedFileUris.isEmpty()) {
            file_list_card.setVisibility(View.VISIBLE);
            updateFileCount();
            // Crucial: Clear the action so the files don't reload on screen rotation
            getIntent().setAction(null);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // IMPORTANT: Updates the activity's intent
        handleIncomingIntent();
    }
}