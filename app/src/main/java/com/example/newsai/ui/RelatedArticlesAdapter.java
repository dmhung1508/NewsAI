package com.example.newsai.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.newsai.R;
import com.example.newsai.data.NewsItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RelatedArticlesAdapter extends RecyclerView.Adapter<RelatedArticlesAdapter.ViewHolder> {

    private final Context context;
    private final List<NewsItem> articles = new ArrayList<>();
    private OnItemClickListener listener;
    private String currentArticleId; // To exclude current article

    public interface OnItemClickListener {
        void onItemClick(NewsItem article);
    }

    public RelatedArticlesAdapter(Context context) {
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setCurrentArticleId(String id) {
        this.currentArticleId = id;
    }

    public void submitList(List<NewsItem> newArticles) {
        articles.clear();
        if (newArticles != null) {
            for (NewsItem article : newArticles) {
                // Exclude current article
                if (currentArticleId == null || !currentArticleId.equals(article.get_id())) {
                    articles.add(article);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_related_article, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NewsItem article = articles.get(position);
        holder.bind(article);
    }

    @Override
    public int getItemCount() {
        return Math.min(articles.size(), 10); // Max 10 related articles
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgThumbnail;
        private final TextView tvTitle;
        private final TextView tvMeta;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgRelatedThumbnail);
            tvTitle = itemView.findViewById(R.id.tvRelatedTitle);
            tvMeta = itemView.findViewById(R.id.tvRelatedMeta);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(articles.get(pos));
                }
            });
        }

        void bind(NewsItem article) {
            // Title
            tvTitle.setText(article.getTitle());

            // Meta: Source + Time
            String source = extractSource(article.getSource_url());
            String timeAgo = formatTimeAgo(article.getPosted_at());
            tvMeta.setText(source + " • " + timeAgo);

            // Image
            List<String> images = article.getImage_contents();
            if (images != null && !images.isEmpty()) {
                String imageUrl = images.get(0);
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.hotnews)
                        .error(R.drawable.hotnews)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(imgThumbnail);
            } else {
                imgThumbnail.setImageResource(R.drawable.hotnews);
            }
        }

        private String extractSource(String url) {
            if (TextUtils.isEmpty(url))
                return "NewsAI";
            try {
                String domain = url.replace("https://", "").replace("http://", "");
                int slashIndex = domain.indexOf('/');
                if (slashIndex > 0) {
                    domain = domain.substring(0, slashIndex);
                }
                // Remove www.
                domain = domain.replace("www.", "");
                // Capitalize first letter
                if (domain.length() > 0) {
                    return domain.substring(0, 1).toUpperCase() + domain.substring(1);
                }
                return domain;
            } catch (Exception e) {
                return "NewsAI";
            }
        }

        private String formatTimeAgo(String postedAt) {
            if (TextUtils.isEmpty(postedAt))
                return "";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(postedAt.replace("Z", "").split("\\.")[0]);
                if (date == null)
                    return "";

                long diffMs = System.currentTimeMillis() - date.getTime();
                long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
                long days = TimeUnit.MILLISECONDS.toDays(diffMs);

                if (days > 0) {
                    return days + " ngày trước";
                } else if (hours > 0) {
                    return hours + " giờ trước";
                } else {
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
                    return Math.max(1, minutes) + " phút trước";
                }
            } catch (ParseException e) {
                return "";
            }
        }
    }
}
