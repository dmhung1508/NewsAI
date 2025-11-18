package com.example.newsai;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import com.example.newsai.api.ApiClient;
import com.example.newsai.api.NewsVerifyApiService;
import com.example.newsai.models.VerifyRequest;
import com.example.newsai.models.VerifyResponse;

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
    
    private NewsVerifyApiService apiService;

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
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> onBackPressed());

        btnVerify.setOnClickListener(v -> {
            String newsText = etNewsInput.getText().toString().trim();
            
            if (TextUtils.isEmpty(newsText)) {
                Toast.makeText(this, "Vui lòng nhập nội dung tin tức", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verify the news
            verifyNews(newsText);
        });
    }

    private void verifyNews(String newsText) {
        // Show loading
        showLoading();

        // Hide previous results
        cvResult.setVisibility(View.GONE);
        cvTrueResult.setVisibility(View.GONE);
        cvFalseResult.setVisibility(View.GONE);

        // Call API to verify news
        VerifyRequest request = new VerifyRequest(newsText);
        Call<VerifyResponse> call = apiService.verifyNews(request);
        
        call.enqueue(new Callback<VerifyResponse>() {
            @Override
            public void onResponse(Call<VerifyResponse> call, Response<VerifyResponse> response) {
                hideLoading();
                
                if (response.isSuccessful() && response.body() != null) {
                    handleVerifyResponse(response.body());
                } else {
                    Toast.makeText(VerifyNewsActivity.this, 
                        "Lỗi: Không thể xác minh tin tức (Mã lỗi: " + response.code() + ")", 
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<VerifyResponse> call, Throwable t) {
                hideLoading();
                Toast.makeText(VerifyNewsActivity.this, 
                    "Không thể kết nối đến server: " + t.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void handleVerifyResponse(VerifyResponse response) {
        if (!"success".equals(response.getStatus())) {
            Toast.makeText(this, "Lỗi xử lý: " + response.getStatus(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Build detailed description from fact_list
        StringBuilder description = new StringBuilder();
        
        if (response.getSummary() != null && !response.getSummary().isEmpty()) {
            description.append(response.getSummary()).append("\n\n");
        }
        
        if (response.getFact_list() != null && !response.getFact_list().isEmpty()) {
            description.append("Chi tiết các sự kiện:\n\n");
            for (int i = 0; i < response.getFact_list().size(); i++) {
                VerifyResponse.FactItem fact = response.getFact_list().get(i);
                description.append("• ").append(fact.getFact());
                description.append(" (").append(fact.isVerify() ? "Đã xác minh" : "Chưa xác minh").append(")");
                if (i < response.getFact_list().size() - 1) {
                    description.append("\n\n");
                }
            }
        } else {
            description.append("Không có thông tin chi tiết về các sự kiện.");
        }

        // Show result based on legit status
        showResult(response.isLegit(), description.toString());
    }

    private void showResult(boolean isTrue, String description) {
        cvResult.setVisibility(View.VISIBLE);

        if (isTrue) {
            cvTrueResult.setVisibility(View.VISIBLE);
            cvFalseResult.setVisibility(View.GONE);
            tvTrueDescription.setText(description);
        } else {
            cvTrueResult.setVisibility(View.GONE);
            cvFalseResult.setVisibility(View.VISIBLE);
            tvFalseDescription.setText(description);
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        btnVerify.setEnabled(false);
        btnVerify.setAlpha(0.5f);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        btnVerify.setEnabled(true);
        btnVerify.setAlpha(1.0f);
    }
}
