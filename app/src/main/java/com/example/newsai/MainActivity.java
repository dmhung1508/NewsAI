package com.example.newsai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newsai.data.ClusterItem;
import com.example.newsai.data.NewsItem;
import com.example.newsai.network.ApiClient;
import com.example.newsai.network.ApiService;
import com.example.newsai.ui.ClusterAdapter;
import com.example.newsai.ui.NewsAdapter;

import java.text.SimpleDateFormat;
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

        // Toggle between News and Clusters
        ivMenu.setOnClickListener(v -> toggleMode());
        
        ImageView btnAccount = findViewById(R.id.btnAccount);
        btnAccount.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        loadNews();
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
                if (res.isSuccessful() && res.body() != null) newsAdapter.submit(res.body());
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