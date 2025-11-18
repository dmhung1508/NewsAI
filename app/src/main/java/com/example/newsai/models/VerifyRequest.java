package com.example.newsai.models;

public class VerifyRequest {
    private String article;

    public VerifyRequest(String article) {
        this.article = article;
    }

    public String getArticle() {
        return article;
    }

    public void setArticle(String article) {
        this.article = article;
    }
}
