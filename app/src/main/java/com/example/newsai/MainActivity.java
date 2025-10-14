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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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

    // Biến dùng cho phân trang bài viết
    private int currentSkip = 0;
    private final int limit = 20;
    private boolean isLoadingMore = false;

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

        // Lắng nghe kéo xuống cuối danh sách để tải thêm (chỉ dùng cho trang chủ)
        rvNews.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // canScrollVertically(1) == false nghĩa là đã ở cuối danh sách
                if (!recyclerView.canScrollVertically(1)
                        && !isLoadingMore
                        && !isClusterMode
                        && currentFilter.equals("home")) {
                    loadMoreNews();
                }
            }
        });

        // Request notification permission and subscribe to topic
        requestNotificationPermission();

        // Tải trang đầu tiên của trang chủ (gồm bài báo và bài Facebook)
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
            // Reset phân trang và tải lại trang đầu tiên
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

        // Web
        view.findViewById(R.id.btnWeb).setOnClickListener(v -> {
            currentFilter = "web";
            tvTitle.setText("Báo điện tử");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            loadNews();               // nạp lại dữ liệu
            bottomSheet.dismiss();
            filterNewsByType("article");
        });

        // Facebook
        view.findViewById(R.id.btnFacebook).setOnClickListener(v -> {
            currentFilter = "facebook";
            tvTitle.setText("Facebook");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            loadNews();               // nạp lại dữ liệu
            bottomSheet.dismiss();
            filterNewsByType("facebook_post");
        });

        // Positive sentiment
        view.findViewById(R.id.btnPositive).setOnClickListener(v -> {
            currentFilter = "positive";
            tvTitle.setText("Tin tích cực");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            bottomSheet.dismiss();
            loadAllNewsThenFilterBySentiment("tich cuc");
        });

        // Negative sentiment
        view.findViewById(R.id.btnNegative).setOnClickListener(v -> {
            currentFilter = "negative";
            tvTitle.setText("Tin tiêu cực");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            bottomSheet.dismiss();
            loadAllNewsThenFilterBySentiment("tieu cuc");
        });

        bottomSheet.show();
    }

    /** Reset phân trang và tải trang đầu tiên gồm articles + facebook_posts */
    private void loadNews() {
        currentSkip = 0;
        allNews = new ArrayList<>();
        newsAdapter.submit(new ArrayList<>());
        loadMoreNews();
    }

    /** Tải thêm 20 bài mới (bài báo + facebook) */
    private void loadMoreNews() {
        isLoadingMore = true;
        ApiService api = ApiClient.get().create(ApiService.class);
        List<NewsItem> newBatch = new ArrayList<>();
        // Gọi API lấy articles với skip/limit
        api.getArticles(currentSkip, limit).enqueue(new Callback<List<NewsItem>>() {
            @Override
            public void onResponse(Call<List<NewsItem>> call, Response<List<NewsItem>> r1) {
                if (r1.isSuccessful() && r1.body() != null) {
                    newBatch.addAll(r1.body());
                }
                // Sau khi lấy articles, lấy facebook_posts với cùng skip/limit
                api.getFacebookPosts(currentSkip, limit).enqueue(new Callback<List<NewsItem>>() {
                    @Override
                    public void onResponse(Call<List<NewsItem>> call2, Response<List<NewsItem>> r2) {
                        if (r2.isSuccessful() && r2.body() != null) {
                            newBatch.addAll(r2.body());
                        }
                        // Cập nhật vị trí skip và danh sách dữ liệu
                        currentSkip += limit;
                        allNews.addAll(newBatch);
                        if (newBatch.size() > 0) {
                            newsAdapter.addAll(newBatch); // cần thêm hàm addAll() trong NewsAdapter
                        }
                        isLoadingMore = false;
                    }

                    @Override public void onFailure(Call<List<NewsItem>> call2, Throwable t) {
                        currentSkip += limit;
                        allNews.addAll(newBatch);
                        if (newBatch.size() > 0) {
                            newsAdapter.addAll(newBatch);
                        }
                        isLoadingMore = false;
                    }
                });
            }

            @Override public void onFailure(Call<List<NewsItem>> call, Throwable t) {
                // Không thành công, dừng trạng thái loading
                isLoadingMore = false;
            }
        });
    }

    /** Lọc theo loại bài trên danh sách đã nạp */
    private void filterNewsByType(String type) {
        if (allNews.isEmpty()) {
            return;
        }
        List<NewsItem> filtered = new ArrayList<>();
        for (NewsItem item : allNews) {
            if (type.equals(item.getType())) {
                filtered.add(item);
            }
        }
        newsAdapter.submit(filtered);
        // Bạn có thể hiển thị Toast nếu danh sách rỗng
    }

    /** Lọc theo nhãn sentiment (positive/negative) */
    private void filterNewsBySentiment(String sentiment) {
        if (allNews.isEmpty()) {
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
    }

    /** Lấy toàn bộ dữ liệu (skip=0) rồi lọc theo sentiment */
    private void loadAllNewsThenFilterBySentiment(final String sentiment) {
        ApiService api = ApiClient.get().create(ApiService.class);
        List<NewsItem> combined = new ArrayList<>();
        api.getArticles(0, 100).enqueue(new Callback<List<NewsItem>>() {
            @Override
            public void onResponse(Call<List<NewsItem>> call, Response<List<NewsItem>> r1) {
                if (r1.isSuccessful() && r1.body() != null) {
                    combined.addAll(r1.body());
                }
                api.getFacebookPosts(0, 100).enqueue(new Callback<List<NewsItem>>() {
                    @Override
                    public void onResponse(Call<List<NewsItem>> call2, Response<List<NewsItem>> r2) {
                        if (r2.isSuccessful() && r2.body() != null) {
                            combined.addAll(r2.body());
                        }
                        allNews = combined;
                        filterNewsBySentiment(sentiment);
                    }

                    @Override public void onFailure(Call<List<NewsItem>> call2, Throwable t) {
                        allNews = combined;
                        filterNewsBySentiment(sentiment);
                    }
                });
            }

            @Override public void onFailure(Call<List<NewsItem>> call, Throwable t) {
                Log.e("API", "FAIL", t);
            }
        });
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
        String img = (it.getImage_contents() != null && !it.getImage_contents().isEmpty())
                ? it.getImage_contents().get(0) : null;
        intent.putExtra(DetailActivity.K_TITLE, it.getTitle());
        intent.putExtra(DetailActivity.K_IMAGE, img);
        intent.putExtra(DetailActivity.K_URL, it.getUrl());
        intent.putExtra(DetailActivity.K_SOURCE_URL, it.getSource_url());
        intent.putExtra(DetailActivity.K_CONTENT, it.getText_content());
        intent.putExtra(DetailActivity.K_DATE, it.getCrawled_at());
        intent.putExtra(DetailActivity.K_SENTIMENT, it.getSentiment_label());
        intent.putExtra(DetailActivity.K_SPAM, it.getSpam_label());
        intent.putExtra(DetailActivity.K_POSTED, it.getPosted_at());
        startActivity(intent);
    }

    private void openClusterDetail(ClusterItem cluster) {
        Intent intent = new Intent(this, ClusterDetailActivity.class);
        intent.putExtra("cluster_id", cluster.getCluster_id());
        startActivity(intent);
    }
}
