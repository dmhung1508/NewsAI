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

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_news, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int p) {
        NewsItem it = items.get(p);

        String title = it.getTitle();
        if (title == null || title.trim().isEmpty()) {
            String tc = it.getText_content();
            title = tc == null ? "" : (tc.length() > 120 ? tc.substring(0, 120) + "…" : tc);
        }
        h.title.setText(title);

        String img = null;
        if (it.getImage_contents() != null && !it.getImage_contents().isEmpty()) img = it.getImage_contents().get(0);
        Glide.with(h.img).load(img).placeholder(R.drawable.news1).error(R.drawable.news1).centerCrop().into(h.img);

        String src = domain(it.getSource_url() != null ? it.getSource_url() : it.getUrl());
        h.chipSource.setText(src.isEmpty() ? "news" : src);

        String d = it.getCrawled_at();
        h.tvDate.setText(d != null && d.length() >= 10 ? d.substring(0, 10) : "");

        h.itemView.setOnClickListener(v -> onClick.click(it));
    }

    private String domain(String u) {
        if (u == null || u.isEmpty()) return "";
        try {
            java.net.URI uri = new java.net.URI(u);
            String host = uri.getHost();
            return host != null ? host.replaceFirst("^www\\.", "") : "";
        } catch (Exception e) { return ""; }
    }

    @Override
    public int getItemCount() { return items.size(); }

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