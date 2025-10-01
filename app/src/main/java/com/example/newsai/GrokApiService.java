package com.example.newsai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GrokApiService {
    private static final String TAG = "GrokApiService";
    private static final String API_URL = "https://api.x.ai/v1/chat/completions";
    private static final String API_KEY = "";
    
    private final OkHttpClient client;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface GrokCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public GrokApiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void sendMessage(String userMessage, GrokCallback callback) {
        executorService.execute(() -> {
            try {
                // Tạo JSON request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "grok-4-fast-non-reasoning");
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 1000);
                
                JSONArray messages = new JSONArray();
                
                // System message
                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", "Bạn là NewsBot, một trợ lý AI chuyên về tin tức và sự kiện thời sự. Hãy trả lời bằng tiếng Việt một cách thân thiện và chính xác.");
                messages.put(systemMsg);
                
                // User message
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", userMessage);
                messages.put(userMsg);
                
                requestBody.put("messages", messages);
                
                // Tạo request
                RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
                );
                
                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();
                
                // Gọi API
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response: " + responseBody);
                    
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    if (choices.length() > 0) {
                        JSONObject firstChoice = choices.getJSONObject(0);
                        JSONObject message = firstChoice.getJSONObject("message");
                        String content = message.getString("content");
                        
                        // Callback trên main thread
                        mainHandler.post(() -> callback.onSuccess(content));
                    } else {
                        mainHandler.post(() -> callback.onError("Không có phản hồi từ Grok"));
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "API Error: " + response.code() + " - " + errorBody);
                    mainHandler.post(() -> callback.onError("Lỗi API: " + response.code()));
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Network error", e);
                mainHandler.post(() -> callback.onError("Lỗi kết nối: " + e.getMessage()));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing response", e);
                mainHandler.post(() -> callback.onError("Lỗi xử lý: " + e.getMessage()));
            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}

