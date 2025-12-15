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
import com.example.newsai.data.SavedArticle;

import java.util.ArrayList;
import java.util.List;

public class SavedArticleAdapter extends RecyclerView.Adapter<SavedArticleAdapter.VH> {

    public interface OnClick { void click(SavedArticle article); }

    private final List<SavedArticle> items = new ArrayList<>();
    private final OnClick onClick;

    public SavedArticleAdapter(OnClick onClick) { this.onClick = onClick; }

    public void submit(List<SavedArticle> list) {
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
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SavedArticle article = items.get(position);

        // Title giống NewsAdapter: fallback bằng nội dung
        String title = article.getTitle();
        if (title == null || title.trim().isEmpty()) {
            String content = article.getContent();
            title = content == null ? "" : (content.length() > 120 ? content.substring(0, 120) + "…" : content);
        }
        holder.title.setText(title);

        // Description: ưu tiên description nếu sau này có, hiện tại lấy từ content
        String description = article.getContent();
        if (description != null) {
            description = description.trim();
        }
        if (description != null && description.length() > 180) {
            description = description.substring(0, 180) + "...";
        }
        if (description == null || description.isEmpty()) {
            holder.description.setVisibility(View.GONE);
        } else {
            holder.description.setVisibility(View.VISIBLE);
            holder.description.setText(description);
        }

        Glide.with(holder.img)
                .load(article.getImageUrl())
                .placeholder(R.drawable.hotnews)
                .error(R.drawable.hotnews)
                .centerCrop()
                .into(holder.img);

        String src = domain(article.getSourceUrl() != null ? article.getSourceUrl() : article.getUrl());
        holder.chipSource.setText(src.isEmpty() ? "facebook.com" : src);
        String date = article.getDate();
        if (date == null || date.isEmpty()) date = article.getPostedAt();
        holder.tvDate.setText(date != null && date.length() >= 10 ? date.substring(0, 10) : "");

        holder.ivSentiment.setImageResource(mapSentiment(article.getSentiment()));
        holder.ivSpam.setImageResource(mapSpam(article.getSpam()));

        holder.itemView.setOnClickListener(v -> onClick.click(article));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img, ivSentiment, ivSpam;
        TextView title, description, chipSource, tvDate;
        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgNews);
            title = v.findViewById(R.id.tvNewsTitle);
            description = v.findViewById(R.id.tvNewsDescription);
            chipSource = v.findViewById(R.id.chipSource);
            tvDate = v.findViewById(R.id.tvDate);
            ivSentiment = v.findViewById(R.id.ivSentiment);
            ivSpam = v.findViewById(R.id.ivSpam);
        }
    }

    private int mapSentiment(String label) {
        if (label == null) return R.drawable.neutral;
        label = label.trim().toLowerCase();
        switch (label) {
            case "tich cuc":
            case "tích cực":
            case "positive": return R.drawable.positive;
            case "tieu cuc":
            case "tiêu cực":
            case "negative": return R.drawable.negative;
            default: return R.drawable.neutral;
        }
    }

    private int mapSpam(String label) {
        if (label == null) return R.drawable.nospam;
        label = label.trim().toLowerCase();
        return label.equals("spam") ? R.drawable.spam : R.drawable.nospam;
    }

    private String domain(String u) {
        if (u == null || u.isEmpty()) return "";
        try {
            java.net.URI uri = new java.net.URI(u);
            String host = uri.getHost();
            return host != null ? host.replaceFirst("^www\\.", "") : "";
        } catch (Exception e) {
            return "";
        }
    }
}

