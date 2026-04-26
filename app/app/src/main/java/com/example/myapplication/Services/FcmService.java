package com.example.myapplication.Services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.myapplication.Activities.MainActivity;
import com.example.myapplication.Apis.NetworkClient;
import com.example.myapplication.Apis.UploadApis;
import com.example.myapplication.R;
import com.example.myapplication.Responses.FcmTokenResponse;
import com.example.myapplication.UserLocalStore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FcmService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "transfer_notifications";
    UserLocalStore userLocalStore;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            sendNotification(remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        // Upload this token to your Django server whenever it changes
        userLocalStore = new UserLocalStore(getApplicationContext());

        sendTokenToServer(userLocalStore.getUsername(), token);
    }

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_download)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Required for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "File Transfer Updates",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }

    public void sendTokenToServer(String username, String token) {

        UploadApis api = NetworkClient.getRetrofit(this).create(UploadApis.class);
        api.sendToken(token, username).enqueue(new Callback<FcmTokenResponse>() {
            @Override
            public void onResponse(Call<FcmTokenResponse> call, Response<FcmTokenResponse> response) {
                if (response.isSuccessful() && "success".equals(response.body().getStatus())){
                    Toast.makeText(getApplicationContext(), "token send", Toast.LENGTH_LONG);
                    Log.d("Fcm Token send", token);
                }
                else {
                    Log.d("Fcm Token not send", response.body().getMessage());
                    Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG);
                }
            }

            @Override
            public void onFailure(Call<FcmTokenResponse> call, Throwable t) {
                Log.d("Fcm Token not send", t.getMessage());
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG);
            }
        });

    }
}
