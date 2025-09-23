package com.example.newsai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View.OnClickListener openDetail = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                startActivity(intent);
            }
        };

        ImageView imgNews1 = findViewById(R.id.imgNews1);
        ImageView imgNews2 = findViewById(R.id.imgNews2);
        ImageView imgNews3 = findViewById(R.id.imgNews3);
        TextView tvNewsTitle1 = findViewById(R.id.tvNewsTitle1);
        TextView tvNewsTitle2 = findViewById(R.id.tvNewsTitle2);
        TextView tvNewsTitle3 = findViewById(R.id.tvNewsTitle3);

        if (imgNews1 != null) imgNews1.setOnClickListener(openDetail);
        if (imgNews2 != null) imgNews2.setOnClickListener(openDetail);
        if (imgNews3 != null) imgNews3.setOnClickListener(openDetail);
        if (tvNewsTitle1 != null) tvNewsTitle1.setOnClickListener(openDetail);
        if (tvNewsTitle2 != null) tvNewsTitle2.setOnClickListener(openDetail);
        if (tvNewsTitle3 != null) tvNewsTitle3.setOnClickListener(openDetail);
    }
}