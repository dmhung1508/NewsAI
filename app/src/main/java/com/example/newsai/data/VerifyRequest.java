package com.example.newsai.data;

/**
 * Model class cho yêu cầu xác minh tin tức
 */
public class VerifyRequest {
    private String text;

    public VerifyRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}