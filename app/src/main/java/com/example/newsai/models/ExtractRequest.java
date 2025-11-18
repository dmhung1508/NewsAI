package com.example.newsai.models;

public class ExtractRequest {
    private String url;
    private boolean use_proxy;

    // Constructor with default use_proxy = false
    public ExtractRequest(String url) {
        this.url = url;
        this.use_proxy = false;
    }

    // Full constructor
    public ExtractRequest(String url, boolean use_proxy) {
        this.url = url;
        this.use_proxy = use_proxy;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isUse_proxy() {
        return use_proxy;
    }

    public void setUse_proxy(boolean use_proxy) {
        this.use_proxy = use_proxy;
    }
}
