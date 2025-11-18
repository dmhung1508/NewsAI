package com.example.newsai.tts;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface TTSApiService {
    @FormUrlEncoded
    @POST("synthesize-text")
    Call<ResponseBody> synthesizeText(
            @Field("text") String text,
            @Field("voice_id") Integer voiceId
    );
}

