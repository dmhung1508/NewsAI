package com.example.newsai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatbotActivity extends AppCompatActivity {
    public static final String EXTRA_ARTICLE_TITLE = "extra_article_title";
    public static final String EXTRA_ARTICLE_URL = "extra_article_url";
    public static final String EXTRA_HISTORY_KEY = "extra_history_key";
    private static final String PREF_CHAT_HISTORY = "pref_chat_history";
    private static final String DEFAULT_HISTORY_KEY = "history_default";
    private static final String TAG = "ChatbotActivity";
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private EditText edtMessage;
    private ImageButton btnSend;
    private GrokApiService grokApiService;
    private SharedPreferences chatPreferences;
    private String historyKey;
    private String articleTitle;
    private String articleUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        recyclerView = findViewById(R.id.recyclerViewChat);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        ImageButton btnBack = findViewById(R.id.btnBack);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);


        grokApiService = new GrokApiService();

        articleTitle = getIntent().getStringExtra(EXTRA_ARTICLE_TITLE);
        articleUrl = getIntent().getStringExtra(EXTRA_ARTICLE_URL);
        historyKey = getIntent().getStringExtra(EXTRA_HISTORY_KEY);
        if (TextUtils.isEmpty(historyKey)) {
            historyKey = DEFAULT_HISTORY_KEY;
        }
        chatPreferences = getSharedPreferences(PREF_CHAT_HISTORY, MODE_PRIVATE);

        initializeConversation();

        btnSend.setOnClickListener(v -> sendMessage());
        btnBack.setOnClickListener(v -> finish());

        // Xử lý click gợi ý
        chatAdapter.setOnSuggestionClickListener(suggestion -> {
            edtMessage.setText(suggestion);
            sendMessage();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (grokApiService != null) {
            grokApiService.shutdown();
        }
    }

    private void sendMessage() {
        String messageText = edtMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            // Thêm tin nhắn của user
            messageList.add(new ChatMessage(messageText, true));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
            edtMessage.setText("");
            saveHistory();

            // Disable nút gửi trong khi chờ phản hồi
            btnSend.setEnabled(false);

            // Thêm tin nhắn "Đang suy nghĩ..."
            ChatMessage thinkingMsg = new ChatMessage("Đang suy nghĩ...", false);
            messageList.add(thinkingMsg);
            int thinkingPosition = messageList.size() - 1;
            chatAdapter.notifyItemInserted(thinkingPosition);
            recyclerView.scrollToPosition(thinkingPosition);

            List<ChatMessage> payloadHistory = buildHistoryForModel(thinkingPosition);
            grokApiService.sendConversation(payloadHistory, articleTitle, articleUrl, new GrokApiService.GrokCallback() {
                @Override
                public void onSuccess(String response) {
                    // Xóa tin nhắn "Đang suy nghĩ..."
                    messageList.remove(thinkingPosition);
                    chatAdapter.notifyItemRemoved(thinkingPosition);

                    // Thêm phản hồi từ Grok
                    messageList.add(new ChatMessage(response, false));
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                    saveHistory();
                    // Enable lại nút gửi
                    btnSend.setEnabled(true);
                }

                @Override
                public void onError(String error) {
                    // Xóa tin nhắn "Đang suy nghĩ..."
                    messageList.remove(thinkingPosition);
                    chatAdapter.notifyItemRemoved(thinkingPosition);

                    // Hiển thị lỗi
                    Toast.makeText(ChatbotActivity.this, error, Toast.LENGTH_SHORT).show();


                    messageList.add(new ChatMessage(
                        "Xin lỗi, hiện tại tôi gặp sự cố kết nối với Grok AI. Vui lòng thử lại sau.",
                        false
                    ));
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                    saveHistory();
                    btnSend.setEnabled(true);
                }
            });
        }
    }

    private List<ChatMessage> buildHistoryForModel(int excludeIndex) {
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 0; i < messageList.size(); i++) {
            if (i == excludeIndex) continue;
            history.add(messageList.get(i));
        }
        return history;
    }

    private void initializeConversation() {
        if (!loadHistory()) {
            ChatMessage welcomeMsg = new ChatMessage(
                buildWelcomeMessage(articleTitle, articleUrl),
                false
            );
            welcomeMsg.setSuggestions(buildSuggestions(articleTitle));
            messageList.add(welcomeMsg);
            chatAdapter.notifyDataSetChanged();
            saveHistory();
        } else {
            chatAdapter.notifyDataSetChanged();
        }
    }

    private boolean loadHistory() {
        String historyJson = chatPreferences.getString(historyKey, null);
        if (TextUtils.isEmpty(historyJson)) return false;
        try {
            JSONArray array = new JSONArray(historyJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ChatMessage message = new ChatMessage(
                    obj.optString("content", ""),
                    obj.optBoolean("isUser", false)
                );
                if (obj.has("suggestions")) {
                    JSONArray sugArr = obj.getJSONArray("suggestions");
                    String[] suggestions = new String[sugArr.length()];
                    for (int j = 0; j < sugArr.length(); j++) {
                        suggestions[j] = sugArr.optString(j);
                    }
                    message.setSuggestions(suggestions);
                }
                messageList.add(message);
            }
            return !messageList.isEmpty();
        } catch (JSONException e) {
            Log.e(TAG, "loadHistory error", e);
            return false;
        }
    }

    private void saveHistory() {
        if (chatPreferences == null || TextUtils.isEmpty(historyKey)) return;
        try {
            JSONArray array = new JSONArray();
            for (ChatMessage message : messageList) {
                JSONObject obj = new JSONObject();
                obj.put("content", message.getContent());
                obj.put("isUser", message.isUser());
                if (message.hasSuggestions()) {
                    JSONArray sugArr = new JSONArray();
                    for (String suggestion : message.getSuggestions()) {
                        sugArr.put(suggestion);
                    }
                    obj.put("suggestions", sugArr);
                }
                array.put(obj);
            }
            chatPreferences.edit().putString(historyKey, array.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "saveHistory error", e);
        }
    }

    private String buildWelcomeMessage(String title, String url) {
        StringBuilder sb = new StringBuilder("Xin chào, mình là NewsBot, mình sẽ giải đáp tất cả thắc mắc của bạn");
        if (!TextUtils.isEmpty(title)) {
            sb.append(" về \"").append(title).append("\"");
        }
        sb.append(". ");
        if (!TextUtils.isEmpty(url)) {
            sb.append("Nguồn: ").append(url).append(". ");
        }
        if (!TextUtils.isEmpty(title)) {
            sb.append("Bạn muốn mình phân tích hay giải thích thêm thông tin nào?");
        } else {
            sb.append("Hãy cho mình biết bạn đang quan tâm đến bài viết nào để mình hỗ trợ nhé.");
        }
        return sb.toString().trim();
    }

    private String[] buildSuggestions(String title) {
        if (TextUtils.isEmpty(title)) {
            return new String[]{
                "Bài viết này nói về điều gì?",
                "Những điểm đáng chú ý nhất là gì?"
            };
        }
        return new String[]{
            "Tóm tắt nội dung chính của \"" + title + "\"",
            "Ảnh hưởng của \"" + title + "\" là gì?"
        };
    }
}
