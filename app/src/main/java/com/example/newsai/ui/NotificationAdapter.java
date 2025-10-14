package com.example.newsai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newsai.NotificationsActivity.NotificationItem;
import com.example.newsai.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationItem> items = new ArrayList<>();
    private final Consumer<String> onItemClick;

    public NotificationAdapter(Consumer<String> onItemClick) {
        this.onItemClick = onItemClick;
    }

    public void submit(List<NotificationItem> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvMessage.setText(item.message);
        
        // Format timestamp
        String timeStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(item.timestamp));
        holder.tvTime.setText(timeStr);

        holder.itemView.setOnClickListener(v -> {
            if (onItemClick != null && item.clusterId != null) {
                onItemClick.accept(item.clusterId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;

        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvNotificationTitle);
            tvMessage = v.findViewById(R.id.tvNotificationMessage);
            tvTime = v.findViewById(R.id.tvNotificationTime);
        }
    }
}
