package com.example.newsai.api;

import com.example.newsai.models.VerifyRequest;
import com.example.newsai.models.VerifyResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface NewsVerifyApiService {
    
    @Headers({
        "accept: application/json",
        "Content-Type: application/json"
    })
    @POST("checkMxh")
    Call<VerifyResponse> verifyNews(@Body VerifyRequest request);
}
