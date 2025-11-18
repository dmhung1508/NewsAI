package com.example.newsai.models;

public class VerifyRequest {
    private String user_input;
    private String related_info;

    public VerifyRequest(String user_input, String related_info) {
        this.user_input = user_input;
        this.related_info = related_info;
    }

    public String getUser_input() {
        return user_input;
    }

    public void setUser_input(String user_input) {
        this.user_input = user_input;
    }

    public String getRelated_info() {
        return related_info;
    }

    public void setRelated_info(String related_info) {
        this.related_info = related_info;
    }
}