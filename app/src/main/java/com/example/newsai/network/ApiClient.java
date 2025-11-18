package com.example.newsai.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class ApiClient {
    // Main API instance (for news articles, clusters, etc.)
    private static Retrofit mainApiInstance;
    
    // News Verify API instance (for search, extract, verify)
    private static Retrofit newsVerifyApiInstance;
    
    // Base URLs
    private static final String MAIN_API_BASE_URL = "http://43.228.212.108:8001/";
    private static final String NEWS_VERIFY_API_BASE_URL = "https://db.dinhmanhhung.net/";

    /**
     * Get main API instance (for news articles, clusters)
     * Uses Moshi converter
     */
    public static Retrofit get() {
        if (mainApiInstance == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .build();

            mainApiInstance = new Retrofit.Builder()
                    .baseUrl(MAIN_API_BASE_URL)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .client(client)
                    .build();
        }
        return mainApiInstance;
    }
    
    /**
     * Get News Verify API instance (for search, extract, verify)
     * Uses Gson converter with extended timeout
     */
    public static Retrofit getNewsVerifyClient() {
        if (newsVerifyApiInstance == null) {
            // Tăng timeout vì API có thể mất thời gian xử lý
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
            
            newsVerifyApiInstance = new Retrofit.Builder()
                    .baseUrl(NEWS_VERIFY_API_BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return newsVerifyApiInstance;
    }

    /**
     * Get NewsVerifyApiService instance
     */
    public static NewsVerifyApiService getNewsVerifyApiService() {
        return getNewsVerifyClient().create(NewsVerifyApiService.class);
    }
}