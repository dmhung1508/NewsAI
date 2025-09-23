package com.example.newsai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView rv = findViewById(R.id.rvNews);
        rv.setLayoutManager(new LinearLayoutManager(this)); // danh sách dọc

        List<News> items = new ArrayList<>();
        items.add(new News(R.drawable.news1, "Tiêu đề 1", "https://example.com/1"));
        items.add(new News(R.drawable.news1, "Tiêu đề 2", "https://example.com/2"));
        items.add(new News(R.drawable.news1, "Tiêu đề 3", "https://example.com/3"));

        NewsAdapter adapter = new NewsAdapter(items, n -> {
            // click vào ảnh hoặc tiêu đề (cả card) đều vào đây
            if (n.url != null && n.url.startsWith("http")) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(n.url)));
            } else {
                Toast.makeText(this, n.title, Toast.LENGTH_SHORT).show();
            }
        });
        rv.setAdapter(adapter);
    }
}
