package com.example.newsai;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_Service";
    private static final String CHANNEL_ID = "news_clusters_channel";
    private static final String CHANNEL_NAME = "Cụm tin mới";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Log.d(TAG, "Message received from: " + message.getFrom());

        String title = null;
        String summary = null;
        String clusterId = null;
        String articleCount = null;

        // 1. Lấy từ notification payload (khi app ở foreground)
        if (message.getNotification() != null) {
            title = message.getNotification().getTitle();
            summary = message.getNotification().getBody();
            Log.d(TAG, "Notification payload - Title: " + title);
        }

        // 2. Lấy từ data payload (luôn có)
        if (message.getData().size() > 0) {
            Log.d(TAG, "Data payload: " + message.getData().toString());

            clusterId = message.getData().get("cluster_id");
            articleCount = message.getData().get("article_count");

            // Fallback: nếu notification payload không có, dùng data
            if (title == null) {
                title = message.getData().get("title");
            }
            if (summary == null) {
                summary = message.getData().get("summary");
            }
        }

        // 3. Hiển thị notification nếu có dữ liệu
        if (clusterId != null || title != null) {
            showNotification(title, summary, clusterId, articleCount);
        } else {
            Log.w(TAG, "No valid data to show notification");
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        // TODO: Send token to server if needed for individual user notifications
    }

    private void showNotification(String title, String message, String clusterId, String articleCount) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Intent clusterIntent = new Intent(this, ClusterDetailActivity.class);
        clusterIntent.putExtra("cluster_id", clusterId);
        clusterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        android.app.TaskStackBuilder stackBuilder = android.app.TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(mainIntent);
        stackBuilder.addNextIntent(clusterIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                (int) System.currentTimeMillis(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // update và bảo mật
        );

        // âm thanh báo
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Nội dung thông báo
        String contentText = message != null ? message : "Nhấn để xem chi tiết";
        if (articleCount != null) {
            contentText = articleCount + " bài viết • " + contentText;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title != null ? title : "Cụm tin mới")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(contentText))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(defaultSoundUri)
                .setVibrate(new long[] { 0, 500, 250, 500 })
                .setContentIntent(pendingIntent);

        notificationManager.notify(
                (int) System.currentTimeMillis(),
                notificationBuilder.build());

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // âm thông báo
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Nhận thông báo khi có cụm tin mới");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[] { 0, 500, 250, 500 });
            channel.enableLights(true);
            channel.setSound(defaultSoundUri, null);
            channel.setShowBadge(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
