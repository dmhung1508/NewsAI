package com.example.newsai.data;

import android.util.Log;

import com.example.newsai.models.Comment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentRepository {
    private static final String TAG = "CommentRepository";
    private static final String COLLECTION_COMMENTS = "comments";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public CommentRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // Interface callbacks
    public interface OnCommentsLoadedListener {
        void onSuccess(List<Comment> comments);

        void onError(String error);
    }

    public interface OnCommentActionListener {
        void onSuccess();

        void onError(String error);
    }

    public interface OnCommentAddedListener {
        void onSuccess(Comment comment);

        void onError(String error);
    }

    // Lấy tất cả comments cho một article (không cần index)
    public void getComments(String articleId, OnCommentsLoadedListener listener) {
        db.collection(COLLECTION_COMMENTS)
                .whereEqualTo("articleId", articleId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Comment> allComments = querySnapshot.toObjects(Comment.class);
                    // Filter: chỉ lấy comments gốc (parentCommentId == null)
                    List<Comment> rootComments = new ArrayList<>();
                    for (Comment c : allComments) {
                        if (c.getParentCommentId() == null || c.getParentCommentId().isEmpty()) {
                            rootComments.add(c);
                        }
                    }
                    // Sort: mới nhất lên đầu
                    Collections.sort(rootComments, (c1, c2) -> {
                        if (c1.getTimestamp() == null && c2.getTimestamp() == null)
                            return 0;
                        if (c1.getTimestamp() == null)
                            return 1;
                        if (c2.getTimestamp() == null)
                            return -1;
                        return c2.getTimestamp().compareTo(c1.getTimestamp());
                    });
                    listener.onSuccess(rootComments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting comments", e);
                    listener.onError(e.getMessage());
                });
    }

    // Lấy replies cho một comment (không cần index)
    public void getReplies(String parentCommentId, OnCommentsLoadedListener listener) {
        db.collection(COLLECTION_COMMENTS)
                .whereEqualTo("parentCommentId", parentCommentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Comment> replies = querySnapshot.toObjects(Comment.class);
                    // Sort: cũ nhất lên đầu
                    Collections.sort(replies, (c1, c2) -> {
                        if (c1.getTimestamp() == null && c2.getTimestamp() == null)
                            return 0;
                        if (c1.getTimestamp() == null)
                            return 1;
                        if (c2.getTimestamp() == null)
                            return -1;
                        return c1.getTimestamp().compareTo(c2.getTimestamp());
                    });
                    listener.onSuccess(replies);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting replies", e);
                    listener.onError(e.getMessage());
                });
    }

    // Thêm comment mới
    public void addComment(String articleId, String content, OnCommentAddedListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError("Vui lòng đăng nhập để bình luận");
            return;
        }

        Comment comment = new Comment(
                articleId,
                user.getUid(),
                user.getDisplayName() != null ? user.getDisplayName() : "Người dùng",
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null,
                content);

        db.collection(COLLECTION_COMMENTS)
                .add(comment)
                .addOnSuccessListener(documentReference -> {
                    comment.setId(documentReference.getId());
                    listener.onSuccess(comment);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding comment", e);
                    listener.onError(e.getMessage());
                });
    }

    // Thêm reply cho một comment
    public void addReply(String articleId, String parentCommentId, String content, OnCommentAddedListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError("Vui lòng đăng nhập để trả lời");
            return;
        }

        Comment reply = new Comment(
                articleId,
                user.getUid(),
                user.getDisplayName() != null ? user.getDisplayName() : "Người dùng",
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null,
                content);
        reply.setParentCommentId(parentCommentId);

        db.collection(COLLECTION_COMMENTS)
                .add(reply)
                .addOnSuccessListener(documentReference -> {
                    reply.setId(documentReference.getId());

                    // Tăng reply count của parent comment
                    db.collection(COLLECTION_COMMENTS)
                            .document(parentCommentId)
                            .update("replyCount", FieldValue.increment(1));

                    listener.onSuccess(reply);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding reply", e);
                    listener.onError(e.getMessage());
                });
    }

    // Like/Unlike comment
    public void toggleLike(String commentId, OnCommentActionListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError("Vui lòng đăng nhập để thích bình luận");
            return;
        }

        DocumentReference commentRef = db.collection(COLLECTION_COMMENTS).document(commentId);

        commentRef.get().addOnSuccessListener(documentSnapshot -> {
            Comment comment = documentSnapshot.toObject(Comment.class);
            if (comment == null) {
                listener.onError("Comment không tồn tại");
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            if (comment.isLikedByUser(user.getUid())) {
                // Unlike
                updates.put("likedBy", FieldValue.arrayRemove(user.getUid()));
                updates.put("likeCount", FieldValue.increment(-1));
            } else {
                // Like
                updates.put("likedBy", FieldValue.arrayUnion(user.getUid()));
                updates.put("likeCount", FieldValue.increment(1));
            }

            commentRef.update(updates)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        }).addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // Xóa comment (chỉ owner)
    public void deleteComment(String commentId, OnCommentActionListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError("Vui lòng đăng nhập");
            return;
        }

        DocumentReference commentRef = db.collection(COLLECTION_COMMENTS).document(commentId);

        commentRef.get().addOnSuccessListener(documentSnapshot -> {
            Comment comment = documentSnapshot.toObject(Comment.class);
            if (comment == null) {
                listener.onError("Comment không tồn tại");
                return;
            }

            if (!comment.getUserId().equals(user.getUid())) {
                listener.onError("Bạn không có quyền xóa bình luận này");
                return;
            }

            // Nếu là reply, giảm reply count của parent
            if (comment.isReply()) {
                db.collection(COLLECTION_COMMENTS)
                        .document(comment.getParentCommentId())
                        .update("replyCount", FieldValue.increment(-1));
            }

            commentRef.delete()
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        }).addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // Đếm số comments của article
    public void getCommentCount(String articleId, OnSuccessListener<Long> listener) {
        db.collection(COLLECTION_COMMENTS)
                .whereEqualTo("articleId", articleId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listener.onSuccess((long) querySnapshot.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error counting comments", e);
                    listener.onSuccess(0L);
                });
    }

    // Real-time listener cho comments (không cần index)
    public void listenToComments(String articleId, OnCommentsLoadedListener listener) {
        db.collection(COLLECTION_COMMENTS)
                .whereEqualTo("articleId", articleId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (querySnapshot != null) {
                        List<Comment> allComments = querySnapshot.toObjects(Comment.class);
                        // Filter: chỉ lấy comments gốc
                        List<Comment> rootComments = new ArrayList<>();
                        for (Comment c : allComments) {
                            if (c.getParentCommentId() == null || c.getParentCommentId().isEmpty()) {
                                rootComments.add(c);
                            }
                        }
                        // Sort: mới nhất lên đầu
                        Collections.sort(rootComments, (c1, c2) -> {
                            if (c1.getTimestamp() == null && c2.getTimestamp() == null)
                                return 0;
                            if (c1.getTimestamp() == null)
                                return 1;
                            if (c2.getTimestamp() == null)
                                return -1;
                            return c2.getTimestamp().compareTo(c1.getTimestamp());
                        });
                        listener.onSuccess(rootComments);
                    }
                });
    }
}
