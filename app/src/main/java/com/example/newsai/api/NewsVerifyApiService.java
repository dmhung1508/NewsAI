package com.example.newsai.api;

import com.example.newsai.models.ExtractRequest;
import com.example.newsai.models.ExtractResponse;
import com.example.newsai.models.SearchRequest;
import com.example.newsai.models.SearchResponse;
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
    @POST("search")
    Call<SearchResponse> searchNews(@Body SearchRequest request);
    
    @Headers({
        "accept: application/json",
        "Content-Type: application/json"
    })
    @POST("extract")
    Call<ExtractResponse> extractContent(@Body ExtractRequest request);
    
    @Headers({
        "accept: application/json",
        "Content-Type: application/json"
    })
    @POST("verify")
    Call<VerifyResponse> verifyNews(@Body VerifyRequest request);
}
