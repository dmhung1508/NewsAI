package com.example.newsai.models;

public class SearchRequest {
    private String query;
    private int pageno;
    private String categories;
    private String engines;
    private String language;
    private int safesearch;

    // Constructor with default values
    public SearchRequest(String query) {
        this.query = query;
        this.pageno = 1;
        this.categories = "";
        this.engines = "";
        this.language = "vi";
        this.safesearch = 1;
    }

    // Full constructor
    public SearchRequest(String query, int pageno, String categories, String engines, String language, int safesearch) {
        this.query = query;
        this.pageno = pageno;
        this.categories = categories;
        this.engines = engines;
        this.language = language;
        this.safesearch = safesearch;
    }

    // Getters and Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getPageno() {
        return pageno;
    }

    public void setPageno(int pageno) {
        this.pageno = pageno;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public String getEngines() {
        return engines;
    }

    public void setEngines(String engines) {
        this.engines = engines;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getSafesearch() {
        return safesearch;
    }

    public void setSafesearch(int safesearch) {
        this.safesearch = safesearch;
    }
}
