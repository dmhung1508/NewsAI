package com.example.newsai.models;

import java.util.List;

public class VerifyResponse {
    private List<FactItem> fact_list;
    private boolean legit;
    private String status;
    private long request_id;
    private String summary;

    public static class FactItem {
        private String fact;
        private List<String> sources;
        private boolean verify;

        public String getFact() {
            return fact;
        }

        public void setFact(String fact) {
            this.fact = fact;
        }

        public List<String> getSources() {
            return sources;
        }

        public void setSources(List<String> sources) {
            this.sources = sources;
        }

        public boolean isVerify() {
            return verify;
        }

        public void setVerify(boolean verify) {
            this.verify = verify;
        }
    }

    public List<FactItem> getFact_list() {
        return fact_list;
    }

    public void setFact_list(List<FactItem> fact_list) {
        this.fact_list = fact_list;
    }

    public boolean isLegit() {
        return legit;
    }

    public void setLegit(boolean legit) {
        this.legit = legit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getRequest_id() {
        return request_id;
    }

    public void setRequest_id(long request_id) {
        this.request_id = request_id;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
