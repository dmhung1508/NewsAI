package com.example.newsai.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class ApiClient {
    private static Retrofit instance;

    public static Retrofit get() {
        if (instance == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .build();

            instance = new Retrofit.Builder()
                    .baseUrl("https://db.dinhmanhhung.net/")   // emulator ↔ host
                    .addConverterFactory(MoshiConverterFactory.create())
                    .client(client)
                    .build();
        }
        return instance;
    }
}