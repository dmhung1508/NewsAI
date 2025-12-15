package com.example.newsai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import com.example.newsai.data.UserProfileManager;
import com.example.newsai.network.PaymentVerificationService;
import com.example.newsai.util.VietQRHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VipPaymentActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvHours, tvMinutes, tvSeconds;
    private TextView tvAmount;
    private ImageView imgQrCode;
    private MaterialButton btnSaveQr, btnCopyBank, btnCopyHolder, btnCopyAccount;
    private MaterialButton btnIHavePaid;
    private TextView tvSupport;

    private CountDownTimer countDownTimer;
    private String packageName, amount, packageId;
    private Bitmap qrBitmap;
    private String transferContent; // Nội dung chuyển khoản để xác minh
    private long vndAmount; // Số tiền thanh toán

    // Payment details - VietQR Configuration
    private static final String BANK_ID = "MB"; // Mã ngân hàng (MB - Military Bank)
    private static final String ACCOUNT_NUMBER = "669699669";
    private static final String ACCOUNT_HOLDER = "DINH Manh Hung";
    private static final String BANK_NAME = "MB Bank (Quân Đội)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vip_payment);

        getIntentData();
        initViews();
        setupTimer();
        generateQrCode();
        setupListeners();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        packageName = intent.getStringExtra("package_name");
        amount = intent.getStringExtra("amount");
        packageId = intent.getStringExtra("package_id");
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvHours = findViewById(R.id.tvHours);
        tvMinutes = findViewById(R.id.tvMinutes);
        tvSeconds = findViewById(R.id.tvSeconds);
        tvAmount = findViewById(R.id.tvAmount);
        imgQrCode = findViewById(R.id.imgQrCode);
        btnSaveQr = findViewById(R.id.btnSaveQr);
        btnCopyBank = findViewById(R.id.btnCopyBank);
        btnCopyHolder = findViewById(R.id.btnCopyHolder);
        btnCopyAccount = findViewById(R.id.btnCopyAccount);
        btnIHavePaid = findViewById(R.id.btnIHavePaid);
        tvSupport = findViewById(R.id.tvSupport);

        // Set payment info
        tvAmount.setText(amount);

        // Update bank info TextViews
        TextView tvBankName = findViewById(R.id.tvBankName);
        TextView tvAccountHolder = findViewById(R.id.tvAccountHolder);
        TextView tvAccountNumber = findViewById(R.id.tvAccountNumber);

        tvBankName.setText(BANK_NAME);
        tvAccountHolder.setText(ACCOUNT_HOLDER);
        tvAccountNumber.setText(ACCOUNT_NUMBER);
    }

    private void setupTimer() {
        // 15 hours countdown (in milliseconds)
        long duration = 15 * 60 * 60 * 1000;

        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long hours = (millisUntilFinished / (1000 * 60 * 60)) % 24;
                long minutes = (millisUntilFinished / (1000 * 60)) % 60;
                long seconds = (millisUntilFinished / 1000) % 60;

                tvHours.setText(String.format(Locale.getDefault(), "%02d", hours));
                tvMinutes.setText(String.format(Locale.getDefault(), "%02d", minutes));
                tvSeconds.setText(String.format(Locale.getDefault(), "%02d", seconds));
            }

            @Override
            public void onFinish() {
                tvHours.setText("00");
                tvMinutes.setText("00");
                tvSeconds.setText("00");
                showTimeoutDialog();
            }
        };
        countDownTimer.start();
    }

    private void generateQrCode() {
        // Parse amount from VND string (e.g., "20000đ" -> 20000)
        String amountValue = amount.replace("đ", "").replace(",", "").replace(".", "").trim();
        vndAmount = Long.parseLong(amountValue); // Lưu vào biến class

        // Get user ID for transfer description
        String userId = "";
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user != null) {
            // Lấy 8 ký tự cuối của UID để ngắn gọn
            String uid = user.getUid();
            userId = uid.length() > 8 ? uid.substring(uid.length() - 8) : uid;
        }

        // Create description for transfer with user ID
        // Format: "userXXXXXXXX" để dễ xác định người thanh toán
        transferContent = "user" + userId; // Lưu vào biến class để verify sau

        // Generate VietQR URL using helper
        String qrUrl = VietQRHelper.generateQROnlyUrl(
                BANK_ID,
                ACCOUNT_NUMBER,
                ACCOUNT_HOLDER,
                vndAmount,
                transferContent);

        if (qrUrl == null) {
            Toast.makeText(this, "Không thể tạo URL QR", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("VipPayment", "QR URL: " + qrUrl);
        Log.d("VipPayment", "Transfer content: " + transferContent);
        Log.d("VipPayment", "Amount: " + vndAmount + " VND");

        // Load QR code from URL in background thread
        new Thread(() -> {
            try {
                URL url = new URL(qrUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream input = connection.getInputStream();
                    qrBitmap = BitmapFactory.decodeStream(input);
                    input.close();

                    // Update UI on main thread
                    runOnUiThread(() -> {
                        if (qrBitmap != null) {
                            imgQrCode.setImageBitmap(qrBitmap);
                        } else {
                            Toast.makeText(this, "Không thể tải mã QR", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.e("VipPayment", "HTTP Error: " + responseCode);
                    runOnUiThread(
                            () -> Toast.makeText(this, "Lỗi tải QR: HTTP " + responseCode, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("VipPayment", "Error loading QR code", e);
                runOnUiThread(
                        () -> Toast.makeText(this, "Lỗi tải mã QR: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSaveQr.setOnClickListener(v -> saveQrCode());

        btnCopyBank.setOnClickListener(v -> {
            copyToClipboard("Bank Name", BANK_NAME);
            Toast.makeText(this, "Đã sao chép tên ngân hàng", Toast.LENGTH_SHORT).show();
        });

        btnCopyHolder.setOnClickListener(v -> {
            copyToClipboard("Account Holder", ACCOUNT_HOLDER);
            Toast.makeText(this, "Đã sao chép tên chủ tài khoản", Toast.LENGTH_SHORT).show();
        });

        btnCopyAccount.setOnClickListener(v -> {
            copyToClipboard("Account Number", ACCOUNT_NUMBER);
            Toast.makeText(this, "Đã sao chép số tài khoản", Toast.LENGTH_SHORT).show();
        });

        btnIHavePaid.setOnClickListener(v -> confirmPayment());

        tvSupport.setOnClickListener(v -> {
            // Open support/contact activity
            Toast.makeText(this, "Liên hệ hỗ trợ: support@newsai.com", Toast.LENGTH_LONG).show();
        });
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }

    private void saveQrCode() {
        if (qrBitmap == null) {
            Toast.makeText(this, "Chưa có mã QR để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "QR_VIP_" + timeStamp + ".png";

            File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "NewsAI");

            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            File imageFile = new File(storageDir, fileName);
            FileOutputStream fos = new FileOutputStream(imageFile);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            Toast.makeText(this, "Đã lưu mã QR vào thư viện ảnh", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("VipPayment", "Error saving QR code", e);
            Toast.makeText(this, "Không thể lưu mã QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmPayment() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận thanh toán")
                .setMessage("Bạn đã hoàn tất thanh toán cho gói " + packageName
                        + "?\n\nHệ thống sẽ kiểm tra giao dịch của bạn tự động.")
                .setPositiveButton("Kiểm tra thanh toán", (dialog, which) -> {
                    // Hiển thị loading
                    android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
                    progressDialog.setMessage("Đang kiểm tra giao dịch...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    // Gọi API xác minh thanh toán
                    PaymentVerificationService.checkPayment(transferContent, vndAmount,
                            new PaymentVerificationService.OnPaymentCheckListener() {
                                @Override
                                public void onSuccess(PaymentVerificationService.PaymentResult result) {
                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();

                                        if (result.paid) {
                                            // ✅ Thanh toán thành công - Kích hoạt VIP
                                            Log.d("VipPayment", "Payment verified! RefNo: " + result.refNo);
                                            saveVipStatus();
                                            showSuccessDialog();
                                        } else {
                                            // ❌ Chưa tìm thấy giao dịch
                                            showPaymentNotFoundDialog(result.message);
                                        }
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(VipPaymentActivity.this,
                                                "Lỗi kiểm tra: " + error, Toast.LENGTH_LONG).show();
                                    });
                                }
                            });
                })
                .setNegativeButton("Chưa thanh toán", null)
                .show();
    }

    /**
     * Hiển thị dialog khi chưa tìm thấy giao dịch
     */
    private void showPaymentNotFoundDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Chưa tìm thấy giao dịch")
                .setMessage("Hệ thống chưa ghi nhận được giao dịch của bạn.\n\n" +
                        "Vui lòng kiểm tra:\n" +
                        "• Đã chuyển khoản đúng số tiền: " + amount + "\n" +
                        "• Nội dung chuyển khoản: " + transferContent + "\n\n" +
                        "Nếu bạn vừa chuyển khoản, vui lòng đợi 1-2 phút rồi thử lại.")
                .setPositiveButton("Thử lại", (dialog, which) -> confirmPayment())
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void saveVipStatus() {
        // Calculate duration in days based on package
        long durationDays = 30; // default 30 days

        switch (packageId) {
            case "1_month":
                durationDays = 30;
                break;
            case "3_months":
                durationDays = 90;
                break;
            case "1_year":
                durationDays = 365;
                break;
        }

        // Save to Firestore
        UserProfileManager userProfileManager = new UserProfileManager();
        userProfileManager.activateVip(packageName, amount, durationDays, new UserProfileManager.OnActionListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Log.d("VipPayment", "VIP saved to Firestore successfully");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("VipPayment", "Error saving VIP to Firestore: " + error);
                    Toast.makeText(VipPaymentActivity.this, "Lỗi lưu VIP: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Thành công!")
                .setMessage("Cảm ơn bạn đã nâng cấp lên VIP! Tài khoản của bạn sẽ được kích hoạt trong vài phút.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Go back to profile or main screen
                    Intent intent = new Intent(this, ProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void showTimeoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hết thời gian")
                .setMessage("Thời gian thanh toán đã hết. Vui lòng thử lại.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
