package com.example.myapplication;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
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
    TextView file_count_text;
    LinearLayout fileListContainer;
    CardView file_list_card;
    String unique_text;
    Iterator it;
    int unique_text_length = 6;
    boolean isUserNameSent = false;
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
        fileListContainer = findViewById(R.id.file_list_container);

        select_files_button.setOnClickListener(view -> selectFiles());

        send_button.setOnClickListener(view -> {
            if(selectedFileUris.isEmpty()){
                Toast.makeText(getApplicationContext(), "Please select files", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast toast = Toast.makeText(getApplicationContext(), "Sending files", Toast.LENGTH_SHORT);
                toast.show();

                unique_text = SmallFunctions.generateUniqueText(unique_text_length);
                it = selectedFileUris.iterator();
                Uri uri = (Uri) it.next();
                sendFiles(uri, unique_text);

            }
        });
    }

    private void sendFiles(Uri uri, String unique_text) {

        if(!isUserNameSent){
            RequestBody unique_text_body = RequestBody.create(MediaType.parse("text/plain"), unique_text);
            UserLocalStore userLocalStore = new UserLocalStore(getApplicationContext());
            String username = userLocalStore.getUsername();
            if(username == null){
                Toast.makeText(getApplicationContext(), "Username not found", Toast.LENGTH_SHORT).show();
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


            Retrofit retrofit = NetworkClient.getRetrofit(getApplicationContext());
            UploadApis uploadApis = retrofit.create(UploadApis.class);


            Call<UploadResponse> call = uploadApis.uploadFile(parts, unique_text_body);
            call.enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                    file.delete();

                    if(it.hasNext()){
                        Uri uri = (Uri) it.next();
                        sendFiles(uri, unique_text);
                    }
                }

                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    file.delete();

                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    });

                    if(it.hasNext()){
                        Uri uri = (Uri) it.next();
                        sendFiles(uri, unique_text);
                    }
                }
            });
        }
        catch (Exception e){
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            SmallFunctions dialogueBox = new SmallFunctions("catch: " + e.getMessage());
            dialogueBox.show(getSupportFragmentManager(), "dialog");
        }

        if(!it.hasNext()){
            Toast.makeText(this, "All Files sent", Toast.LENGTH_SHORT).show();
            launchQrActivity();
        }
    }

    private void launchQrActivity() {
        Intent intent = new Intent(sendActivity.this, qrActivity.class);
        intent.putExtra("unique_text", unique_text);
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
                            handler.post(() -> {
                                Toast.makeText(this, "Error loading thumbnail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    final Bitmap finalThumbnailBitmap = thumbnailBitmap;

                    handler.post(() ->
                            showFileInUi(fileName, fileSize, finalThumbnailBitmap)
                    );
                }
                cursor.close();
            }
        });

        thread.start();
    }

    private void showFileInUi(String fileName, long fileSize, Bitmap thumbnailBitmap) {
        DecimalFormat df = new DecimalFormat("0.00");

        ImageView imageView = new ImageView(this);
        imageView.setPadding(8, 8, 8, 8);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(200, 200);
        imageView.setLayoutParams(imageParams);

        if (thumbnailBitmap != null) {
            imageView.setImageBitmap(thumbnailBitmap);
        } else {

            if (fileName.endsWith(".pdf")) {
                imageView.setImageResource(R.drawable.pdf);
            } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx") || fileName.endsWith(".txt")) {
                imageView.setImageResource(R.drawable.docs);
            } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".flac")) {
                imageView.setImageResource(R.drawable.audio);
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                 imageView.setImageResource(R.drawable.ic_launcher_foreground);
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_help);
            }
        }

        TextView fileInfoText = new TextView(this);
        String formattedSize;

        if (fileSize < 1024) formattedSize = df.format(fileSize) + " B";
        else if (fileSize < 1024 * 1024) formattedSize = df.format(fileSize / 1024.0) + " KB";
        else formattedSize = df.format(fileSize / (1024.0 * 1024.0)) + " MB";

        fileInfoText.setText(fileName + "\n" + formattedSize);
        fileInfoText.setTextColor(Color.BLACK);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        fileInfoText.setLayoutParams(textParams);
        fileInfoText.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(16, 16, 16, 16);

        itemLayout.addView(imageView);
        itemLayout.addView(fileInfoText);

        fileListContainer.addView(itemLayout);
    }
}