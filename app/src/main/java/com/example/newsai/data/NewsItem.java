package com.example.newsai.data;

import java.util.List;

public class NewsItem {
    private String _id;
    private String url;
    private String source_url;
    private String title;
    private String text_content;
    private String posted_at;
    private List<String> image_contents;
    private List<String> video_contents;
    private String author;
    private String description;
    private List<String> tags;
    private String crawled_at;
    private String type;
    private Integer content_length;
    private String sentiment_label;
    private String spam_label;
    public String get_id() { return _id; }
    public String getUrl() { return url; }
    public String getSource_url() { return source_url; }
    public String getTitle() { return title; }
    public String getText_content() { return text_content; }
    public String getPosted_at() { return posted_at; }
    public List<String> getImage_contents() { return image_contents; }
    public List<String> getVideo_contents() { return video_contents; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public List<String> getTags() { return tags; }
    public String getCrawled_at() { return crawled_at; }
    public String getType() { return type; }
    public Integer getContent_length() { return content_length; }
    public String getSentiment_label() { return sentiment_label; }
    public void setSentiment_label(String s) { this.sentiment_label = s; }
    public String getSpam_label() { return spam_label; }
    public void setSpam_label(String s) { this.spam_label = s; }

}