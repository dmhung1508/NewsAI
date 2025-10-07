package com.example.newsai.network;

import com.example.newsai.data.NewsItem;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("articles")
    Call<List<NewsItem>> getArticles();

    @GET("facebook_posts")
    Call<List<NewsItem>> getFacebookPosts(); // nếu muốn hiển thị bài FB
}