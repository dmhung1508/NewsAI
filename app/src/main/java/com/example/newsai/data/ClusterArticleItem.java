package com.example.newsai.data;

public class ClusterArticleItem {
    private String _id;
    private String cluster_id;
    private String article_id;
    private String title;
    private String text;
    private String link;
    private String source;
    private int rank;
    private String created_at;

    // Getters
    public String get_id() { return _id; }
    public String getCluster_id() { return cluster_id; }
    public String getArticle_id() { return article_id; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public String getLink() { return link; }
    public String getSource() { return source; }
    public int getRank() { return rank; }
    public String getCreated_at() { return created_at; }
}
