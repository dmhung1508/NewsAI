package com.example.newsai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.VH> {
    private final List<News> data;
    private final OnItemClick onItemClick;
    public interface OnItemClick { void click(News n); }
    public NewsAdapter(List<News> data, OnItemClick onItemClick){
        this.data = data; this.onItemClick = onItemClick;
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView root;
        ImageView img; TextView title;
        VH(View v){
            super(v);
            root = (MaterialCardView) v;
            img = v.findViewById(R.id.imgNews);
            title = v.findViewById(R.id.tvNewsTitle);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        News n = data.get(pos);
        h.img.setImageResource(n.imageRes);
        h.title.setText(n.title);
        h.root.setOnClickListener(v -> { if (onItemClick != null) onItemClick.click(n); });
    }

    @Override public int getItemCount() { return data.size(); }
}

