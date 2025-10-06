package com.example.newsai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

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
        String title = it.getStringExtra(K_TITLE);
        String image = it.getStringExtra(K_IMAGE);
        String url = it.getStringExtra(K_URL);
        String sourceUrl = it.getStringExtra(K_SOURCE_URL);
        String content = it.getStringExtra(K_CONTENT);
        String date = it.getStringExtra(K_DATE);

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
