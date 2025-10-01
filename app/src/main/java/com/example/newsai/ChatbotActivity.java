package com.example.newsai;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatbotActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private EditText edtMessage;
    private ImageButton btnSend;
    private GrokApiService grokApiService;

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


        ChatMessage welcomeMsg = new ChatMessage(
            "Xin chào, mình là NewsBot được hỗ trợ bởi Grok AI. Tôi có thể giúp bạn về tin tức, bạn có vấn đề thắc mắc nào về tin: \"Cuộc xung đột giữa Israel và phong trào Hamas: Hơn 1.100 người thiệt mạng trong cuộc chiến đẫm máu\"",
            false
        );
        welcomeMsg.setSuggestions(new String[]{
            "Cuộc xung đột này bắt đầu từ khi nào?",
            "Nguyên nhân của xung đột là gì?"
        });
        messageList.add(welcomeMsg);
        chatAdapter.notifyDataSetChanged();

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

            // Disable nút gửi trong khi chờ phản hồi
            btnSend.setEnabled(false);

            // Thêm tin nhắn "Đang suy nghĩ..."
            ChatMessage thinkingMsg = new ChatMessage("Đang suy nghĩ...", false);
            messageList.add(thinkingMsg);
            int thinkingPosition = messageList.size() - 1;
            chatAdapter.notifyItemInserted(thinkingPosition);
            recyclerView.scrollToPosition(thinkingPosition);

            grokApiService.sendMessage(messageText, new GrokApiService.GrokCallback() {
                @Override
                public void onSuccess(String response) {
                    // Xóa tin nhắn "Đang suy nghĩ..."
                    messageList.remove(thinkingPosition);
                    chatAdapter.notifyItemRemoved(thinkingPosition);

                    // Thêm phản hồi từ Grok
                    messageList.add(new ChatMessage(response, false));
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);

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


                    btnSend.setEnabled(true);
                }
            });
        }
    }
}
