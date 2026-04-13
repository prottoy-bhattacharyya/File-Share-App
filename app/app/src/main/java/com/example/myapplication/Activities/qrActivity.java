package com.example.myapplication.Activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class qrActivity extends AppCompatActivity {
    ImageView qrImage;
    TextView qrTextView;
    TextView count_text;
    Button shareButton;
    String qrText;
    Bitmap combinedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr);

        qrImage = findViewById(R.id.qr_image);
        qrTextView = findViewById(R.id.qrText);
        count_text = findViewById(R.id.count_text);
        shareButton = findViewById(R.id.shareButton);

        qrText = getIntent().getStringExtra("unique_text");
        int total_files_count = getIntent().getIntExtra("total_files_count", 0);
        int successful_send_count = getIntent().getIntExtra("successful_send_count", 0);

        if(total_files_count == successful_send_count) {
            count_text.setText("All files upload successfull");
        }
        else if(successful_send_count == 0) {
            count_text.setText("No files uploaded");
            count_text.setTextColor(Color.RED);
        }
        else {
            count_text.setText(total_files_count - successful_send_count + " files upload failed");
            count_text.setTextColor(Color.RED);
        }


        shareButton.setVisibility(View.INVISIBLE);

        shareButton.setOnClickListener(view -> shareCode());


        Runnable runnable = () -> {
            Bitmap qrBitmap = generateQR(qrText);
            combinedBitmap = qrBitmap;
            handler.sendEmptyMessage(0);
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void shareCode() {
        String msgText = "File Share App QR Code:\n " + qrText;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, msgText);
        startActivity(Intent.createChooser(shareIntent, "Share QR Code text"));


    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            qrImage.setImageBitmap(combinedBitmap);
            qrTextView.setText(qrText);
            shareButton.setVisibility(View.VISIBLE);
        }
    };



    private Bitmap generateQR(String qrText) {
        // 1. Get the Primary Color (The "ink" of the QR)
        int foreground = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSecondary, Color.BLACK);

        // 2. Get the Surface Color (The "paper" background)
        int background = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurface, Color.WHITE);

        QRGEncoder qrgEncoder = new QRGEncoder(qrText, null, QRGContents.Type.TEXT, 400);

        // Set colors to match UI theme
        qrgEncoder.setColorBlack(foreground);
        qrgEncoder.setColorWhite(background);

        try {
            return qrgEncoder.getBitmap(0);
        } catch (Exception e) {
            return null;
        }
    }
}