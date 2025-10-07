package com.example.newsai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.newsai.data.NewsItem;
import com.example.newsai.network.ApiClient;
import com.example.newsai.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity {

    public static final String K_TITLE = "k_title";
    public static final String K_IMAGE = "k_image";
    public static final String K_URL = "k_url";
    public static final String K_SOURCE_URL = "k_source_url";
    public static final String K_CONTENT = "k_content";
    public static final String K_DATE = "k_date";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ImageView img = findViewById(R.id.imgHeader);
        TextView tvCaption = findViewById(R.id.tvCaption);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvLede = findViewById(R.id.tvLede);
        TextView tvMeta = findViewById(R.id.tvMeta);
        TextView tvContent = findViewById(R.id.tvContent);
        TextView tvSourceLink = findViewById(R.id.tvSourceLink);
        TextView btnShare = findViewById(R.id.btnShare);
        TextView btnBookmark = findViewById(R.id.btnBookmark);

        Intent it = getIntent();
        
        // Check if article_id is provided (from ClusterArticle)
        String articleId = it.getStringExtra("article_id");
        if (articleId != null) {
            loadArticleById(articleId, img, tvCaption, tvTitle, tvLede, tvMeta, tvContent, tvSourceLink, btnShare, btnBookmark);
            return;
        }
        
        // Otherwise use passed data
        String title = it.getStringExtra(K_TITLE);
        String image = it.getStringExtra(K_IMAGE);
        String url = it.getStringExtra(K_URL);
        String sourceUrl = it.getStringExtra(K_SOURCE_URL);
        String content = it.getStringExtra(K_CONTENT);
        String date = it.getStringExtra(K_DATE);

        displayArticle(img, tvCaption, tvTitle, tvLede, tvMeta, tvContent, tvSourceLink, btnShare, btnBookmark,
                      title, image, url, sourceUrl, content, date);
    }
    
    private void loadArticleById(String articleId, ImageView img, TextView tvCaption, TextView tvTitle, 
                                 TextView tvLede, TextView tvMeta, TextView tvContent, TextView tvSourceLink, 
                                 TextView btnShare, TextView btnBookmark) {
        ApiService api = ApiClient.get().create(ApiService.class);
        api.getArticleById(articleId).enqueue(new Callback<NewsItem>() {
            @Override
            public void onResponse(Call<NewsItem> call, Response<NewsItem> res) {
                if (res.isSuccessful() && res.body() != null) {
                    NewsItem article = res.body();
                    String img_url = (article.getImage_contents() != null && !article.getImage_contents().isEmpty())
                            ? article.getImage_contents().get(0) : null;
                    displayArticle(img, tvCaption, tvTitle, tvLede, tvMeta, tvContent, tvSourceLink, btnShare, btnBookmark,
                                  article.getTitle(), img_url, article.getUrl(), article.getSource_url(), 
                                  article.getText_content(), article.getCrawled_at());
                } else {
                    Toast.makeText(DetailActivity.this, "Không tìm thấy bài viết", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            
            @Override
            public void onFailure(Call<NewsItem> call, Throwable t) {
                Log.e("API", "FAIL", t);
                Toast.makeText(DetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    private void displayArticle(ImageView img, TextView tvCaption, TextView tvTitle, TextView tvLede, 
                                TextView tvMeta, TextView tvContent, TextView tvSourceLink, 
                                TextView btnShare, TextView btnBookmark,
                                String title, String image, String url, String sourceUrl, 
                                String content, String date) {
        // Ảnh + chú thích (caption lấy domain nguồn)
        Glide.with(img).load(image).placeholder(R.drawable.news1).error(R.drawable.news1).centerCrop().into(img);
        tvCaption.setText(domain(sourceUrl != null ? sourceUrl : url));

        // tiêu đề
        tvTitle.setText(safe(title));

        // lede: lấy 1-2 câu đầu từ text_content
        tvLede.setText(makeLede(content));

        // meta (ngày/giờ)
        tvMeta.setText(safe(formatDate(date)));

        // nội dung
        tvContent.setText(safe(content));

        // link nguồn ẩn để click mở
        tvSourceLink.setText(url != null ? url : "");
        tvSourceLink.setOnClickListener(v -> openUrl(url));

        // share
        btnShare.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, title);
            share.putExtra(Intent.EXTRA_TEXT, (title == null ? "" : title) + "\n" + (url == null ? "" : url));
            startActivity(Intent.createChooser(share, "Chia sẻ bài viết"));
        });

        // bookmark demo
        btnBookmark.setOnClickListener(v -> Toast.makeText(this, "Đã lưu (demo)", Toast.LENGTH_SHORT).show());
    }

    private void openUrl(String u) {
        if (TextUtils.isEmpty(u)) {
            Toast.makeText(this, "Không có đường dẫn bài gốc", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));
    }

    private String safe(String s) { return s == null ? "" : s; }

    private String makeLede(String content) {
        if (content == null) return "";
        String c = content.trim();
        if (c.length() > 220) c = c.substring(0, 220) + "…";
        return c;
    }

    private String formatDate(String d) {
        if (d == null || d.length() < 10) return "";
        // d kiểu 2025-10-06T22:31:56.869000 -> lấy ngày
        return d.substring(0, 10);
    }

    private String domain(String u) {
        try {
            if (u == null || u.isEmpty()) return "";
            java.net.URI uri = new java.net.URI(u);
            String host = uri.getHost();
            return host != null ? host.replaceFirst("^www\\.", "") : "";
        } catch (Exception e) {
            return "";
        }
    }
}