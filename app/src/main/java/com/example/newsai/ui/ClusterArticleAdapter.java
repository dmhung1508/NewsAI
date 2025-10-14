package com.example.newsai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.newsai.R;
import com.example.newsai.data.ClusterArticleItem;
import java.util.ArrayList;
import java.util.List;

public class ClusterArticleAdapter extends RecyclerView.Adapter<ClusterArticleAdapter.VH> {

    public interface OnClick {
        void click(ClusterArticleItem item);
    }

    private final List<ClusterArticleItem> items = new ArrayList<>();
    private final OnClick onClick;

    public ClusterArticleAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    public void submit(List<ClusterArticleItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cluster_article, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int p) {
        ClusterArticleItem article = items.get(p);

        h.tvRank.setText(String.valueOf(article.getRank() + 1));

        String title = article.getTitle();
        if (title == null || title.trim().isEmpty()) {
            String text = article.getText();
            title = text != null && text.length() > 80 
                ? text.substring(0, 80) + "..." 
                : (text != null ? text : "Không có tiêu đề");
        }
        h.tvArticleTitle.setText(title);

        String source = article.getSource();
        h.tvSourceBadge.setText(source != null && source.equalsIgnoreCase("facebook") ? "Facebook" : "Web");

        String text = article.getText();
        if (text != null && text.length() > 120) {
            text = text.substring(0, 120) + "...";
        }
        h.tvArticleText.setText(text != null ? text : "");

        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.click(article);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    protected static class VH extends RecyclerView.ViewHolder {
        TextView tvRank;
        TextView tvArticleTitle;
        TextView tvArticleText;
        TextView tvSourceBadge;

        VH(@NonNull View v) {
            super(v);
//            tvRank = v.findViewById(R.id.tvRank);
            tvArticleTitle = v.findViewById(R.id.tvArticleTitle);
            tvArticleText = v.findViewById(R.id.tvArticleText);
            tvSourceBadge = v.findViewById(R.id.tvSourceBadge);
        }
    }
}
