package com.citomoltd.banglagoststory.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.citomoltd.banglagoststory.MainActivity;
import com.citomoltd.banglagoststory.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "story_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getTitle()
                : "New Story";
        String body = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getBody()
                : "Tap to read";

        Log.d("FCM", "Message Received: " + title + " - " + body);
        showNotification(title, body);
    }

    private void showNotification(String title, String body) {

        // ✅ Create notification channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Stories Notification",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("New story updates");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        // ✅ Intent to open MainActivity on tap
      /*  Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );*/

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("open_fragment", "recent");

// ✅ নতুন task শুরু করবে, পুরনো সব ক্লিয়ার করবে
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                1001, // ✅ unique requestCode রাখো
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );





        // ✅ Custom layout for notification
        RemoteViews customLayout = new RemoteViews(getPackageName(), R.layout.custom_notification_premium);
        customLayout.setTextViewText(R.id.tvNotificationTitle, title);
        customLayout.setTextViewText(R.id.tvNotificationBody, body);
        customLayout.setImageViewResource(R.id.ivNotificationIcon, R.mipmap.app_icon);



        // ✅ Build notification with custom design
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_auto_stories_24)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(customLayout)
                .setCustomBigContentView(customLayout)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // ✅ Show notification if permission granted
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } else {
            Log.w("FCM", "❌ Notification permission not granted!");
        }
    }
}
