package com.example.newsai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ImageButton btnSearch = findViewById(R.id.ivMenu);
        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
            finish();
        });

        ImageButton btnProfile = findViewById(R.id.ivIConHomePage);
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
            finish();
        });
        LinearLayout llnew1 = findViewById(R.id.llNews1);
        llnew1.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            startActivity(intent);
            finish();
        });

        // Nút mở Chatbot
        findViewById(R.id.fabChatbot).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatbotActivity.class);
            startActivity(intent);
        });

        }


}