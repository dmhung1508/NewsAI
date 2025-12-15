package com.example.newsai.stats;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface StatsApiService {

    @FormUrlEncoded
    @POST("send-daily-email")
    Call<StatsApiResponse> sendDailyEmail(@Field("email") String email, @Field("username") String username);

    @FormUrlEncoded
    @POST("send-weekly-email")
    Call<StatsApiResponse> sendWeeklyEmail(@Field("email") String email, @Field("username") String username);

    @FormUrlEncoded
    @POST("send-monthly-email")
    Call<StatsApiResponse> sendMonthlyEmail(@Field("email") String email, @Field("username") String username);

    @FormUrlEncoded
    @POST("send-yearly-email")
    Call<StatsApiResponse> sendYearlyEmail(@Field("email") String email, @Field("username") String username);
}

