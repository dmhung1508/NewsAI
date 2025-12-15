package com.example.newsai.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.List;

public class Comment {
    @DocumentId
    private String id;
    private String articleId;
    private String userId;
    private String userName;
    private String userAvatar;
    private String content;
    @ServerTimestamp
    private Timestamp timestamp;
    private int likeCount;
    private List<String> likedBy;
    private String parentCommentId; // null nếu là comment gốc, có giá trị nếu là reply
    private int replyCount;

    // Empty constructor for Firestore
    public Comment() {
        this.likedBy = new ArrayList<>();
        this.likeCount = 0;
        this.replyCount = 0;
    }

    public Comment(String articleId, String userId, String userName, String userAvatar, String content) {
        this.articleId = articleId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.content = content;
        this.likedBy = new ArrayList<>();
        this.likeCount = 0;
        this.replyCount = 0;
        this.parentCommentId = null;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public List<String> getLikedBy() {
        return likedBy;
    }

    public void setLikedBy(List<String> likedBy) {
        this.likedBy = likedBy;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }

    public boolean isLikedByUser(String userId) {
        return likedBy != null && likedBy.contains(userId);
    }

    public boolean isReply() {
        return parentCommentId != null && !parentCommentId.isEmpty();
    }

    // Helper method để format timestamp
    public String getTimeAgo() {
        if (timestamp == null)
            return "";

        long now = System.currentTimeMillis();
        long time = timestamp.toDate().getTime();
        long diff = now - time;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " ngày trước";
        } else if (hours > 0) {
            return hours + " giờ trước";
        } else if (minutes > 0) {
            return minutes + " phút trước";
        } else {
            return "Vừa xong";
        }
    }
}
