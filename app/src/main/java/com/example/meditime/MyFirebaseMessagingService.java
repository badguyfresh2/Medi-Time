package com.example.meditime;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "fcm_default_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Extract data from the message
        String title = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getTitle() : "New Notification";
        String body = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getBody() : "";

        // Show system notification
        showNotification(title, body);

        // Optionally save to Firebase Database (if not already done by sender)
        // This is useful for in-app history
        saveToDatabase(title, body);
    }

    private void showNotification(String title, String body) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Create channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        // Intent to open the notification list
        Intent intent = new Intent(this, NotificationActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logonobg)   // your app icon
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void saveToDatabase(String title, String body) {
        // Save to same path used by NotificationActivity
        // Assumes user is logged in; you can handle when no user is available
        try {
            String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notifications").child(uid);
            Map<String, Object> notif = new HashMap<>();
            notif.put("title", title);
            notif.put("body", body);
            notif.put("timestamp", System.currentTimeMillis());
            notif.put("read", false);
            ref.push().setValue(notif);
        } catch (Exception ignored) {}
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        android.util.Log.d("FCM_TOKEN", "New token: " + token);
        // (Optional) you could also send this token to your server/database
    }
}