package com.example.newsai.models;

import java.util.List;

public class SearchResponse {
    private String query;
    private int number_of_results;
    private List<SearchResult> results;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getNumber_of_results() {
        return number_of_results;
    }

    public void setNumber_of_results(int number_of_results) {
        this.number_of_results = number_of_results;
    }

    public List<SearchResult> getResults() {
        return results;
    }

    public void setResults(List<SearchResult> results) {
        this.results = results;
    }

    public static class SearchResult {
        private String title;
        private String url;
        private String content;
        private String engine;
        private double score;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getEngine() {
            return engine;
        }

        public void setEngine(String engine) {
            this.engine = engine;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }
    }
}

