package com.example.newsai;

public class News {
    public final int imageRes;   // R.drawable.xxx
    public final String title;
    public final String url;     // nếu có link
    public News(int imageRes, String title, String url) {
        this.imageRes = imageRes; this.title = title; this.url = url;
    }
}
