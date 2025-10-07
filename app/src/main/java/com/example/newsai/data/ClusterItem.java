package com.example.newsai.data;

import java.util.List;

public class ClusterItem {
    private String _id;
    private String cluster_id;
    private String title;
    private String summary;
    private int article_count;
    private List<String> article_ids;
    private List<String> links;
    private double score;
    private String primary_source;
    private List<String> image_contents;
    private String created_at;
    private String updated_at;

    // Getters
    public String get_id() { return _id; }
    public String getCluster_id() { return cluster_id; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public int getArticle_count() { return article_count; }
    public List<String> getArticle_ids() { return article_ids; }
    public List<String> getLinks() { return links; }
    public double getScore() { return score; }
    public String getPrimary_source() { return primary_source; }
    public List<String> getImage_contents() { return image_contents; }
    public String getCreated_at() { return created_at; }
    public String getUpdated_at() { return updated_at; }
}
