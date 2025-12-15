package com.example.newsai.data;

/**
 * Model class cho kết quả xác minh tin tức
 */
public class VerifyResponse {
    private boolean isTrue;
    private String description;
    private double confidence;
    private String source;

    // Constructors
    public VerifyResponse() {}

    public VerifyResponse(boolean isTrue, String description, double confidence, String source) {
        this.isTrue = isTrue;
        this.description = description;
        this.confidence = confidence;
        this.source = source;
    }

    // Getters
    public boolean isTrue() {
        return isTrue;
    }

    public String getDescription() {
        return description;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getSource() {
        return source;
    }

    // Setters
    public void setTrue(boolean aTrue) {
        isTrue = aTrue;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setSource(String source) {
        this.source = source;
    }
}