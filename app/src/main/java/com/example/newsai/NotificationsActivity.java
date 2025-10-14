package com.example.newsai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newsai.ui.NotificationAdapter;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tvEmpty);
        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NotificationAdapter(this::openClusterDetail);
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        // TODO: Load from local database or SharedPreferences
        // For now, show empty state
        List<NotificationItem> notifications = new ArrayList<>();
        
        if (notifications.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
            adapter.submit(notifications);
        }
    }

    private void openClusterDetail(String clusterId) {
        Intent intent = new Intent(this, ClusterDetailActivity.class);
        intent.putExtra("cluster_id", clusterId);
        startActivity(intent);
    }

    // Simple notification item model
    public static class NotificationItem {
        public String id;
        public String title;
        public String message;
        public String clusterId;
        public long timestamp;

        public NotificationItem(String id, String title, String message, String clusterId, long timestamp) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.clusterId = clusterId;
            this.timestamp = timestamp;
        }
    }
}
