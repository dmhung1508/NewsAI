package com.example.newsai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.example.newsai.data.ClusterItem;
import com.example.newsai.data.NewsItem;
import com.example.newsai.network.ApiClient;
import com.example.newsai.network.ApiService;
import com.example.newsai.ui.ImagePagerAdapter;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClusterDetailActivity extends AppCompatActivity {

    private LinearLayout articlesContainer;
    private String clusterId;
    private ViewPager2 viewPager;
    private RecyclerView dotsIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cluster_detail);

        Intent intent = getIntent();
        clusterId = intent.getStringExtra("cluster_id");
        
        Log.d("ClusterDetail", "Received cluster_id: " + clusterId);

        viewPager = findViewById(R.id.viewPager);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvSummary = findViewById(R.id.tvSummary);
        TextView tvMeta = findViewById(R.id.tvMeta);
        articlesContainer = findViewById(R.id.articlesContainer);

        if (clusterId == null || clusterId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy cụm tin", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadClusterDetail();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        
        // mở cluster từ noti
        String newClusterId = intent.getStringExtra("cluster_id");

        
        if (newClusterId != null && !newClusterId.equals(clusterId)) {
            clusterId = newClusterId;
            articlesContainer.removeAllViews();
            loadClusterDetail();
        }
    }

    private void loadClusterDetail() {
        if (clusterId == null) return;

        ApiService api = ApiClient.get().create(ApiService.class);
        api.getClusterById(clusterId).enqueue(new Callback<ClusterItem>() {
            @Override
            public void onResponse(Call<ClusterItem> call, Response<ClusterItem> res) {
                if (res.isSuccessful() && res.body() != null) {
                    ClusterItem cluster = res.body();
                    displayClusterInfo(cluster);
                }
            }

            @Override
            public void onFailure(Call<ClusterItem> call, Throwable t) {
                Log.e("API", "FAIL load cluster", t);
            }
        });
    }

    private void loadArticlesByIds(List<String> articleIds) {
        articlesContainer.removeAllViews();
        
        ApiService api = ApiClient.get().create(ApiService.class);
        
        for (int i = 0; i < articleIds.size(); i++) {
            String articleId = articleIds.get(i);
            int rank = i;
            
            api.getArticleById(articleId).enqueue(new Callback<NewsItem>() {
                @Override
                public void onResponse(Call<NewsItem> call, Response<NewsItem> res) {
                    if (res.isSuccessful() && res.body() != null) {
                        NewsItem article = res.body();
                        addArticleView(article, rank);
                    }
                }

                @Override
                public void onFailure(Call<NewsItem> call, Throwable t) {
                    Log.e("ClusterDetail", "Failed to load article " + articleId, t);
                }
            });
        }
    }

    private void addArticleView(NewsItem article, int rank) {
        View itemView = getLayoutInflater().inflate(R.layout.item_cluster_article, articlesContainer, false);
        
        TextView tvArticleTitle = itemView.findViewById(R.id.tvArticleTitle);
        TextView tvArticleText = itemView.findViewById(R.id.tvArticleText);
        TextView tvSourceBadge = itemView.findViewById(R.id.tvSourceBadge);
        ImageView imgArticle = itemView.findViewById(R.id.imgArticle);
        
        // xử lí hiển thị tiêu đề
        String title = article.getTitle();
        if (title != null && !title.isEmpty()) {
            tvArticleTitle.setText(title);
            tvArticleTitle.setVisibility(View.VISIBLE);
        } else {
            // ko có tiêu đề thì lấy 1 đoạn text ngắn làm tiêu đề
            String text = article.getText_content();
            if (text != null && !text.isEmpty()) {
                String preview = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                tvArticleTitle.setText(preview);
                tvArticleTitle.setVisibility(View.VISIBLE);
            } else {
                tvArticleTitle.setText("Bài viết không có tiêu đề");
                tvArticleTitle.setVisibility(View.VISIBLE);
            }
        }
        
        // Hiển thị đoạn text báo ngắn
        String text = article.getText_content();
        if (text != null && !text.isEmpty() && title != null && !title.isEmpty()) {
            tvArticleText.setVisibility(View.VISIBLE);
            // giới hạn 150 ký tự
            String preview = text.length() > 150 ? text.substring(0, 150) + "..." : text;
            tvArticleText.setText(preview);
        } else {
            tvArticleText.setVisibility(View.GONE);
        }
        
        // load ảnh có sẵn
        List<String> images = article.getImage_contents();
        if (images != null && !images.isEmpty() && images.get(0) != null) {
            String imageUrl = images.get(0);
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.hotnews)
                    .error(R.drawable.hotnews)
                    .centerCrop()
                    .into(imgArticle);
        } else {
            // nếu không có ảnh thì hiển thị ảnh mặc định
            imgArticle.setImageResource(R.drawable.hotnews);
            imgArticle.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
        imgArticle.setVisibility(View.VISIBLE);
        
        // Hiển thị nguồn 
        String type = article.getType();
        if (type != null && type.equals("facebook_post")) {
            tvSourceBadge.setText("📱 Facebook");
        } else {
            tvSourceBadge.setText("🌐 Web");
        }
        
        // Click vào thì mở detail
        itemView.setOnClickListener(v -> openArticleDetail(article));
        
        articlesContainer.addView(itemView);
    }

    private void openArticleDetail(NewsItem article) {
        Intent intent = new Intent(this, DetailActivity.class);
        
        // lấy thông tin từng bài xong mở UI detail
        if (article.getTitle() != null) {
            intent.putExtra(DetailActivity.K_TITLE, article.getTitle());
        }
        if (article.getText_content() != null) {
            intent.putExtra(DetailActivity.K_CONTENT, article.getText_content());
        }
        if (article.getUrl() != null) {
            intent.putExtra(DetailActivity.K_URL, article.getUrl());
        }
        if (article.getSource_url() != null) {
            intent.putExtra(DetailActivity.K_SOURCE_URL, article.getSource_url());
        }
        if (article.getCrawled_at() != null) {
            intent.putExtra(DetailActivity.K_DATE, article.getCrawled_at());
        }
        
        // push ảnh nếu có
        List<String> images = article.getImage_contents();
        if (images != null && !images.isEmpty() && images.get(0) != null) {
            intent.putExtra(DetailActivity.K_IMAGE, images.get(0));
        }
        
        startActivity(intent);
    }

    private void displayClusterInfo(ClusterItem cluster) {
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvSummary = findViewById(R.id.tvSummary);
        TextView tvMeta = findViewById(R.id.tvMeta);

        tvTitle.setText(cluster.getTitle() != null ? cluster.getTitle() : "");
        tvSummary.setText(cluster.getSummary() != null ? cluster.getSummary() : "");
        tvMeta.setText((cluster.getPrimary_source() != null ? cluster.getPrimary_source() : "") 
                + " • " + cluster.getArticle_count() + " bài viết");

        // trượt ảnh nếu cụm có nhiều ảnh
        List<String> images = cluster.getImage_contents();
        if (images != null && !images.isEmpty()) {
            viewPager.setVisibility(View.VISIBLE);
            ImagePagerAdapter pagerAdapter = new ImagePagerAdapter(images);
            viewPager.setAdapter(pagerAdapter);
        } else {
            // 7ẩn
            viewPager.setVisibility(View.GONE);
        }

        // mở báo bằng article_id
        List<String> articleIds = cluster.getArticle_ids();
        if (articleIds != null && !articleIds.isEmpty()) {
            loadArticlesByIds(articleIds);
        }
    }

    private void openArticleLink(String link) {
        if (TextUtils.isEmpty(link)) {
            Toast.makeText(this, "Không có đường dẫn bài viết", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
    }
}
