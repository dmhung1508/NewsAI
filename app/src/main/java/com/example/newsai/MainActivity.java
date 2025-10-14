package com.example.newsai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newsai.data.ClusterItem;
import com.example.newsai.data.NewsItem;
import com.example.newsai.network.ApiClient;
import com.example.newsai.network.ApiService;
import com.example.newsai.ui.ClusterAdapter;
import com.example.newsai.ui.NewsAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvNews;
    private NewsAdapter newsAdapter;
    private ClusterAdapter clusterAdapter;
    private TextView tvTitle;
    private ImageButton ivMenu;
    private boolean isClusterMode = false;
    private String currentFilter = "home"; // home, newest, web, facebook, positive, negative
    private List<NewsItem> allNews = new ArrayList<>();
    
    // Notification permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("FCM", "Notification permission granted");
                    subscribeToNewsClusters();
                } else {
                    Log.d("FCM", "Notification permission denied");
                    Toast.makeText(this, "Bạn sẽ không nhận được thông báo cụm tin mới", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTitle = findViewById(R.id.tvTitle);
        ivMenu = findViewById(R.id.ivMenu);
        rvNews = findViewById(R.id.rvNews);
        rvNews.setLayoutManager(new LinearLayoutManager(this));
        
        newsAdapter = new NewsAdapter(this::openDetail);
        clusterAdapter = new ClusterAdapter(this::openClusterDetail);
        rvNews.setAdapter(newsAdapter);

        TextView tvDate = findViewById(R.id.tvDate);
        tvDate.setText(new SimpleDateFormat("EEE, dd.MM", Locale.getDefault()).format(new Date()));

        // Open filter menu
        ivMenu.setOnClickListener(v -> showFilterMenu());
        
        ImageView btnAccount = findViewById(R.id.btnAccount);
        btnAccount.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Request notification permission and subscribe to topic
        requestNotificationPermission();

        loadNews();
    }
    
    private void showFilterMenu() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null);
        bottomSheet.setContentView(view);
        
        // Home
        view.findViewById(R.id.btnHome).setOnClickListener(v -> {
            currentFilter = "home";
            tvTitle.setText("Trang chủ");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            loadNews();
            bottomSheet.dismiss();
        });
        
        // Clusters
        view.findViewById(R.id.btnClusters).setOnClickListener(v -> {
            currentFilter = "clusters";
            tvTitle.setText("Cụm tin");
            isClusterMode = true;
            rvNews.setAdapter(clusterAdapter);
            loadClusters();
            bottomSheet.dismiss();
        });
        
        // Newest
        view.findViewById(R.id.btnNewest).setOnClickListener(v -> {
            currentFilter = "newest";
            tvTitle.setText("Mới nhất");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            loadNews();
            bottomSheet.dismiss();
        });
        
        // Web
        view.findViewById(R.id.btnWeb).setOnClickListener(v -> {
            currentFilter = "web";
            tvTitle.setText("Báo điện tử");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            filterNewsByType("article");
            bottomSheet.dismiss();
        });
        
        // Facebook
        view.findViewById(R.id.btnFacebook).setOnClickListener(v -> {
            currentFilter = "facebook";
            tvTitle.setText("Facebook");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            filterNewsByType("facebook_post");
            bottomSheet.dismiss();
        });
        
        // Positive sentiment
        view.findViewById(R.id.btnPositive).setOnClickListener(v -> {
            currentFilter = "positive";
            tvTitle.setText("Tin tích cực");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            filterNewsBySentiment("positive");
            bottomSheet.dismiss();
        });
        
        // Negative sentiment
        view.findViewById(R.id.btnNegative).setOnClickListener(v -> {
            currentFilter = "negative";
            tvTitle.setText("Tin tiêu cực");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            filterNewsBySentiment("negative");
            bottomSheet.dismiss();
        });
        
        bottomSheet.show();
    }
    
    private void filterNewsByType(String type) {
        if (allNews.isEmpty()) {
            loadNews();
            return;
        }
        
        List<NewsItem> filtered = new ArrayList<>();
        for (NewsItem item : allNews) {
            if (type.equals(item.getType())) {
                filtered.add(item);
            }
        }
        newsAdapter.submit(filtered);
        
        if (filtered.isEmpty()) {
            Toast.makeText(this, "Không có bài viết nào", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void filterNewsBySentiment(String sentiment) {
        if (allNews.isEmpty()) {
            loadNews();
            return;
        }
        
        List<NewsItem> filtered = new ArrayList<>();
        for (NewsItem item : allNews) {
            String itemSentiment = item.getSentiment_label();
            if (sentiment.equals(itemSentiment)) {
                filtered.add(item);
            }
        }
        newsAdapter.submit(filtered);
        
        if (filtered.isEmpty()) {
            Toast.makeText(this, "Không có bài viết nào", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                subscribeToNewsClusters();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            subscribeToNewsClusters();
        }
    }
    
    private void subscribeToNewsClusters() {
        FirebaseMessaging.getInstance().subscribeToTopic("news_clusters")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "Subscribed to news_clusters topic");
                    } else {
                        Log.e("FCM", "Failed to subscribe to topic", task.getException());
                    }
                });
    }

    private void toggleMode() {
        isClusterMode = !isClusterMode;
        
        if (isClusterMode) {
            tvTitle.setText("Cụm tin");
            rvNews.setAdapter(clusterAdapter);
            loadClusters();
        } else {
            tvTitle.setText("Mới và Hot");
            rvNews.setAdapter(newsAdapter);
            loadNews();
        }
    }

    private void loadNews() {
        ApiService api = ApiClient.get().create(ApiService.class);
        api.getArticles().enqueue(new Callback<List<NewsItem>>() {
            @Override public void onResponse(Call<List<NewsItem>> call, Response<List<NewsItem>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    allNews = res.body();
                    newsAdapter.submit(allNews);
                }
                else Log.e("API", "HTTP " + res.code());
            }
            @Override public void onFailure(Call<List<NewsItem>> call, Throwable t) {
                Log.e("API", "FAIL", t);
            }
        });
    }
    
    private void loadClusters() {
        ApiService api = ApiClient.get().create(ApiService.class);
        api.getTopClusters(20).enqueue(new Callback<List<ClusterItem>>() {
            @Override public void onResponse(Call<List<ClusterItem>> call, Response<List<ClusterItem>> res) {
                if (res.isSuccessful() && res.body() != null) clusterAdapter.submit(res.body());
                else Log.e("API", "HTTP " + res.code());
            }
            @Override public void onFailure(Call<List<ClusterItem>> call, Throwable t) {
                Log.e("API", "FAIL", t);
            }
        });
    }

    private void openDetail(NewsItem it) {
        Intent intent = new Intent(this, DetailActivity.class);
        String img = (it.getImage_contents()!=null && !it.getImage_contents().isEmpty())
                ? it.getImage_contents().get(0) : null;
        intent.putExtra(DetailActivity.K_TITLE, it.getTitle());
        intent.putExtra(DetailActivity.K_IMAGE, img);
        intent.putExtra(DetailActivity.K_URL, it.getUrl());
        intent.putExtra(DetailActivity.K_SOURCE_URL, it.getSource_url());
        intent.putExtra(DetailActivity.K_CONTENT, it.getText_content());
        intent.putExtra(DetailActivity.K_DATE, it.getCrawled_at());
        startActivity(intent);
    }
    
    private void openClusterDetail(ClusterItem cluster) {
        Intent intent = new Intent(this, ClusterDetailActivity.class);
        intent.putExtra("cluster_id", cluster.getCluster_id());
        startActivity(intent);
    }


}