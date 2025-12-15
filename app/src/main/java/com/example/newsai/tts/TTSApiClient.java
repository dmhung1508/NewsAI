package com.example.newsai.tts;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

public class TTSApiClient {
    private static final String BASE_URL = "http://14.224.224.25:4500/";
    private static Retrofit instance;

    public static Retrofit get() {
        if (instance == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .build();

            instance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .build();
        }
        return instance;
    }

    public static TTSApiService getService() {
        return get().create(TTSApiService.class);
    }
}

