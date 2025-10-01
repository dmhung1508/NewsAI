package com.example.newsai;

public class ChatMessage {
    private String content;
    private boolean isUser;
    private String[] suggestions;

    public ChatMessage(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
    }

    public String getContent() {
        return content;
    }

    public boolean isUser() {
        return isUser;
    }

    public String[] getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(String[] suggestions) {
        this.suggestions = suggestions;
    }

    public boolean hasSuggestions() {
        return suggestions != null && suggestions.length > 0;
    }
}

