package com.example.newsai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;
    private List<ChatMessage> messageList;
    private Context context;
    private OnSuggestionClickListener suggestionClickListener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);
    }

    public ChatAdapter(List<ChatMessage> messageList, Context context) {
        this.messageList = messageList;
        this.context = context;
    }

    public void setOnSuggestionClickListener(OnSuggestionClickListener listener) {
        this.suggestionClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_bot, parent, false);
            return new BotMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else {
            ((BotMessageViewHolder) holder).bind(message, suggestionClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        UserMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvUserMessage);
        }

        void bind(ChatMessage message) {
            tvMessage.setText(message.getContent());
        }
    }

    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        LinearLayout suggestionsLayout;

        BotMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvBotMessage);
            suggestionsLayout = itemView.findViewById(R.id.suggestionsLayout);
        }

        void bind(ChatMessage message, OnSuggestionClickListener listener) {
            tvMessage.setText(message.getContent());

            if (message.hasSuggestions()) {
                suggestionsLayout.setVisibility(View.VISIBLE);
                suggestionsLayout.removeAllViews();

                for (String suggestion : message.getSuggestions()) {
                    MaterialButton btnSuggestion = new MaterialButton(itemView.getContext(), null,
                            com.google.android.material.R.attr.materialButtonOutlinedStyle);
                    btnSuggestion.setText(suggestion);
                    btnSuggestion.setTextColor(itemView.getContext().getColor(R.color.blue_primary));
                    btnSuggestion.setStrokeColor(itemView.getContext().getColorStateList(R.color.blue_primary));
                    btnSuggestion.setCornerRadius(50);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 8, 0, 8);
                    btnSuggestion.setLayoutParams(params);

                    btnSuggestion.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onSuggestionClick(suggestion);
                        }
                    });

                    suggestionsLayout.addView(btnSuggestion);
                }
            } else {
                suggestionsLayout.setVisibility(View.GONE);
            }
        }
    }
}

