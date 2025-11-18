package com.example.newsai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newsai.data.BookmarkStorage;
import com.example.newsai.data.SavedArticle;
import com.example.newsai.ui.SavedArticleAdapter;

import java.util.List;

public class BookmarksActivity extends AppCompatActivity {

    private SavedArticleAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        RecyclerView recyclerView = findViewById(R.id.recyclerBookmarks);
        tvEmpty = findViewById(R.id.tvEmptyBookmarks);
        ImageButton btnBack = findViewById(R.id.btnBackBookmark);

        adapter = new SavedArticleAdapter(this::openDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookmarks();
    }

    private void loadBookmarks() {
        List<SavedArticle> data = BookmarkStorage.getBookmarks(this);
        adapter.submit(data);
        tvEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openDetail(SavedArticle article) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.K_TITLE, article.getTitle());
        intent.putExtra(DetailActivity.K_IMAGE, article.getImageUrl());
        intent.putExtra(DetailActivity.K_URL, article.getUrl());
        intent.putExtra(DetailActivity.K_SOURCE_URL, article.getSourceUrl());
        intent.putExtra(DetailActivity.K_CONTENT, article.getContent());
        intent.putExtra(DetailActivity.K_DATE, article.getDate());
        intent.putExtra(DetailActivity.K_POSTED, article.getPostedAt());
        intent.putExtra(DetailActivity.K_SENTIMENT, article.getSentiment());
        intent.putExtra(DetailActivity.K_SPAM, article.getSpam());
        startActivity(intent);
    }
}

