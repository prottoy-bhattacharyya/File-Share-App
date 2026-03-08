package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class qrActivity extends AppCompatActivity {
    ImageView qrImage;
    TextView qrTextView;
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
        shareButton = findViewById(R.id.shareButton);
        qrText = getIntent().getStringExtra("unique_text");

        shareButton.setVisibility(View.INVISIBLE);

        shareButton.setOnClickListener(view -> shareCode());


        Runnable runnable = () -> {
            Bitmap qrBitmap = generateQR(qrText);
            Bitmap logoBitmap = generateLogoBitmap();
            combinedBitmap = addLogoToQR(qrBitmap, logoBitmap);
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



    private Bitmap generateQR(String qrText){
        Bitmap bitmap;
        QRGEncoder qrgEncoder = new QRGEncoder(qrText, null, QRGContents.Type.TEXT, 400);
        qrgEncoder.setColorBlack(Color.BLACK);
        qrgEncoder.setColorWhite(Color.WHITE);
        try {
            bitmap = qrgEncoder.getBitmap(0);

        } catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    private Bitmap generateLogoBitmap(){
        Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo_8k_circle);
        return logoBitmap;
    }

    private Bitmap addLogoToQR(Bitmap qrBitmap, Bitmap logoBitmap){
        int qrWidth = qrBitmap.getWidth();
        int qrHeight = qrBitmap.getHeight();

        Bitmap combinedBitmap = Bitmap.createBitmap(qrWidth, qrHeight, qrBitmap.getConfig());
        Canvas canvas = new Canvas(combinedBitmap);
        canvas.drawBitmap(qrBitmap, 0, 0, null);

        int logoSize = (int) (qrWidth * 0.2);
        Bitmap scaledLogo = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true);

        float left = (qrWidth - scaledLogo.getWidth()) / 2f;
        float top = (qrHeight - scaledLogo.getHeight()) / 2f;

        canvas.drawBitmap(scaledLogo, left, top, null);
        return combinedBitmap;
    }
}