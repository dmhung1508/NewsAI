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

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity {

    // Keys
    public static final String K_TITLE      = "k_title";
    public static final String K_IMAGE      = "k_image";
    public static final String K_URL        = "k_url";
    public static final String K_SOURCE_URL = "k_source_url";
    public static final String K_CONTENT    = "k_content";
    public static final String K_DATE       = "k_date";     // crawled_at
    public static final String K_POSTED     = "k_posted";   // posted_at
    public static final String K_SENTIMENT  = "k_sentiment";
    public static final String K_SPAM       = "k_spam";
    public static final String K_ID         = "article_id";

    private static final String TAG = "DETAIL";

    // Views
    private ImageView imgHeader, ivSentiment, ivSpam;
    private TextView tvCaption, tvTitle, tvLede, tvMeta, tvContent, tvSourceLink, btnShare, btnBookmark, badgeReadTime;
    private FloatingActionButton fabChatbot;
    private SavedArticle currentSavedArticle;
    private FloatingActionButton fabTTS;
    private ProgressBar progressTTS;
    private LinearLayout contentContainer;
    private TextToSpeechManager ttsManager;
    private String currentTitle;
    private String currentContent;
    private boolean isAutoPlaying;
    private int currentParagraphIndex;

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
                it.getStringExtra(K_SPAM)
        );
    }

    private void bindViews() {
        imgHeader     = findViewById(R.id.imgHeader);
        tvCaption     = findViewById(R.id.tvCaption);
        tvTitle       = findViewById(R.id.tvTitle);
        tvLede        = findViewById(R.id.tvLede);
        tvMeta        = findViewById(R.id.tvMeta);
        tvContent     = findViewById(R.id.tvContent);
        tvSourceLink  = findViewById(R.id.tvSourceLink);
        btnShare      = findViewById(R.id.btnShare);
        btnBookmark   = findViewById(R.id.btnBookmark);
        badgeReadTime = findViewById(R.id.badgeReadTime);
        ivSentiment   = findViewById(R.id.ivSentimentDetail);
        ivSpam        = findViewById(R.id.ivSpamDetail);
        fabChatbot    = findViewById(R.id.fabChatbot);
        fabTTS        = findViewById(R.id.fabTTS);
        progressTTS   = findViewById(R.id.progressTTS);
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

        if (fabTTS != null) {
            fabTTS.setOnClickListener(v -> toggleTTSPlayback());
        }
    }

    private void fetchArticleById(String articleId) {
        ApiService api = ApiClient.get().create(ApiService.class);
        api.getArticleById(articleId).enqueue(new Callback<NewsItem>() {
            @Override public void onResponse(Call<NewsItem> call, Response<NewsItem> res) {
                if (res.isSuccessful() && res.body() != null) {
                    NewsItem a = res.body();
                    String imgUrl = (a.getImage_contents() != null && !a.getImage_contents().isEmpty())
                            ? a.getImage_contents().get(0) : null;

                    displayArticle(
                            a.getTitle(),
                            imgUrl,
                            a.getUrl(),
                            a.getSource_url(),
                            a.getText_content(),
                            a.getCrawled_at(),
                            a.getPosted_at(),
                            a.getSentiment_label(),
                            a.getSpam_label()
                    );
                } else {
                    Toast.makeText(DetailActivity.this, "Không tìm thấy bài viết", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override public void onFailure(Call<NewsItem> call, Throwable t) {
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

        // Lưu title và content cho TTS
        currentTitle = sanitizedTitle;
        currentContent = content;
        
        // Hiển thị nội dung với các nút play cho từng đoạn
        setupContentWithTTS(content);

        // Link nguồn
        tvSourceLink.setText(url != null ? url : "");
        tvSourceLink.setOnClickListener(v -> openUrl(url));
        tvTitle.setOnClickListener(v -> { if (!TextUtils.isEmpty(url)) openUrl(url); });
        imgHeader.setOnClickListener(v -> { if (!TextUtils.isEmpty(url)) openUrl(url); });

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
        if (ivSentiment != null) ivSentiment.setImageResource(mapSentiment(sentiment));
        if (ivSpam != null)      ivSpam.setImageResource(mapSpam(spam));

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
                spam
        );
        updateBookmarkState();
    }

    // ===== Helpers =====
    private void openUrl(String u) {
        if (TextUtils.isEmpty(u)) {
            Toast.makeText(this, "Không có đường dẫn bài gốc", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));
    }

    private String safe(String s) { return s == null ? "" : s; }

    private String makeLede(String content) {
        if (content == null) return "";
        String c = content.trim();
        if (c.length() > 500) c = c.substring(0, 500) + "…";
        return c;
    }

    private void setupChatbotButton(String title, String content, String url) {
        if (fabChatbot == null) return;
        fabChatbot.setOnClickListener(v -> {
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
        if (btnBookmark == null || currentSavedArticle == null) return;
        String key = getBookmarkKey();
        boolean saved = BookmarkStorage.isBookmarked(this, key);
        btnBookmark.setText(saved ? "Đã lưu" : "Lưu");
    }

    private String getBookmarkKey() {
        if (currentSavedArticle == null) return "";
        if (!TextUtils.isEmpty(currentSavedArticle.getUrl())) return currentSavedArticle.getUrl();
        if (!TextUtils.isEmpty(currentSavedArticle.getId())) return currentSavedArticle.getId();
        return currentSavedArticle.getTitle();
    }

    private String sanitizeTitle(String title) {
        if (TextUtils.isEmpty(title)) return "";
        Pattern pattern = Pattern.compile("\\s*\\|\\s*nguồn.*$", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(title).replaceAll("").trim();
    }

    private String buildHistoryKey(String url, String title) {
        String base = !TextUtils.isEmpty(url) ? url : title;
        if (TextUtils.isEmpty(base)) return "history_default";
        return "history_" + base.hashCode();
    }

    /** Làm sạch & tự chia đoạn mỗi ~3 câu nếu nguồn không có xuống dòng */
    private String prettyContent(String raw) {
        if (raw == null) return "";
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
            if (s.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(s.trim());
            cnt++;
            if (cnt >= 3) { sb.append("\n\n"); cnt = 0; }
        }
        return sb.toString().trim();
    }

    private String formatDate(String d) {
        if (d == null) return "";
        return d.length() >= 10 ? d.substring(0, 10) : d;
    }

    private String domain(String u) {
        try {
            if (u == null || u.isEmpty()) return "";
            java.net.URI uri = new java.net.URI(u);
            String host = uri.getHost();
            return host != null ? host.replaceFirst("^www\\.", "") : "";
        } catch (Exception e) { return ""; }
    }

    private int mapSentiment(String label) {
        if (label == null) return R.drawable.neutral;
        String l = label.trim().toLowerCase(Locale.ROOT);
        switch (l) {
            case "tich cuc":
            case "tích cực":
            case "positive": return R.drawable.positive;
            case "tieu cuc":
            case "tiêu cực":
            case "negative": return R.drawable.negative;
            default: return R.drawable.neutral;
        }
    }
    private int mapSpam(String label) {
        if (label == null) return R.drawable.nospam;
        String l = label.trim().toLowerCase(Locale.ROOT);
        return l.equals("spam") ? R.drawable.spam : R.drawable.nospam; // "no_spam" -> nospam
    }

    /** Tính “X phút/giờ/ngày trước” từ ISO "yyyy-MM-dd'T'HH:mm:ss[.SSS...]" */
    private String timeAgoVi(String iso) {
        if (TextUtils.isEmpty(iso)) return "";
        try {
            // Chuẩn hóa về yyyy-MM-dd'T'HH:mm:ss
            String base = iso;
            int dot = iso.indexOf('.');
            if (dot > 0) base = iso.substring(0, dot);
            if (base.length() > 19) base = base.substring(0, 19);

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

            if (seconds < 60) return "vừa xong";
            long minutes = seconds / 60;
            if (minutes < 60) return minutes + " phút trước";
            long hours = minutes / 60;
            if (hours < 24) return hours + " giờ trước";
            long days = hours / 24;
            if (days < 30) return days + " ngày trước";
            long months = days / 30;
            if (months < 12) return months + " tháng trước";
            long years = months / 12;
            return years + " năm trước";

        } catch (Exception e) {
            return iso.length() >= 10 ? iso.substring(0, 10) : "";
        }
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
        if (!isAutoPlaying) return;

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
        if (tvContent == null) return;
        
        String pretty = prettyContent(content);
        tvContent.setText(pretty);
        tvContent.setVisibility(android.view.View.VISIBLE);
        
        try {
            PrecomputedTextCompat.Params params =
                    new PrecomputedTextCompat.Params.Builder(tvContent.getPaint())
                            .setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED)
                            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL)
                            .build();
            PrecomputedTextCompat p = PrecomputedTextCompat.create(tvContent.getText(), params);
            TextViewCompat.setPrecomputedText(tvContent, p);
        } catch (Exception ignore) {}
    }

    private void updateTTSUI(boolean isPlaying, boolean isLoading) {
        if (fabTTS == null) return;
        
        if (isLoading) {
            // Hiển thị loading indicator
            if (progressTTS != null) {
                progressTTS.setVisibility(android.view.View.VISIBLE);
            }
            fabTTS.setVisibility(android.view.View.GONE);
        } else {
            // Ẩn loading indicator
            if (progressTTS != null) {
                progressTTS.setVisibility(android.view.View.GONE);
            }
            fabTTS.setVisibility(android.view.View.VISIBLE);
            
            // Cập nhật icon FAB: nếu đang phát thì icon pause, không thì icon loa
            if (isPlaying) {
                fabTTS.setImageResource(R.drawable.ic_pause);
            } else {
                fabTTS.setImageResource(R.drawable.ic_speaker);
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
}