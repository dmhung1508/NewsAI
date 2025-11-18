package com.example.newsai;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.newsai.network.ApiClient;
import com.example.newsai.network.NewsVerifyApiService;
import com.example.newsai.models.ExtractRequest;
import com.example.newsai.models.ExtractResponse;
import com.example.newsai.models.SearchRequest;
import com.example.newsai.models.SearchResponse;
import com.example.newsai.models.VerifyRequest;
import com.example.newsai.models.VerifyResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyNewsActivity extends AppCompatActivity {

    private EditText etNewsInput;
    private AppCompatButton btnVerify;
    private CardView cvResult;
    private CardView cvTrueResult;
    private CardView cvFalseResult;
    private TextView tvTrueDescription;
    private TextView tvFalseDescription;
    private ProgressBar progressBar;
    private ImageButton btnMenu;
    private ScrollView scrollView;
    
    // Processing views
    private CardView cvProcessing;
    private TextView tvLoadingStatus;
    private TextView tvSearchQuery;
    private TextView tvReviewingCount;
    private LinearLayout llSearchingSection;
    private LinearLayout llReviewingSection;
    private LinearLayout llSourcesList;
    
    private NewsVerifyApiService apiService;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_news);

        // Initialize views
        initViews();

        // Setup listeners
        setupListeners();
        
        // Initialize API service
        initApiService();
    }

    private void initApiService() {
        apiService = ApiClient.getNewsVerifyApiService();
    }

    private void initViews() {
        etNewsInput = findViewById(R.id.etNewsInput);
        btnVerify = findViewById(R.id.btnVerify);
        cvResult = findViewById(R.id.cvResult);
        cvTrueResult = findViewById(R.id.cvTrueResult);
        cvFalseResult = findViewById(R.id.cvFalseResult);
        tvTrueDescription = findViewById(R.id.tvTrueDescription);
        tvFalseDescription = findViewById(R.id.tvFalseDescription);
        progressBar = findViewById(R.id.progressBar);
        btnMenu = findViewById(R.id.btnMenu);
        scrollView = findViewById(R.id.scrollView);
        
        // Processing views
        cvProcessing = findViewById(R.id.cvProcessing);
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus);
        tvSearchQuery = findViewById(R.id.tvSearchQuery);
        tvReviewingCount = findViewById(R.id.tvReviewingCount);
        llSearchingSection = findViewById(R.id.llSearchingSection);
        llReviewingSection = findViewById(R.id.llReviewingSection);
        llSourcesList = findViewById(R.id.llSourcesList);
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> onBackPressed());

        btnVerify.setOnClickListener(v -> {
            String newsText = etNewsInput.getText().toString().trim();
            
            if (TextUtils.isEmpty(newsText)) {
                Toast.makeText(this, "Vui lòng nhập nội dung tin tức", Toast.LENGTH_SHORT).show();
                return;
            }

            // Start verification workflow
            startVerificationWorkflow(newsText);
        });
    }

    private void startVerificationWorkflow(String newsText) {
        // Hide keyboard
        hideKeyboard();
        
        // Hide previous results
        cvResult.setVisibility(View.GONE);
        cvTrueResult.setVisibility(View.GONE);
        cvFalseResult.setVisibility(View.GONE);
        
        // Show processing card
        cvProcessing.setVisibility(View.VISIBLE);
        llSearchingSection.setVisibility(View.GONE);
        llReviewingSection.setVisibility(View.GONE);
        
        // Disable verify button
        btnVerify.setEnabled(false);
        btnVerify.setAlpha(0.5f);
        
        // Show initial status
        tvLoadingStatus.setText("Đang xác nhận yêu cầu về \"" + newsText + "\" và chuẩn bị tra cứu thông tin chính xác.");
        
        // Scroll to processing card with delay to ensure it's visible
        handler.postDelayed(() -> smoothScrollToView(cvProcessing), 200);

        // Step 1: Search for related information
        searchRelatedInfo(newsText);
    }

    private void searchRelatedInfo(String newsText) {
        // Show searching section
        llSearchingSection.setVisibility(View.VISIBLE);
        tvSearchQuery.setText(newsText);
        
        SearchRequest searchRequest = new SearchRequest(newsText);
        Call<SearchResponse> call = apiService.searchNews(searchRequest);
        
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SearchResponse searchResponse = response.body();
                    
                    if (searchResponse.getResults() != null && !searchResponse.getResults().isEmpty()) {
                        // Show reviewing section with sources
                        showReviewingSources(searchResponse.getResults());
                        
                        // Step 2: Extract content from top results
                        extractContentFromResults(newsText, searchResponse.getResults());
                    } else {
                        hideLoading();
                        Toast.makeText(VerifyNewsActivity.this, 
                            "Không tìm thấy thông tin liên quan", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    hideLoading();
                    Toast.makeText(VerifyNewsActivity.this, 
                        "Lỗi tìm kiếm: " + response.code(), 
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                hideLoading();
                Toast.makeText(VerifyNewsActivity.this, 
                    "Không thể kết nối: " + t.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showReviewingSources(List<SearchResponse.SearchResult> results) {
        llReviewingSection.setVisibility(View.VISIBLE);
        
        // Update reviewing count
        int count = Math.min(10, results.size());
        tvReviewingCount.setText("Reviewing sources · " + count);
        
        // Clear previous sources
        llSourcesList.removeAllViews();
        
        // Add source items
        for (int i = 0; i < count; i++) {
            SearchResponse.SearchResult result = results.get(i);
            View sourceView = createSourceItemView(result);
            llSourcesList.addView(sourceView);
        }
        
        // Scroll to reviewing section
        handler.postDelayed(() -> smoothScrollToView(llReviewingSection), 300);
    }
    
    private View createSourceItemView(SearchResponse.SearchResult result) {
        LinearLayout itemLayout = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, dpToPx(8));
        itemLayout.setLayoutParams(layoutParams);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        
        // Check icon
        ImageView checkIcon = new ImageView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(16), dpToPx(16));
        iconParams.setMargins(0, 0, dpToPx(12), 0);
        checkIcon.setLayoutParams(iconParams);
        checkIcon.setImageResource(android.R.drawable.checkbox_on_background);
        checkIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        
        // Text container
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        ));
        textContainer.setOrientation(LinearLayout.VERTICAL);
        
        // Title
        TextView titleText = new TextView(this);
        titleText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        titleText.setText(result.getTitle());
        titleText.setTextColor(ContextCompat.getColor(this, R.color.title_black));
        titleText.setTextSize(14);
        titleText.setMaxLines(2);
        titleText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        
        // Domain
        TextView domainText = new TextView(this);
        domainText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        domainText.setText(extractDomain(result.getUrl()));
        domainText.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
        domainText.setTextSize(12);
        domainText.setPadding(0, dpToPx(2), 0, 0);
        
        textContainer.addView(titleText);
        textContainer.addView(domainText);
        
        itemLayout.addView(checkIcon);
        itemLayout.addView(textContainer);
        
        return itemLayout;
    }
    
    private String extractDomain(String url) {
        try {
            URL aUrl = new URL(url);
            String host = aUrl.getHost();
            // Remove www. if present
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host;
        } catch (Exception e) {
            return url;
        }
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void extractContentFromResults(String newsText, List<SearchResponse.SearchResult> results) {
        tvLoadingStatus.setText("Đang trích xuất nội dung từ các nguồn...");
        
        // Extract content from top 3 results
        int maxResults = Math.min(3, results.size());
        List<ExtractResponse> extractedContents = new ArrayList<>();
        final int[] completedRequests = {0};
        
        for (int i = 0; i < maxResults; i++) {
            SearchResponse.SearchResult result = results.get(i);
            ExtractRequest extractRequest = new ExtractRequest(result.getUrl());
            
            Call<ExtractResponse> call = apiService.extractContent(extractRequest);
            call.enqueue(new Callback<ExtractResponse>() {
                @Override
                public void onResponse(Call<ExtractResponse> call, Response<ExtractResponse> response) {
                    completedRequests[0]++;
                    
                    if (response.isSuccessful() && response.body() != null) {
                        ExtractResponse extractResponse = response.body();
                        if (extractResponse.isSuccess()) {
                            extractedContents.add(extractResponse);
                        }
                    }
                    
                    // When all requests completed, proceed to verification
                    if (completedRequests[0] == maxResults) {
                        if (!extractedContents.isEmpty()) {
                            verifyNewsWithExtractedInfo(newsText, extractedContents);
                        } else {
                            hideLoading();
                            Toast.makeText(VerifyNewsActivity.this, 
                                "Không thể trích xuất nội dung từ các nguồn", 
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<ExtractResponse> call, Throwable t) {
                    completedRequests[0]++;
                    
                    if (completedRequests[0] == maxResults) {
                        if (!extractedContents.isEmpty()) {
                            verifyNewsWithExtractedInfo(newsText, extractedContents);
                        } else {
                            hideLoading();
                            Toast.makeText(VerifyNewsActivity.this, 
                                "Không thể trích xuất nội dung", 
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
    }

    private void verifyNewsWithExtractedInfo(String newsText, List<ExtractResponse> extractedContents) {
        tvLoadingStatus.setText("Đang xác minh tin tức với AI...");
        
        // Build related_info string from extracted contents
        StringBuilder relatedInfo = new StringBuilder();
        for (int i = 0; i < extractedContents.size(); i++) {
            ExtractResponse content = extractedContents.get(i);
            relatedInfo.append("Nguồn ").append(i + 1).append(": ").append(content.getTitle()).append("\n");
            relatedInfo.append("URL: ").append(content.getUrl()).append("\n");
            
            // Limit content to 500 characters
            String textContent = content.getTextContent();
            if (textContent != null) {
                if (textContent.length() > 500) {
                    textContent = textContent.substring(0, 500) + "...";
                }
                relatedInfo.append("Nội dung: ").append(textContent).append("\n\n");
            }
        }
        
        // Step 3: Verify news with Grok AI
        VerifyRequest verifyRequest = new VerifyRequest(newsText, relatedInfo.toString());
        Call<VerifyResponse> call = apiService.verifyNews(verifyRequest);
        
        call.enqueue(new Callback<VerifyResponse>() {
            @Override
            public void onResponse(Call<VerifyResponse> call, Response<VerifyResponse> response) {
                hideLoading();
                
                if (response.isSuccessful() && response.body() != null) {
                    handleVerifyResponse(response.body());
                } else {
                    Toast.makeText(VerifyNewsActivity.this, 
                        "Lỗi xác minh: " + response.code(), 
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<VerifyResponse> call, Throwable t) {
                hideLoading();
                Toast.makeText(VerifyNewsActivity.this, 
                    "Không thể xác minh: " + t.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void handleVerifyResponse(VerifyResponse response) {
        boolean isVerified = response.isIs_verified();
        String confidence = response.getConfidence();
        String explanation = response.getExplanation();
        
        // Build description with confidence level
        StringBuilder description = new StringBuilder();
        description.append("Độ tin cậy: ").append(confidence).append("\n\n");
        description.append(explanation);
        
        // Show result
        showResult(isVerified, description.toString());
    }

    private void showResult(boolean isVerified, String description) {
        cvResult.setVisibility(View.VISIBLE);

        if (isVerified) {
            cvTrueResult.setVisibility(View.VISIBLE);
            cvFalseResult.setVisibility(View.GONE);
            tvTrueDescription.setText(description);
        } else {
            cvTrueResult.setVisibility(View.GONE);
            cvFalseResult.setVisibility(View.VISIBLE);
            tvFalseDescription.setText(description);
        }
        
        // Scroll to result
        handler.postDelayed(() -> smoothScrollToView(cvResult), 200);
    }

    private void hideLoading() {
        cvProcessing.setVisibility(View.GONE);
        btnVerify.setEnabled(true);
        btnVerify.setAlpha(1.0f);
    }
    
    /**
     * Hide soft keyboard
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        
        // Also clear focus from EditText
        etNewsInput.clearFocus();
    }
    
    /**
     * Smooth scroll to a specific view
     */
    private void smoothScrollToView(final View view) {
        if (scrollView != null && view != null) {
            // Calculate the position to scroll to
            int yPosition = view.getTop();
            
            // Get the parent offset
            View parent = (View) view.getParent();
            while (parent != null && parent != scrollView) {
                yPosition += parent.getTop();
                if (parent.getParent() instanceof View) {
                    parent = (View) parent.getParent();
                } else {
                    break;
                }
            }
            
            // Smooth scroll to position
            scrollView.smoothScrollTo(0, yPosition - dpToPx(16));
        }
    }
}
