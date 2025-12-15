package com.example.newsai.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.newsai.R;
import com.example.newsai.data.CommentRepository;
import com.example.newsai.models.Comment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class CommentBottomSheet extends BottomSheetDialogFragment implements CommentAdapter.OnCommentActionListener {

    private static final String ARG_ARTICLE_ID = "article_id";

    private String articleId;
    private CommentRepository commentRepository;
    private CommentAdapter adapter;

    // Views
    private RecyclerView rvComments;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private EditText etComment;
    private ImageButton btnSend, btnClose, btnCancelReply;
    private TextView tvCommentCount, tvReplyingTo;
    private LinearLayout replyIndicator;
    private ShapeableImageView ivUserAvatar;

    // Reply state
    private String replyToCommentId = null;
    private String replyToUserName = null;

    public static CommentBottomSheet newInstance(String articleId) {
        CommentBottomSheet fragment = new CommentBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ARTICLE_ID, articleId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            articleId = getArguments().getString(ARG_ARTICLE_ID);
        }
        commentRepository = new CommentRepository();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);

                // Set height to 70% of screen
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                bottomSheet.getLayoutParams().height = (int) (screenHeight * 0.7);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadUserAvatar();
        loadComments();
    }

    private void initViews(View view) {
        rvComments = view.findViewById(R.id.rvComments);
        emptyState = view.findViewById(R.id.emptyState);
        progressBar = view.findViewById(R.id.progressBar);
        etComment = view.findViewById(R.id.etComment);
        btnSend = view.findViewById(R.id.btnSend);
        btnClose = view.findViewById(R.id.btnClose);
        tvCommentCount = view.findViewById(R.id.tvCommentCount);
        replyIndicator = view.findViewById(R.id.replyIndicator);
        tvReplyingTo = view.findViewById(R.id.tvReplyingTo);
        btnCancelReply = view.findViewById(R.id.btnCancelReply);
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar);
    }

    private void setupRecyclerView() {
        adapter = new CommentAdapter(requireContext(), this);
        rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComments.setAdapter(adapter);
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> dismiss());

        btnSend.setOnClickListener(v -> sendComment());

        btnCancelReply.setOnClickListener(v -> cancelReply());
    }

    private void loadUserAvatar() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(ivUserAvatar);
        }
    }

    private void loadComments() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        rvComments.setVisibility(View.GONE);

        commentRepository.getComments(articleId, new CommentRepository.OnCommentsLoadedListener() {
            @Override
            public void onSuccess(List<Comment> comments) {
                progressBar.setVisibility(View.GONE);

                if (comments.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    rvComments.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    rvComments.setVisibility(View.VISIBLE);
                    adapter.setComments(comments);
                }

                updateCommentCount(comments.size());
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendComment() {
        String content = etComment.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(getContext(), "Vui lòng nhập nội dung bình luận", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);

        if (replyToCommentId != null) {
            // Reply mode
            commentRepository.addReply(articleId, replyToCommentId, content,
                    new CommentRepository.OnCommentAddedListener() {
                        @Override
                        public void onSuccess(Comment comment) {
                            btnSend.setEnabled(true);
                            etComment.setText("");
                            cancelReply();
                            Toast.makeText(getContext(), "Đã trả lời", Toast.LENGTH_SHORT).show();
                            loadComments(); // Reload to update reply count
                        }

                        @Override
                        public void onError(String error) {
                            btnSend.setEnabled(true);
                            Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // New comment mode
            commentRepository.addComment(articleId, content, new CommentRepository.OnCommentAddedListener() {
                @Override
                public void onSuccess(Comment comment) {
                    btnSend.setEnabled(true);
                    etComment.setText("");
                    adapter.addComment(comment);
                    rvComments.scrollToPosition(0);
                    emptyState.setVisibility(View.GONE);
                    rvComments.setVisibility(View.VISIBLE);
                    updateCommentCount(adapter.getItemCount());
                }

                @Override
                public void onError(String error) {
                    btnSend.setEnabled(true);
                    Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void cancelReply() {
        replyToCommentId = null;
        replyToUserName = null;
        replyIndicator.setVisibility(View.GONE);
        etComment.setHint("Viết bình luận...");
    }

    private void updateCommentCount(int count) {
        tvCommentCount.setText(count + " bình luận");
    }

    // CommentAdapter.OnCommentActionListener implementations

    @Override
    public void onLikeClick(Comment comment, int position) {
        commentRepository.toggleLike(comment.getId(), new CommentRepository.OnCommentActionListener() {
            @Override
            public void onSuccess() {
                // Reload the specific comment to update UI
                loadComments();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onReplyClick(Comment comment) {
        replyToCommentId = comment.getId();
        replyToUserName = comment.getUserName();
        replyIndicator.setVisibility(View.VISIBLE);
        tvReplyingTo.setText("@" + replyToUserName);
        etComment.setHint("Trả lời " + replyToUserName + "...");
        etComment.requestFocus();
    }

    @Override
    public void onDeleteClick(Comment comment, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa bình luận")
                .setMessage("Bạn có chắc muốn xóa bình luận này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    commentRepository.deleteComment(comment.getId(), new CommentRepository.OnCommentActionListener() {
                        @Override
                        public void onSuccess() {
                            adapter.removeComment(position);
                            updateCommentCount(adapter.getItemCount());
                            if (adapter.getItemCount() == 0) {
                                emptyState.setVisibility(View.VISIBLE);
                                rvComments.setVisibility(View.GONE);
                            }
                            Toast.makeText(getContext(), "Đã xóa bình luận", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onViewRepliesClick(Comment comment) {
        // TODO: Show replies in a nested view or new bottom sheet
        Toast.makeText(getContext(), "Xem " + comment.getReplyCount() + " câu trả lời của " + comment.getUserName(),
                Toast.LENGTH_SHORT).show();
    }
}
