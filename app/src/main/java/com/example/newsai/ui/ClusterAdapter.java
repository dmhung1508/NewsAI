package com.example.newsai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.newsai.R;
import com.example.newsai.data.ClusterItem;
import java.util.ArrayList;
import java.util.List;

public class ClusterAdapter extends RecyclerView.Adapter<ClusterAdapter.VH> {

    public interface OnClick {
        void click(ClusterItem item);
    }

    private final List<ClusterItem> items = new ArrayList<>();
    private final OnClick onClick;

    public ClusterAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    public void submit(List<ClusterItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_news, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int p) {
        ClusterItem cluster = items.get(p);

        h.title.setText(cluster.getTitle() != null ? cluster.getTitle() : "");

        String summary = cluster.getSummary();
        if (summary != null && summary.length() > 100) {
            summary = summary.substring(0, 100) + "...";
        }

        String img = null;
        if (cluster.getImage_contents() != null && !cluster.getImage_contents().isEmpty()) {
            img = cluster.getImage_contents().get(0);
        }
        
        // Clear previous image first to prevent flash
        h.img.setImageDrawable(null);
        
        if (img != null && !img.isEmpty()) {
            Glide.with(h.img)
                    .load(img)
                    .placeholder(android.R.color.white)
                    .error(android.R.color.white)
                    .centerCrop()
                    .into(h.img);
        } else {
            // Set white background for clusters without images
            h.img.setBackgroundColor(
                h.img.getContext().getResources().getColor(android.R.color.white)
            );
        }

        String src = cluster.getPrimary_source();
        h.chipSource.setText(src != null ? src : "");

        String date = formatDate(cluster.getCreated_at());
        h.tvDate.setText(date + " • " + cluster.getArticle_count() + " bài");

        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.click(cluster);
        });
    }

    private String formatDate(String d) {
        if (d == null || d.length() < 10) return "";
        return d.substring(0, 10);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    protected static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView title;
        TextView chipSource;
        TextView tvDate;

        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgNews);
            title = v.findViewById(R.id.tvNewsTitle);
            chipSource = v.findViewById(R.id.chipSource);
            tvDate = v.findViewById(R.id.tvDate);
        }
    }
}
