package com.example.newsai.network;

import com.example.newsai.data.NewsItem;
import com.example.newsai.data.ClusterItem;
import com.example.newsai.data.ClusterArticleItem;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("articles")
    Call<List<NewsItem>> getArticles();

    @GET("facebook_posts")
    Call<List<NewsItem>> getFacebookPosts();
    
    // Get article by ID
    @GET("articles/{article_id}")
    Call<NewsItem> getArticleById(@Path("article_id") String articleId);
    
    // Cluster endpoints
    @GET("clusters/top")
    Call<List<ClusterItem>> getTopClusters(@Query("n") int n);
    
    @GET("clusters/{cluster_id}")
    Call<ClusterItem> getClusterById(@Path("cluster_id") String clusterId);
    
    @GET("clusters/{cluster_id}/articles")
    Call<List<ClusterArticleItem>> getClusterArticles(@Path("cluster_id") String clusterId);
}