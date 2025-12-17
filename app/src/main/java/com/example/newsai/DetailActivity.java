package com.example.newsai;

import android.content.Intent;
import android.graphics.text.LineBreaker; // API 29+
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.PrecomputedTextCompat;
import androidx.core.widget.TextViewCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.bumptech.glide.Glide;
import com.example.newsai.data.BookmarkStorage;
import com.example.newsai.data.NewsItem;
import com.example.newsai.data.SavedArticle;
import com.example.newsai.network.ApiClient;
import com.example.newsai.network.ApiService;
import com.example.newsai.tts.TextToSpeechManager;
import com.example.newsai.data.CommentRepository;
import com.example.newsai.ui.CommentBottomSheet;
import com.example.newsai.models.Comment;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity {

    // Keys
    public static final String K_TITLE = "k_title";
    public static final String K_IMAGE = "k_image";
    public static final String K_URL = "k_url";
    public static final String K_SOURCE_URL = "k_source_url";
    public static final String K_CONTENT = "k_content";
    public static final String K_DATE = "k_date"; // crawled_at
    public static final String K_POSTED = "k_posted"; // posted_at
    public static final String K_SENTIMENT = "k_sentiment";
    public static final String K_SPAM = "k_spam";
    public static final String K_ID = "article_id";

    private static final String TAG = "DETAIL";

    // Views
    private ImageView imgHeader, ivSentiment, ivSpam;
    private TextView tvCaption, tvTitle, tvLede, tvMeta, tvContent, tvSourceLink, btnShare, btnBookmark, badgeReadTime;
    private android.widget.FrameLayout btnChatbot;
    private SavedArticle currentSavedArticle;
    private android.widget.FrameLayout btnTTS;
    private ProgressBar progressTTS;
    private LinearLayout contentContainer;
    private TextToSpeechManager ttsManager;
    private String currentTitle;
    private String currentContent;
    private String currentUrl;
    private boolean isAutoPlaying;
    private int currentParagraphIndex;

    // Comment feature
    private android.widget.FrameLayout btnComment;
    private TextView tvCommentBadge;
    private CommentRepository commentRepository;
    private String currentArticleId;

    // Inline comments
    private LinearLayout commentsContainer;
    private LinearLayout emptyCommentsState;
    private EditText etInlineComment;
    private ImageButton btnInlineSend;
    private ShapeableImageView ivCommentAvatar;
    private TextView tvInlineCommentCount;
    private TextView tvLoadMoreComments;
    private List<Comment> loadedComments = new java.util.ArrayList<>();

    // Related Articles
    private LinearLayout relatedArticlesSection;
    private androidx.recyclerview.widget.RecyclerView rvRelatedArticles;
    private com.example.newsai.ui.RelatedArticlesAdapter relatedAdapter;

    // FAB Menu
    private FloatingActionButton fabMain;
    private LinearLayout menuContainer;
    private boolean isMenuExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ttsManager = new TextToSpeechManager(this);
        setupTTSListener();
        bindViews();

        Intent it = getIntent();
        String articleId = it.getStringExtra(K_ID);
        if (!TextUtils.isEmpty(articleId)) {
            fetchArticleById(articleId);
            return;
        }

        // Nhận dữ liệu trực tiếp
        displayArticle(
                it.getStringExtra(K_TITLE),
                it.getStringExtra(K_IMAGE),
                it.getStringExtra(K_URL),
                it.getStringExtra(K_SOURCE_URL),
                it.getStringExtra(K_CONTENT),
                it.getStringExtra(K_DATE),
                it.getStringExtra(K_POSTED),
                it.getStringExtra(K_SENTIMENT),
                it.getStringExtra(K_SPAM));
    }

    private void bindViews() {
        imgHeader = findViewById(R.id.imgHeader);
        tvCaption = findViewById(R.id.tvCaption);
        tvTitle = findViewById(R.id.tvTitle);
        tvLede = findViewById(R.id.tvLede);
        tvMeta = findViewById(R.id.tvMeta);
        tvContent = findViewById(R.id.tvContent);
        tvSourceLink = findViewById(R.id.tvSourceLink);
        btnShare = findViewById(R.id.btnShare);
        btnBookmark = findViewById(R.id.btnBookmark);
        badgeReadTime = findViewById(R.id.badgeReadTime);
        ivSentiment = findViewById(R.id.ivSentimentDetail);
        ivSpam = findViewById(R.id.ivSpamDetail);
        btnChatbot = findViewById(R.id.btnChatbot);
        btnTTS = findViewById(R.id.btnTTS);
        progressTTS = findViewById(R.id.progressTTS);
        // Get parent LinearLayout containing tvContent
        android.view.ViewParent parent = tvContent.getParent();
        if (parent instanceof LinearLayout) {
            contentContainer = (LinearLayout) parent;
        }

        // Justify: API29+ dùng LineBreaker, API26–28 dùng Layout
        if (Build.VERSION.SDK_INT >= 29) {
            tvContent.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
        } else if (Build.VERSION.SDK_INT >= 26) {
            tvContent.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
        }

        // TTS Button
        btnTTS = findViewById(R.id.btnTTS);
        if (btnTTS != null) {
            btnTTS.setOnClickListener(v -> {
                toggleMenu(); // Auto-collapse
                toggleTTSPlayback();
            });
        }

        // Comment Button
        btnComment = findViewById(R.id.btnComment);
        tvCommentBadge = findViewById(R.id.tvCommentBadge);
        commentRepository = new CommentRepository();

        if (btnComment != null) {
            btnComment.setOnClickListener(v -> {
                toggleMenu(); // Auto-collapse
                scrollToComments();
            });
        }

        // Inline comments
        commentsContainer = findViewById(R.id.commentsContainer);
        emptyCommentsState = findViewById(R.id.emptyCommentsState);
        etInlineComment = findViewById(R.id.etInlineComment);
        btnInlineSend = findViewById(R.id.btnInlineSend);
        ivCommentAvatar = findViewById(R.id.ivCommentAvatar);
        tvInlineCommentCount = findViewById(R.id.tvInlineCommentCount);
        tvLoadMoreComments = findViewById(R.id.tvLoadMoreComments);

        // Setup send button
        if (btnInlineSend != null) {
            btnInlineSend.setOnClickListener(v -> sendInlineComment());
        }

        // Load user avatar
        loadCommentUserAvatar();

        // FAB Menu Setup
        fabMain = findViewById(R.id.fabMain);
        menuContainer = findViewById(R.id.menuContainer);

        if (fabMain != null) {
            fabMain.setAlpha(0.3f); // Lower alpha for inactive state
            fabMain.setImageResource(R.drawable.ic_white_circle); // White circle icon
            fabMain.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF555555)); // Grey
            fabMain.setOnClickListener(v -> toggleMenu());
        }

        // Related Articles Setup
        relatedArticlesSection = findViewById(R.id.relatedArticlesSection);
        rvRelatedArticles = findViewById(R.id.rvRelatedArticles);
        setupRelatedArticles();
    }

    private void fetchArticleById(String articleId) {
        ApiService api = ApiClient.get().create(ApiService.class);
        api.getArticleById(articleId).enqueue(new Callback<NewsItem>() {
            @Override
            public void onResponse(Call<NewsItem> call, Response<NewsItem> res) {
                if (res.isSuccessful() && res.body() != null) {
                    NewsItem a = res.body();
                    String imgUrl = (a.getImage_contents() != null && !a.getImage_contents().isEmpty())
                            ? a.getImage_contents().get(0)
                            : null;

                    displayArticle(
                            a.getTitle(),
                            imgUrl,
                            a.getUrl(),
                            a.getSource_url(),
                            a.getText_content(),
                            a.getCrawled_at(),
                            a.getPosted_at(),
                            a.getSentiment_label(),
                            a.getSpam_label());
                } else {
                    Toast.makeText(DetailActivity.this, "Không tìm thấy bài viết", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<NewsItem> call, Throwable t) {
                Log.e(TAG, "API FAIL", t);
                Toast.makeText(DetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayArticle(String title, String image, String url, String sourceUrl,
            String content, String crawledAt, String postedAt,
            String sentiment, String spam) {

        // Header + caption
        Glide.with(imgHeader).load(image)
                .placeholder(R.drawable.hotnews)
                .error(R.drawable.hotnews)
                .centerCrop()
                .into(imgHeader);
        tvCaption.setText(domain(!TextUtils.isEmpty(sourceUrl) ? sourceUrl : url));

        // Title + lede
        String sanitizedTitle = sanitizeTitle(title);
        tvTitle.setText(safe(sanitizedTitle));
        String lede = makeLede(content);
        tvLede.setText(lede);

        // Meta ngày (crawled_at)
        tvMeta.setText(safe(formatDate(crawledAt)));

        // Store for TTS and Chatbot
        currentTitle = sanitizedTitle;
        currentContent = content;
        currentUrl = url;

        // Hiển thị nội dung với các nút play cho từng đoạn
        setupContentWithTTS(content);

        // Link nguồn
        tvSourceLink.setText(url != null ? url : "");
        tvSourceLink.setOnClickListener(v -> openUrl(url));
        tvTitle.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(url))
                openUrl(url);
        });
        imgHeader.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(url))
                openUrl(url);
        });

        // Share / Bookmark
        btnShare.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, title);
            share.putExtra(Intent.EXTRA_TEXT, (title == null ? "" : title) + "\n" + (url == null ? "" : url));
            startActivity(Intent.createChooser(share, "Chia sẻ bài viết"));
        });
        btnBookmark.setOnClickListener(v -> toggleBookmark());

        // Icons
        if (ivSentiment != null) {
            ivSentiment.setImageResource(mapSentiment(sentiment));
            ivSentiment.setOnClickListener(v -> showTooltip(v, getSentimentText(sentiment), -50));
        }
        if (ivSpam != null) {
            ivSpam.setImageResource(mapSpam(spam));
            ivSpam.setOnClickListener(v -> showTooltip(v, getSpamText(spam), 0));
        }

        // Badge thời gian: now - posted_at (fallback crawled_at)
        String baseTime = !TextUtils.isEmpty(postedAt) ? postedAt : crawledAt;
        Log.d(TAG, "postedAt=" + postedAt + " | crawledAt=" + crawledAt + " | base=" + baseTime);
        if (badgeReadTime != null) {
            String timeAgo = timeAgoVi(baseTime);
            badgeReadTime.setText(!TextUtils.isEmpty(timeAgo) ? timeAgo : "—");
        }

        setupChatbotButton(sanitizedTitle, content, url);
        currentSavedArticle = new SavedArticle(
                url != null ? url : title,
                sanitizedTitle,
                image,
                url,
                sourceUrl,
                content,
                crawledAt,
                postedAt,
                sentiment,
                spam);
        updateBookmarkState();

        // Set article ID for comments (use URL hash as unique ID)
        String idSource = url;
        if (TextUtils.isEmpty(idSource))
            idSource = title;
        if (TextUtils.isEmpty(idSource))
            idSource = content;
        if (TextUtils.isEmpty(idSource))
            idSource = "unknown_" + System.currentTimeMillis();

        currentArticleId = String.valueOf(idSource.hashCode());
        loadCommentCount();
        loadInlineComments();
        loadRelatedArticles();
    }

    // ===== Comment Methods =====
    private void scrollToComments() {
        if (commentsContainer != null) {
            commentsContainer.getParent().requestChildFocus(commentsContainer, commentsContainer);
        }
    }

    private void loadCommentUserAvatar() {
        // Load avatar from Firestore profile (supports custom uploaded avatar)
        com.example.newsai.data.UserProfileManager userProfileManager = new com.example.newsai.data.UserProfileManager();
        userProfileManager.loadProfile(new com.example.newsai.data.UserProfileManager.OnProfileLoadedListener() {
            @Override
            public void onSuccess(com.example.newsai.data.UserProfileManager.UserProfile profile) {
                runOnUiThread(() -> {
                    if (ivCommentAvatar == null)
                        return;

                    String photoUrl = profile.getPhotoUrl();
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(DetailActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_avatar)
                                .error(R.drawable.ic_avatar)
                                .circleCrop()
                                .into(ivCommentAvatar);
                    } else {
                        // Fallback to Firebase Auth
                        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                                .getCurrentUser();
                        if (user != null && user.getPhotoUrl() != null) {
                            Glide.with(DetailActivity.this)
                                    .load(user.getPhotoUrl())
                                    .placeholder(R.drawable.ic_avatar)
                                    .error(R.drawable.ic_avatar)
                                    .circleCrop()
                                    .into(ivCommentAvatar);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                // Fallback to Firebase Auth
                runOnUiThread(() -> {
                    com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                            .getCurrentUser();
                    if (user != null && user.getPhotoUrl() != null && ivCommentAvatar != null) {
                        Glide.with(DetailActivity.this)
                                .load(user.getPhotoUrl())
                                .placeholder(R.drawable.ic_avatar)
                                .error(R.drawable.ic_avatar)
                                .circleCrop()
                                .into(ivCommentAvatar);
                    }
                });
            }
        });
    }

    private void loadCommentCount() {
        if (commentRepository == null || currentArticleId == null)
            return;

        commentRepository.getCommentCount(currentArticleId, count -> {
            runOnUiThread(() -> {
                // Update badge
                if (tvCommentBadge != null) {
                    if (count > 0) {
                        tvCommentBadge.setVisibility(android.view.View.VISIBLE);
                        tvCommentBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    } else {
                        tvCommentBadge.setVisibility(android.view.View.GONE);
                    }
                }
                // Update inline count
                if (tvInlineCommentCount != null) {
                    tvInlineCommentCount.setText(String.valueOf(count));
                }
            });
        });
    }

    private void loadInlineComments() {
        if (commentRepository == null || currentArticleId == null || commentsContainer == null)
            return;

        commentRepository.getComments(currentArticleId, new CommentRepository.OnCommentsLoadedListener() {
            @Override
            public void onSuccess(java.util.List<Comment> comments) {
                runOnUiThread(() -> {
                    loadedComments.clear();
                    loadedComments.addAll(comments);
                    displayInlineComments(comments);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (emptyCommentsState != null) {
                        emptyCommentsState.setVisibility(android.view.View.VISIBLE);
                    }
                });
            }
        });
    }

    private void displayInlineComments(java.util.List<Comment> comments) {
        if (commentsContainer == null)
            return;

        commentsContainer.removeAllViews();

        if (comments.isEmpty()) {
            if (emptyCommentsState != null) {
                emptyCommentsState.setVisibility(android.view.View.VISIBLE);
            }
            return;
        }

        if (emptyCommentsState != null) {
            emptyCommentsState.setVisibility(android.view.View.GONE);
        }

        // Show max 5 comments initially
        int maxShow = Math.min(comments.size(), 5);
        for (int i = 0; i < maxShow; i++) {
            Comment comment = comments.get(i);
            android.view.View commentView = createCommentView(comment);
            commentsContainer.addView(commentView);
        }

        // Show "Load more" if there are more comments
        if (comments.size() > 5 && tvLoadMoreComments != null) {
            tvLoadMoreComments.setVisibility(android.view.View.VISIBLE);
            tvLoadMoreComments.setText("Xem thêm " + (comments.size() - 5) + " bình luận...");
            tvLoadMoreComments.setOnClickListener(v -> showAllComments());
        } else if (tvLoadMoreComments != null) {
            tvLoadMoreComments.setVisibility(android.view.View.GONE);
        }
    }

    private void showAllComments() {
        if (commentsContainer == null)
            return;

        commentsContainer.removeAllViews();
        for (Comment comment : loadedComments) {
            android.view.View commentView = createCommentView(comment);
            commentsContainer.addView(commentView);
        }

        if (tvLoadMoreComments != null) {
            tvLoadMoreComments.setVisibility(android.view.View.GONE);
        }
    }

    private android.view.View createCommentView(Comment comment) {
        android.view.View view = getLayoutInflater().inflate(R.layout.item_comment, commentsContainer, false);

        ShapeableImageView ivAvatar = view.findViewById(R.id.ivAvatar);
        TextView tvUserName = view.findViewById(R.id.tvUserName);
        TextView tvTime = view.findViewById(R.id.tvTime);
        TextView tvContent = view.findViewById(R.id.tvContent);
        TextView tvLikeCount = view.findViewById(R.id.tvLikeCount);
        ImageView ivLike = view.findViewById(R.id.ivLike);
        LinearLayout btnLike = view.findViewById(R.id.btnLike);
        ImageButton btnMore = view.findViewById(R.id.btnMore);

        // Set data
        tvUserName.setText(comment.getUserName());
        tvTime.setText(comment.getTimeAgo());
        tvContent.setText(comment.getContent());
        tvLikeCount.setText(String.valueOf(comment.getLikeCount()));

        // Load avatar - for current user, load fresh from Firestore
        com.google.firebase.auth.FirebaseUser currentUserForAvatar = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        boolean isCurrentUserComment = currentUserForAvatar != null
                && currentUserForAvatar.getUid().equals(comment.getUserId());

        if (isCurrentUserComment) {
            // Load fresh avatar from Firestore for current user's comments
            com.example.newsai.data.UserProfileManager profileManager = new com.example.newsai.data.UserProfileManager();
            profileManager.loadProfile(new com.example.newsai.data.UserProfileManager.OnProfileLoadedListener() {
                @Override
                public void onSuccess(com.example.newsai.data.UserProfileManager.UserProfile profile) {
                    runOnUiThread(() -> {
                        String photoUrl = profile.getPhotoUrl();
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(DetailActivity.this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.ic_avatar)
                                    .error(R.drawable.ic_avatar)
                                    .circleCrop()
                                    .into(ivAvatar);
                        } else if (comment.getUserAvatar() != null && !comment.getUserAvatar().isEmpty()) {
                            Glide.with(DetailActivity.this)
                                    .load(comment.getUserAvatar())
                                    .placeholder(R.drawable.ic_avatar)
                                    .error(R.drawable.ic_avatar)
                                    .circleCrop()
                                    .into(ivAvatar);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    // Fallback to stored avatar
                    if (comment.getUserAvatar() != null && !comment.getUserAvatar().isEmpty()) {
                        Glide.with(DetailActivity.this)
                                .load(comment.getUserAvatar())
                                .placeholder(R.drawable.ic_avatar)
                                .error(R.drawable.ic_avatar)
                                .circleCrop()
                                .into(ivAvatar);
                    }
                }
            });
        } else {
            // For other users, use stored avatar
            if (comment.getUserAvatar() != null && !comment.getUserAvatar().isEmpty()) {
                Glide.with(this)
                        .load(comment.getUserAvatar())
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .circleCrop()
                        .into(ivAvatar);
            }
        }

        // Like state
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        boolean isLiked = currentUser != null && comment.isLikedByUser(currentUser.getUid());
        if (isLiked) {
            ivLike.setImageResource(R.drawable.ic_like_filled);
            ivLike.setColorFilter(getColor(R.color.result_false_text));
        } else {
            ivLike.setImageResource(R.drawable.ic_like_outline);
            ivLike.setColorFilter(getColor(R.color.title_gray));
        }

        // Like click
        btnLike.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }
            commentRepository.toggleLike(comment.getId(), new CommentRepository.OnCommentActionListener() {
                @Override
                public void onSuccess() {
                    loadInlineComments();
                    loadCommentCount();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(DetailActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Delete button (only show for comment owner)
        if (currentUser != null && comment.getUserId().equals(currentUser.getUid())) {
            btnMore.setVisibility(android.view.View.VISIBLE);
            btnMore.setOnClickListener(v -> deleteComment(comment));
        } else {
            btnMore.setVisibility(android.view.View.GONE);
        }

        // Hide reply-related views for inline display
        view.findViewById(R.id.btnReply).setVisibility(android.view.View.GONE);
        view.findViewById(R.id.tvViewReplies).setVisibility(android.view.View.GONE);

        return view;
    }

    private void sendInlineComment() {
        if (etInlineComment == null)
            return;

        String content = etInlineComment.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung bình luận", Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnInlineSend != null)
            btnInlineSend.setEnabled(false);

        commentRepository.addComment(currentArticleId, content, new CommentRepository.OnCommentAddedListener() {
            @Override
            public void onSuccess(Comment comment) {
                runOnUiThread(() -> {
                    if (btnInlineSend != null)
                        btnInlineSend.setEnabled(true);
                    etInlineComment.setText("");

                    // Hide keyboard
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                            INPUT_METHOD_SERVICE);
                    if (imm != null && etInlineComment != null) {
                        imm.hideSoftInputFromWindow(etInlineComment.getWindowToken(), 0);
                    }

                    Toast.makeText(DetailActivity.this, "Đã đăng bình luận", Toast.LENGTH_SHORT).show();
                    loadInlineComments();
                    loadCommentCount();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (btnInlineSend != null)
                        btnInlineSend.setEnabled(true);
                    Toast.makeText(DetailActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void deleteComment(Comment comment) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Xóa bình luận")
                .setMessage("Bạn có chắc muốn xóa bình luận này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    commentRepository.deleteComment(comment.getId(), new CommentRepository.OnCommentActionListener() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                Toast.makeText(DetailActivity.this, "Đã xóa bình luận", Toast.LENGTH_SHORT).show();
                                loadInlineComments();
                                loadCommentCount();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(DetailActivity.this, error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload comments when returning
        loadCommentCount();
        loadInlineComments();
    }

    // ===== Helpers =====
    private void openUrl(String u) {
        if (TextUtils.isEmpty(u)) {
            Toast.makeText(this, "Không có đường dẫn bài gốc", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String makeLede(String content) {
        if (content == null)
            return "";
        String c = content.trim();
        if (c.length() > 500)
            c = c.substring(0, 500) + "…";
        return c;
    }

    private void setupChatbotButton(String title, String content, String url) {
        if (btnChatbot == null)
            return;
        btnChatbot.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, ChatbotActivity.class);
            String safeTitle = safe(title);
            intent.putExtra(ChatbotActivity.EXTRA_ARTICLE_TITLE, safeTitle);
            intent.putExtra(ChatbotActivity.EXTRA_ARTICLE_URL, url);
            intent.putExtra(ChatbotActivity.EXTRA_HISTORY_KEY, buildHistoryKey(url, safeTitle));
            startActivity(intent);
        });
    }

    private void toggleBookmark() {
        if (currentSavedArticle == null) {
            Toast.makeText(this, "Không có dữ liệu bài viết để lưu", Toast.LENGTH_SHORT).show();
            return;
        }
        String key = getBookmarkKey();
        if (TextUtils.isEmpty(key)) {
            Toast.makeText(this, "Không thể lưu bài viết này", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean saved = BookmarkStorage.isBookmarked(this, key);
        if (saved) {
            BookmarkStorage.removeBookmark(this, key);
            Toast.makeText(this, "Đã gỡ khỏi danh sách lưu", Toast.LENGTH_SHORT).show();
        } else {
            BookmarkStorage.addBookmark(this, currentSavedArticle);
            Toast.makeText(this, "Đã lưu bài viết", Toast.LENGTH_SHORT).show();
        }
        updateBookmarkState();
    }

    private void updateBookmarkState() {
        if (btnBookmark == null || currentSavedArticle == null)
            return;
        String key = getBookmarkKey();
        boolean saved = BookmarkStorage.isBookmarked(this, key);
        btnBookmark.setText(saved ? "Đã lưu" : "Lưu");
    }

    private String getBookmarkKey() {
        if (currentSavedArticle == null)
            return "";
        if (!TextUtils.isEmpty(currentSavedArticle.getUrl()))
            return currentSavedArticle.getUrl();
        if (!TextUtils.isEmpty(currentSavedArticle.getId()))
            return currentSavedArticle.getId();
        return currentSavedArticle.getTitle();
    }

    private String sanitizeTitle(String title) {
        if (TextUtils.isEmpty(title))
            return "";
        Pattern pattern = Pattern.compile("\\s*\\|\\s*nguồn.*$", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(title).replaceAll("").trim();
    }

    private String buildHistoryKey(String url, String title) {
        String base = !TextUtils.isEmpty(url) ? url : title;
        if (TextUtils.isEmpty(base))
            return "history_default";
        return "history_" + base.hashCode();
    }

    /** Làm sạch & tự chia đoạn mỗi ~3 câu nếu nguồn không có xuống dòng */
    private String prettyContent(String raw) {
        if (raw == null)
            return "";
        String text = raw.trim();
        if (text.contains("\n")) {
            return text.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                    .replaceAll("\\n{3,}", "\n\n")
                    .trim();
        }
        String[] sentences = text.replaceAll("\\s+", " ").split("(?<=[\\.\\!\\?])\\s+");
        StringBuilder sb = new StringBuilder();
        int cnt = 0;
        for (String s : sentences) {
            if (s.isEmpty())
                continue;
            if (sb.length() > 0)
                sb.append(' ');
            sb.append(s.trim());
            cnt++;
            if (cnt >= 3) {
                sb.append("\n\n");
                cnt = 0;
            }
        }
        return sb.toString().trim();
    }

    private String formatDate(String d) {
        if (d == null)
            return "";
        return d.length() >= 10 ? d.substring(0, 10) : d;
    }

    private String domain(String u) {
        try {
            if (u == null || u.isEmpty())
                return "";
            java.net.URI uri = new java.net.URI(u);
            String host = uri.getHost();
            return host != null ? host.replaceFirst("^www\\.", "") : "";
        } catch (Exception e) {
            return "";
        }
    }

    private int mapSentiment(String label) {
        if (label == null)
            return R.drawable.neutral;
        String l = label.trim().toLowerCase(Locale.ROOT);
        switch (l) {
            case "tich cuc":
            case "tích cực":
            case "positive":
                return R.drawable.positive;
            case "tieu cuc":
            case "tiêu cực":
            case "negative":
                return R.drawable.negative;
            default:
                return R.drawable.neutral;
        }
    }

    private int mapSpam(String label) {
        if (label == null)
            return R.drawable.nospam;
        String l = label.trim().toLowerCase(Locale.ROOT);
        return l.equals("spam") ? R.drawable.spam : R.drawable.nospam; // "no_spam" -> nospam
    }

    /** Tính “X phút/giờ/ngày trước” từ ISO "yyyy-MM-dd'T'HH:mm:ss[.SSS...]" */
    private String timeAgoVi(String iso) {
        if (TextUtils.isEmpty(iso))
            return "";
        try {
            // Chuẩn hóa về yyyy-MM-dd'T'HH:mm:ss
            String base = iso;
            int dot = iso.indexOf('.');
            if (dot > 0)
                base = iso.substring(0, dot);
            if (base.length() > 19)
                base = base.substring(0, 19);

            long seconds;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDateTime ldt = LocalDateTime.parse(
                        base, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                Instant then = ldt.atZone(ZoneId.systemDefault()).toInstant();
                seconds = Duration.between(then, Instant.now()).getSeconds();
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date then = sdf.parse(base);
                seconds = (System.currentTimeMillis() - (then != null ? then.getTime() : 0L)) / 1000L;
            }

            if (seconds < 60)
                return "vừa xong";
            long minutes = seconds / 60;
            if (minutes < 60)
                return minutes + " phút trước";
            long hours = minutes / 60;
            if (hours < 24)
                return hours + " giờ trước";
            long days = hours / 24;
            if (days < 30)
                return days + " ngày trước";
            long months = days / 30;
            if (months < 12)
                return months + " tháng trước";
            long years = months / 12;
            return years + " năm trước";

        } catch (Exception e) {
            return iso.length() >= 10 ? iso.substring(0, 10) : "";
        }
    }

    private void toggleMenu() {
        if (isMenuExpanded) {
            // Collapse
            menuContainer.setVisibility(android.view.View.GONE);

            fabMain.setImageResource(R.drawable.ic_white_circle); // White circle icon
            fabMain.animate().alpha(0.3f).setDuration(200).start();
            fabMain.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF555555)); // Grey

            isMenuExpanded = false;
        } else {
            // Expand
            menuContainer.setVisibility(android.view.View.VISIBLE);

            fabMain.setImageResource(R.drawable.ic_clear);
            fabMain.animate().alpha(1.0f).setDuration(200).start();
            fabMain.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4F6EF7)); // Blue

            // Simple animation
            menuContainer.setAlpha(0f);
            menuContainer.setTranslationX(50f);
            menuContainer.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(200)
                    .start();

            isMenuExpanded = true;
        }
    }

    private void showCommentDialog() {
        if (currentArticleId == null)
            return;
        CommentBottomSheet fragment = CommentBottomSheet.newInstance(currentArticleId);
        fragment.show(getSupportFragmentManager(), "CommentBottomSheet");
    }

    private void showChatbotDialog() {
        if (currentUrl == null || currentTitle == null) {
            Toast.makeText(this, "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(DetailActivity.this, ChatbotActivity.class);
        String safeTitle = safe(currentTitle);
        intent.putExtra(ChatbotActivity.EXTRA_ARTICLE_TITLE, safeTitle);
        intent.putExtra(ChatbotActivity.EXTRA_ARTICLE_URL, currentUrl);
        intent.putExtra(ChatbotActivity.EXTRA_HISTORY_KEY, buildHistoryKey(currentUrl, safeTitle));
        startActivity(intent);
    }

    private String getSentimentText(String label) {
        if (label != null) {
            String l = label.trim().toLowerCase(java.util.Locale.ROOT);
            if (l.equals("tich cuc") || l.equals("tích cực") || l.equals("positive")) {
                return "Tin tích cực";
            } else if (l.equals("tieu cuc") || l.equals("tiêu cực") || l.equals("negative")) {
                return "Tin tiêu cực";
            }
        }
        return "Tin trung lập";
    }

    private String getSpamText(String label) {
        if (label != null) {
            String l = label.trim().toLowerCase(java.util.Locale.ROOT);
            if (l.equals("spam")) {
                return "Tin spam";
            }
        }
        return "Tin không spam";
    }

    private void showTooltip(android.view.View anchor, String text, int xOffset) {
        android.view.View view = getLayoutInflater().inflate(R.layout.popup_tooltip, null);
        TextView tv = view.findViewById(R.id.tvTooltipText);
        tv.setText(text);

        android.widget.PopupWindow popup = new android.widget.PopupWindow(
                view,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setOutsideTouchable(true);

        // Calculate position: center horizontally above the anchor
        view.measure(android.view.View.MeasureSpec.UNSPECIFIED, android.view.View.MeasureSpec.UNSPECIFIED);
        int popupWidth = view.getMeasuredWidth();
        int popupHeight = view.getMeasuredHeight();

        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int x = location[0] + (anchor.getWidth() / 2) - (popupWidth / 2) + xOffset;
        int y = location[1] - popupHeight - 20; // Place above the icon with some margin

        popup.showAtLocation(anchor, android.view.Gravity.NO_GRAVITY, x, y);
    }

    // ===== Text-to-Speech Methods =====

    private void setupTTSListener() {
        ttsManager.setOnPlayStateChangeListener(new TextToSpeechManager.OnPlayStateChangeListener() {
            @Override
            public void onPlayStateChanged(boolean isPlaying, String text) {
                updateTTSUI(isPlaying, ttsManager.isLoading());
            }

            @Override
            public void onLoadingStateChanged(boolean isLoading, String text) {
                updateTTSUI(ttsManager.isPlaying(), isLoading);
            }

            @Override
            public void onPlaybackCompleted(String text) {
                // Tự động chuyển sang đoạn tiếp theo nếu đang auto play
                if (isAutoPlaying) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        playNextSegment();
                    }, 300); // Delay 300ms giữa các đoạn
                }
            }

            @Override
            public void onError(String text, String error) {
                updateTTSUI(false, false);
                isAutoPlaying = false;
            }
        });
    }

    private void toggleTTSPlayback() {
        if (ttsManager.isPlaying()) {
            // Đang phát, dừng lại
            ttsManager.stop();
            updateTTSUI(false, false);
            isAutoPlaying = false;
            return;
        }

        // Bắt đầu đọc từ đầu
        if (currentTitle == null || currentTitle.trim().isEmpty()) {
            Toast.makeText(this, "Không có nội dung để đọc", Toast.LENGTH_SHORT).show();
            return;
        }

        ttsManager.stop();
        isAutoPlaying = true;
        currentParagraphIndex = -1; // Bắt đầu từ tiêu đề (-1 = tiêu đề)
        playNextSegment();
    }

    private void playNextSegment() {
        if (!isAutoPlaying)
            return;

        if (currentParagraphIndex == -1) {
            // Đọc tiêu đề
            if (currentTitle != null && !currentTitle.trim().isEmpty()) {
                ttsManager.playText(currentTitle.trim(), 1);
                currentParagraphIndex = 0;
                return;
            }
            currentParagraphIndex = 0;
        }

        // Parse nội dung thành các đoạn
        String pretty = prettyContent(currentContent);
        String[] paragraphs = pretty.split("\\n\\n+");

        if (currentParagraphIndex < paragraphs.length) {
            String para = paragraphs[currentParagraphIndex].trim();
            if (!para.isEmpty()) {
                ttsManager.playText(para, 1);
                currentParagraphIndex++;
                return;
            }
            currentParagraphIndex++;
            playNextSegment(); // Bỏ qua đoạn trống
        } else {
            // Đã đọc hết
            isAutoPlaying = false;
            updateTTSUI(false, false);
            Toast.makeText(this, "Đã đọc xong bài viết", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupContentWithTTS(String content) {
        if (tvContent == null)
            return;

        String pretty = prettyContent(content);
        tvContent.setText(pretty);
        tvContent.setVisibility(android.view.View.VISIBLE);
    }

    private void updateTTSUI(boolean isPlaying, boolean isLoading) {
        if (btnTTS == null)
            return;

        if (isLoading) {
            // Hiển thị loading indicator trong button
            if (progressTTS != null) {
                progressTTS.setVisibility(android.view.View.VISIBLE);
            }
        } else {
            // Ẩn loading indicator
            if (progressTTS != null) {
                progressTTS.setVisibility(android.view.View.GONE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) {
            ttsManager.release();
        }
    }

    // ===== Related Articles Methods =====
    private void setupRelatedArticles() {
        if (rvRelatedArticles == null)
            return;

        relatedAdapter = new com.example.newsai.ui.RelatedArticlesAdapter(this);
        relatedAdapter.setOnItemClickListener(article -> {
            // Open detail page for related article
            Intent intent = new Intent(DetailActivity.this, DetailActivity.class);
            intent.putExtra(K_ID, article.get_id());
            startActivity(intent);
        });

        rvRelatedArticles.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(
                        this,
                        androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                        false));
        rvRelatedArticles.setAdapter(relatedAdapter);
    }

    private void loadRelatedArticles() {
        if (relatedAdapter == null || currentArticleId == null)
            return;

        // Set current article ID to exclude from results
        relatedAdapter.setCurrentArticleId(currentArticleId);

        // Fetch latest articles from API
        ApiService api = ApiClient.get().create(ApiService.class);
        api.getArticles(0, 15).enqueue(new Callback<List<NewsItem>>() {
            @Override
            public void onResponse(Call<List<NewsItem>> call, Response<List<NewsItem>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    runOnUiThread(() -> {
                        relatedAdapter.submitList(response.body());
                        // Show section only if there are related articles
                        if (relatedArticlesSection != null && relatedAdapter.getItemCount() > 0) {
                            relatedArticlesSection.setVisibility(android.view.View.VISIBLE);
                        }
                    });
                } else {
                    Log.d(TAG, "No related articles found");
                }
            }

            @Override
            public void onFailure(Call<List<NewsItem>> call, Throwable t) {
                Log.e(TAG, "Failed to load related articles", t);
            }
        });
    }
}