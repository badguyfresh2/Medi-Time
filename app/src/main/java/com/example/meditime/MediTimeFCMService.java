package com.example.meditime;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MediTimeFCMService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "meditime_notifications";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body  = remoteMessage.getNotification().getBody();
            showNotification(title, body);
            saveNotificationToRTDB(title, body);
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Save token to RTDB for this user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            FirebaseDatabase.getInstance().getReference("users")
                .child(auth.getCurrentUser().getUid())
                .child("fcmToken").setValue(token);
        }
    }

    private void showNotification(String title, String body) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "MediTime", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title != null ? title : "MediTime")
            .setContentText(body != null ? body : "")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void saveNotificationToRTDB(String title, String body) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("body",  body);
        notif.put("read",  false);
        notif.put("timestamp", ServerValue.TIMESTAMP);
        
        FirebaseDatabase.getInstance().getReference("users")
            .child(auth.getCurrentUser().getUid())
            .child("notifications").push().setValue(notif);
    }
}
