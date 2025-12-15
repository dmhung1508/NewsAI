package com.example.newsai;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GrokApiService {
    private static final String TAG = "GrokApiService";
    private static final String API_URL = "https://api.yescale.io/v1/chat/completions";
    private static final String API_KEY = "sk-mLcRIaOFbZF5yJjJpVKn3tsrt7BBp9r8m0Gxl8t1LLBJQTIe";
    
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

    public void sendConversation(List<ChatMessage> history, String articleTitle, String articleUrl, GrokCallback callback) {
        executorService.execute(() -> {
            try {

                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "grok-3-mini-fast-beta");
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 1000);
                
                JSONArray messages = new JSONArray();
                

                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", buildSystemPrompt(articleTitle, articleUrl));
                messages.put(systemMsg);
                
                if (history != null) {
                    for (ChatMessage chatMessage : history) {
                        JSONObject message = new JSONObject();
                        message.put("role", chatMessage.isUser() ? "user" : "assistant");
                        message.put("content", chatMessage.getContent());
                        messages.put(message);
                    }
                }
                
                requestBody.put("messages", messages);

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

    private String buildSystemPrompt(String articleTitle, String articleUrl) {
        StringBuilder sb = new StringBuilder("Bạn là NewsBot, một trợ lý AI chuyên về tin tức và sự kiện thời sự tại Việt Nam. Hãy trả lời bằng tiếng Việt tự nhiên, dễ hiểu và chính xác.");
        if (!TextUtils.isEmpty(articleTitle)) {
            sb.append(" Người dùng hiện đang trao đổi về bài viết \"").append(articleTitle).append("\"");
            if (!TextUtils.isEmpty(articleUrl)) {
                sb.append(" (").append(articleUrl).append(")");
            }
            sb.append(". Hãy ưu tiên sử dụng thông tin liên quan đến bài viết này, đồng thời linh hoạt theo các câu hỏi phát sinh.");
        } else {
            sb.append(" Nếu người dùng chưa cung cấp bài viết cụ thể, hãy hỏi thêm để hiểu ngữ cảnh trước khi phân tích sâu.");
        }
        return sb.toString();
    }
}

