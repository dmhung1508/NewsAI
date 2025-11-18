package com.example.newsai;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.newsai.util.UserPrefs;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;


public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private EditText editName, editEmail, editPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Khởi tạo views
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        ImageView imageView = findViewById(R.id.imgAvatar);
        ImageButton btnBack = findViewById(R.id.btnBackProfile);
        
        // Kiểm tra nếu chưa đăng nhập thì quay về login
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // Hiển thị thông tin user
        loadUserInfo();
        

        findViewById(R.id.btnUpdate).setOnClickListener(v -> updateUserInfo());

        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());
        
        // TEST: Long press nút Update để test gửi email thống kê
        findViewById(R.id.btnUpdate).setOnLongClickListener(v -> {
            com.example.newsai.stats.StatsScheduler.testSendNow(this, com.example.newsai.stats.StatsScheduler.TYPE_DAY);
            Toast.makeText(this, "Đã trigger test gửi email thống kê. Xem logcat để kiểm tra.", Toast.LENGTH_LONG).show();
            return true;
        });
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
        });
        
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                finish();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload thông tin mỗi khi quay lại màn hình
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    currentUser = mAuth.getCurrentUser();
                    loadUserInfo();
                }
            });
        }
    }
    
    private void loadUserInfo() {
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            if (name != null) {
                editName.setText(name);
            }
            if (email != null) {
                editEmail.setText(email);
                editEmail.setEnabled(false); // Email không thể sửa
            }
            if (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isEmpty()) {
                editPhone.setText(currentUser.getPhoneNumber());
            }
            UserPrefs.save(this, name, email);
        }
    }
    
    private void updateUserInfo() {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        
        // Kiểm trs
        if (TextUtils.isEmpty(name)) {
            editName.setError("Vui lòng nhập tên");
            editName.requestFocus();
            return;
        }
        
        // Cập nhật tên
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        
        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Reload user từ server
                    currentUser.reload().addOnCompleteListener(reloadTask -> {
                        if (reloadTask.isSuccessful()) {
                            currentUser = mAuth.getCurrentUser();
                            loadUserInfo();
                            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                            UserPrefs.save(this, currentUser != null ? currentUser.getDisplayName() : name,
                                    currentUser != null ? currentUser.getEmail() : null);
                        } else {
                            Toast.makeText(this, "Reload thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    String errorMsg = "Cập nhật thất bại";
                    if (task.getException() != null) {
                        errorMsg += ": " + task.getException().getMessage();
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất?")
            .setPositiveButton("Đăng xuất", (dialog, which) -> {
                // Đăng xuất Firebase
                mAuth.signOut();

                // Đăng xuất Google nếu có
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso).signOut();
                
                Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                
                // Chuyển về LoginActivity
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
}
