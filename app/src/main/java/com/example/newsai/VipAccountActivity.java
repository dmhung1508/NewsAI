package com.example.newsai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class VipAccountActivity extends AppCompatActivity {

    private CardView cardVip1Month;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vip_account);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        cardVip1Month = findViewById(R.id.cardVip1Month);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // VIP 1 Month - 20,000 VND
        cardVip1Month.setOnClickListener(v -> {
            openPaymentScreen("VIP 1 Tháng", "20000đ", "1_month");
        });
    }

    private void openPaymentScreen(String packageName, String amount, String packageId) {
        Intent intent = new Intent(this, VipPaymentActivity.class);
        intent.putExtra("package_name", packageName);
        intent.putExtra("amount", amount);
        intent.putExtra("package_id", packageId);
        startActivity(intent);
    }
}

