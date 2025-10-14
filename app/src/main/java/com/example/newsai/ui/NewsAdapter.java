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
import com.example.newsai.data.NewsItem;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.VH> {

    public interface OnClick { void click(NewsItem item); }

    private final List<NewsItem> items = new ArrayList<>();
    private final OnClick onClick;

    public NewsAdapter(OnClick onClick) { this.onClick = onClick; }

    public void submit(List<NewsItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_news, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int p) {
        NewsItem it = items.get(p);

        // Title
        String title = it.getTitle();
        if (title == null || title.trim().isEmpty()) {
            String tc = it.getText_content();
            title = tc == null ? "" : (tc.length() > 120 ? tc.substring(0,120) + "…" : tc);
        }
        h.title.setText(title);

        // Image
        String img = (it.getImage_contents()!=null && !it.getImage_contents().isEmpty())
                ? it.getImage_contents().get(0) : null;
        Glide.with(h.img).load(img)
                .placeholder(R.drawable.hotnews)
                .error(R.drawable.hotnews)
                .centerCrop()
                .into(h.img);

        // Source + date
        String src = domain(it.getSource_url() != null ? it.getSource_url() : it.getUrl());
        h.chipSource.setText(src.isEmpty() ? "facebook.com" : src);

        String d = it.getCrawled_at();
        h.tvDate.setText(d != null && d.length() >= 10 ? d.substring(0,10) : "");

        // Sentiment icon
        h.ivSentiment.setImageResource(mapSentiment(it.getSentiment_label()));
        h.ivSpam.setImageResource(mapSpam(it.getSpam_label()));
        // Click
        h.itemView.setOnClickListener(v -> onClick.click(it));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        ImageView img, ivSentiment, ivSpam;
        TextView title, chipSource, tvDate;
        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgNews);
            title = v.findViewById(R.id.tvNewsTitle);
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
            case "binh thuong":
            case "bình thường":
            case "neutral":
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
        } catch (Exception e) { return ""; }
    }
    public void addAll(List<NewsItem> list) {
        int start = items.size();
        items.addAll(list);
        notifyItemRangeInserted(start, list.size());
    }

}