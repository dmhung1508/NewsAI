package com.example.newsai.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.newsai.R;
import com.example.newsai.models.Comment;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments;
    private Context context;
    private OnCommentActionListener listener;
    private FirebaseUser currentUser;

    public interface OnCommentActionListener {
        void onLikeClick(Comment comment, int position);

        void onReplyClick(Comment comment);

        void onDeleteClick(Comment comment, int position);

        void onViewRepliesClick(Comment comment);
    }

    public CommentAdapter(Context context, OnCommentActionListener listener) {
        this.context = context;
        this.comments = new ArrayList<>();
        this.listener = listener;
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    public void addComment(Comment comment) {
        this.comments.add(0, comment);
        notifyItemInserted(0);
    }

    public void removeComment(int position) {
        if (position >= 0 && position < comments.size()) {
            comments.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void updateComment(int position, Comment comment) {
        if (position >= 0 && position < comments.size()) {
            comments.set(position, comment);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment, position);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvUserName, tvTime, tvContent, tvLikeCount, tvViewReplies;
        LinearLayout btnLike, btnReply, repliesContainer;
        ImageView ivLike;
        ImageButton btnMore;

        CommentViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvViewReplies = itemView.findViewById(R.id.tvViewReplies);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnReply = itemView.findViewById(R.id.btnReply);
            repliesContainer = itemView.findViewById(R.id.repliesContainer);
            ivLike = itemView.findViewById(R.id.ivLike);
            btnMore = itemView.findViewById(R.id.btnMore);
        }

        void bind(Comment comment, int position) {
            // User info
            tvUserName.setText(comment.getUserName());
            tvTime.setText(comment.getTimeAgo());
            tvContent.setText(comment.getContent());

            // Avatar
            if (comment.getUserAvatar() != null && !comment.getUserAvatar().isEmpty()) {
                Glide.with(context)
                        .load(comment.getUserAvatar())
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_avatar);
            }

            // Like count
            int likeCount = comment.getLikeCount();
            tvLikeCount.setText(String.valueOf(likeCount));

            // Like state
            boolean isLiked = currentUser != null && comment.isLikedByUser(currentUser.getUid());
            if (isLiked) {
                ivLike.setImageResource(R.drawable.ic_like_filled);
                ivLike.setColorFilter(ContextCompat.getColor(context, R.color.result_false_text));
                tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.result_false_text));
            } else {
                ivLike.setImageResource(R.drawable.ic_like_outline);
                ivLike.setColorFilter(ContextCompat.getColor(context, R.color.title_gray));
                tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.title_gray));
            }

            // Reply count
            if (comment.getReplyCount() > 0) {
                tvViewReplies.setVisibility(View.VISIBLE);
                tvViewReplies.setText("Xem " + comment.getReplyCount() + " câu trả lời");
            } else {
                tvViewReplies.setVisibility(View.GONE);
            }

            // Show delete button for owner
            if (currentUser != null && comment.getUserId().equals(currentUser.getUid())) {
                btnMore.setVisibility(View.VISIBLE);
            } else {
                btnMore.setVisibility(View.GONE);
            }

            // Click listeners
            btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(comment, position);
                }
            });

            btnReply.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReplyClick(comment);
                }
            });

            tvViewReplies.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewRepliesClick(comment);
                }
            });

            btnMore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(comment, position);
                }
            });
        }
    }
}
