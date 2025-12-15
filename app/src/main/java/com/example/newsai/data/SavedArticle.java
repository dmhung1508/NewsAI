package com.example.newsai.data;

public class SavedArticle {
    private final String id;
    private final String title;
    private final String imageUrl;
    private final String url;
    private final String sourceUrl;
    private final String content;
    private final String date;
    private final String postedAt;
    private final String sentiment;
    private final String spam;

    public SavedArticle(String id,
                        String title,
                        String imageUrl,
                        String url,
                        String sourceUrl,
                        String content,
                        String date,
                        String postedAt,
                        String sentiment,
                        String spam) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.url = url;
        this.sourceUrl = sourceUrl;
        this.content = content;
        this.date = date;
        this.postedAt = postedAt;
        this.sentiment = sentiment;
        this.spam = spam;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getImageUrl() { return imageUrl; }
    public String getUrl() { return url; }
    public String getSourceUrl() { return sourceUrl; }
    public String getContent() { return content; }
    public String getDate() { return date; }
    public String getPostedAt() { return postedAt; }
    public String getSentiment() { return sentiment; }
    public String getSpam() { return spam; }
}

