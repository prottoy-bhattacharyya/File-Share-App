package com.example.android_api_project;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.flexbox.FlexboxLayout;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.io.File;
import java.io.IOException;

public class send_files extends AppCompatActivity {
    Button share_btn, scan_btn, send_btn, download_btn;
    TextView test_data, select_multiple_image;
    String rawValue;
    Vibrator vibrator;
    ImageView select_image;
    Bitmap selected_image_bitmap;
    FlexboxLayout picked_images_layout;
    long downloadId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_send_files);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(
                    onDownloadComplete,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    RECEIVER_NOT_EXPORTED
            );
        }

        test_data = findViewById(R.id.test_data);
        share_btn = findViewById(R.id.share_btn);
        scan_btn = findViewById(R.id.scan_btn);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        select_image = findViewById(R.id.select_image);
        select_multiple_image = findViewById(R.id.select_multiple_image);
        send_btn = findViewById(R.id.send_btn);
        download_btn = findViewById(R.id.download_btn);

        download_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                begin_download();
            }
        });

        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qr_scanner();
            }
        });

        share_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(rawValue == null){
                    test_data.setText("Please scan the QR code first");
                    return;
                }
                share_qr_text();
            }
        });

        //Select One Image

        ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri->{
                    if(uri != null){
                        try {
                            selected_image_bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            select_image.setImageBitmap(selected_image_bitmap);
                        } catch (IOException e) {
                            select_image.setImageResource(R.drawable.warning);
                        }
                    }
                    else{
                        select_image.setImageResource(R.drawable.warning);
                    }
                });

        select_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                        .build()
                );
            }
        });

        //Select Multiple Images
        picked_images_layout = new FlexboxLayout(this);
        picked_images_layout = findViewById(R.id.picked_images_layout);

        ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia =
                registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(50), uris ->{
                    for(Uri uri : uris){
                        ImageView image = new ImageView(this);
                        image.setImageURI(uri);
                        image.setLayoutParams(new LinearLayout.LayoutParams(400, 400));
                        image.setPadding(5, 16, 5, 16);
                        
                        picked_images_layout.addView(image);
                    }
                });




        select_multiple_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickMultipleMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                        .build()
                );
            }
        });
    }


    void begin_download(){
        File file = new File(getExternalFilesDir(null), "Test File");

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse("http://10.0.2.2:5000/WhatsApp%20Image%202025-09-17%20at%2012.26.17_00e79c4a.jpg"))
                .setTitle("Test File")
                .setDescription("Downloading Test File")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.getName())
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true);

        DownloadManager downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        downloadId = downloadManager.enqueue(request);
        Toast toast = Toast.makeText(getApplicationContext(), "Download Started", Toast.LENGTH_SHORT);
        toast.show();
    }

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if(downloadId == id){
                Toast toast = Toast.makeText(getApplicationContext(), "Download Completed", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    };

    @Override
    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
    }

    void qr_scanner() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_ALL_FORMATS
                ).enableAutoZoom()
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this);

        scanner
                .startScan()
                .addOnSuccessListener(
                        barcode -> {
                            vibrator.vibrate(100);
                            rawValue = barcode.getRawValue();
                            test_data.setText(rawValue);
                        })
                .addOnCanceledListener(
                        () -> {
                            test_data.setText("canceled");
                            rawValue = null;
                        })
                .addOnFailureListener(
                        e -> {
                            test_data.setText("failed");
                            rawValue = null;
                        });
    }

    void share_qr_text(){
        Intent share_intent = new Intent(Intent.ACTION_SEND);
        share_intent.setType("text/plain");
        share_intent.putExtra(Intent.EXTRA_TEXT, rawValue);
        startActivity(Intent.createChooser(share_intent, "Share QR Code"));
    }
}