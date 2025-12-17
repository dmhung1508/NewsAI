package com.example.newsai;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.example.newsai.data.UserProfileManager;
import com.example.newsai.util.UserPrefs;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private TextView editName, editEmail, editPhone;
    private TextView tvUserName, tvUserEmail;
    private androidx.cardview.widget.CardView cardVipStatus;
    private TextView tvVipExpiry;
    private UserProfileManager userProfileManager;

    // Avatar
    private ImageView imgAvatar;
    private FrameLayout avatarContainer;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        userProfileManager = new UserProfileManager();

        // Setup image picker
        setupImagePicker();

        // Khởi tạo views
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        imgAvatar = findViewById(R.id.imgAvatar);
        avatarContainer = findViewById(R.id.avatarContainer);
        ImageButton btnBack = findViewById(R.id.btnBackProfile);
        cardVipStatus = findViewById(R.id.cardVipStatus);
        tvVipExpiry = findViewById(R.id.tvVipExpiry);

        // Avatar click to change
        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v -> showAvatarOptions());
        }

        // VIP Upgrade Button
        findViewById(R.id.btnUpgradeVip).setOnClickListener(v -> {
            Intent intent = new Intent(this, VipAccountActivity.class);
            startActivity(intent);
        });

        // Stats Settings Button
        findViewById(R.id.btnStatsSettings).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.newsai.stats.StatsSettingsActivity.class);
            startActivity(intent);
        });

        // Profile item click listeners for editing
        findViewById(R.id.layoutFullName)
                .setOnClickListener(v -> showEditDialog("Họ và tên", editName.getText().toString(), "name"));
        findViewById(R.id.layoutPhone)
                .setOnClickListener(v -> showEditDialog("Số điện thoại", editPhone.getText().toString(), "phone"));

        // Kiểm tra nếu chưa đăng nhập thì quay về login
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Hiển thị thông tin user từ Firestore
        loadUserProfile();

        findViewById(R.id.btnUpdate).setOnClickListener(v -> updateUserInfo());

        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());

        // TEST: Long press nút Update để test gửi email thống kê
        findViewById(R.id.btnUpdate).setOnLongClickListener(v -> {
            com.example.newsai.stats.StatsScheduler.testSendNow(this, com.example.newsai.stats.StatsScheduler.TYPE_DAY);
            Toast.makeText(this, "Đã trigger test gửi email thống kê. Xem logcat để kiểm tra.", Toast.LENGTH_LONG)
                    .show();
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
                    loadUserProfile();
                }
            });
        }
    }

    /**
     * Load user profile từ Firestore (bao gồm phone và VIP status)
     */
    private void loadUserProfile() {
        // First load from Firebase Auth
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            if (name != null) {
                editName.setText(name);
                tvUserName.setText(name);
            }
            if (email != null) {
                editEmail.setText(email);
                tvUserEmail.setText(email);
            }
            UserPrefs.save(this, name, email);

            // Load avatar from Firebase Auth
            loadAvatar();
        }

        // Then load additional info from Firestore
        userProfileManager.loadProfile(new UserProfileManager.OnProfileLoadedListener() {
            @Override
            public void onSuccess(UserProfileManager.UserProfile profile) {
                runOnUiThread(() -> {
                    // Load phone from Firestore
                    if (profile.getPhone() != null && !profile.getPhone().isEmpty()) {
                        editPhone.setText(profile.getPhone());
                    }

                    // Check VIP status from Firestore
                    if (profile.isVipActive()) {
                        cardVipStatus.setVisibility(View.VISIBLE);
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy",
                                java.util.Locale.getDefault());
                        String expiryDate = sdf.format(new java.util.Date(profile.getVipExpiry()));
                        tvVipExpiry.setText("Hết hạn: " + expiryDate);
                    } else {
                        cardVipStatus.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                // Fallback: just use Firebase Auth data
                runOnUiThread(() -> {
                    cardVipStatus.setVisibility(View.GONE);
                });
            }
        });
    }

    private void showEditDialog(String title, String currentValue, String field) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        com.google.android.material.textfield.TextInputEditText etInput = view.findViewById(R.id.etDialogInput);
        View btnCancel = view.findViewById(R.id.btnDialogCancel);
        View btnSave = view.findViewById(R.id.btnDialogSave);

        tvTitle.setText(title);
        etInput.setText(currentValue != null ? currentValue : "");
        if (currentValue != null) {
            etInput.setSelection(currentValue.length());
        }

        // Set input type for phone
        if (field.equals("phone")) {
            etInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newValue = etInput.getText().toString().trim();
            if (!TextUtils.isEmpty(newValue)) {
                if (field.equals("name")) {
                    editName.setText(newValue);
                    updateUserInfo();
                } else if (field.equals("phone")) {
                    editPhone.setText(newValue);
                    updatePhone(newValue);
                }
                dialog.dismiss();
            } else {
                etInput.setError("Không được để trống");
            }
        });

        dialog.show();
    }

    /**
     * Cập nhật số điện thoại vào Firestore
     */
    private void updatePhone(String phone) {
        userProfileManager.updatePhone(phone, new UserProfileManager.OnActionListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "Đã cập nhật số điện thoại", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateUserInfo() {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        // Kiểm tra
        if (TextUtils.isEmpty(name)) {
            editName.setError("Vui lòng nhập tên");
            editName.requestFocus();
            return;
        }

        // Cập nhật tên trong Firebase Auth
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Cập nhật cả name và phone vào Firestore
                        userProfileManager.updateProfile(name, phone, new UserProfileManager.OnActionListener() {
                            @Override
                            public void onSuccess() {
                                runOnUiThread(() -> {
                                    currentUser.reload().addOnCompleteListener(reloadTask -> {
                                        if (reloadTask.isSuccessful()) {
                                            currentUser = mAuth.getCurrentUser();
                                            loadUserProfile();
                                            Toast.makeText(ProfileActivity.this, "Cập nhật thành công!",
                                                    Toast.LENGTH_SHORT).show();
                                            UserPrefs.save(ProfileActivity.this,
                                                    currentUser != null ? currentUser.getDisplayName() : name,
                                                    currentUser != null ? currentUser.getEmail() : null);
                                        }
                                    });
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ProfileActivity.this, "Lỗi lưu Firestore: " + error,
                                            Toast.LENGTH_SHORT).show();
                                });
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

    // ===== Avatar Methods =====
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadAvatarToFirebase(uri);
                    }
                });
    }

    private void showAvatarOptions() {
        String[] options = { "Chọn ảnh từ thư viện", "Xóa ảnh đại diện" };

        new AlertDialog.Builder(this)
                .setTitle("Đổi ảnh đại diện")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Pick from gallery
                        imagePickerLauncher.launch("image/*");
                    } else if (which == 1) {
                        // Remove avatar
                        removeAvatar();
                    }
                })
                .show();
    }

    private void uploadAvatarToFirebase(Uri imageUri) {
        if (currentUser == null)
            return;

        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        // Compress and convert to base64, then save to Firestore
        new Thread(() -> {
            try {
                // Load and compress image
                java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                if (inputStream != null)
                    inputStream.close();

                // Resize if too large (max 400x400)
                int maxSize = 400;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float scale = Math.min((float) maxSize / width, (float) maxSize / height);
                if (scale < 1) {
                    bitmap = android.graphics.Bitmap.createScaledBitmap(
                            bitmap,
                            (int) (width * scale),
                            (int) (height * scale),
                            true);
                }

                // Convert to base64
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = "data:image/jpeg;base64,"
                        + android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);

                // Save to Firestore
                runOnUiThread(() -> {
                    userProfileManager.updateAvatarUrl(base64Image);

                    // Also update local display
                    Glide.with(ProfileActivity.this)
                            .load(imageUri)
                            .circleCrop()
                            .into(imgAvatar);

                    Toast.makeText(this, "Đã cập nhật ảnh đại diện", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void removeAvatar() {
        if (currentUser == null)
            return;

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(null)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userProfileManager.updateAvatarUrl(null);
                        imgAvatar.setImageResource(R.drawable.ic_avatar);
                        Toast.makeText(this, "Đã xóa ảnh đại diện", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAvatar() {
        // First try to load from Firestore profile (supports base64)
        userProfileManager.loadProfile(new UserProfileManager.OnProfileLoadedListener() {
            @Override
            public void onSuccess(UserProfileManager.UserProfile profile) {
                runOnUiThread(() -> {
                    String photoUrl = profile.getPhotoUrl();
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_avatar)
                                .error(R.drawable.ic_avatar)
                                .circleCrop()
                                .into(imgAvatar);
                    } else if (currentUser != null && currentUser.getPhotoUrl() != null) {
                        // Fallback to Firebase Auth photo
                        Glide.with(ProfileActivity.this)
                                .load(currentUser.getPhotoUrl())
                                .placeholder(R.drawable.ic_avatar)
                                .error(R.drawable.ic_avatar)
                                .circleCrop()
                                .into(imgAvatar);
                    } else {
                        imgAvatar.setImageResource(R.drawable.ic_avatar);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Fallback to Firebase Auth
                    if (currentUser != null && currentUser.getPhotoUrl() != null) {
                        Glide.with(ProfileActivity.this)
                                .load(currentUser.getPhotoUrl())
                                .placeholder(R.drawable.ic_avatar)
                                .error(R.drawable.ic_avatar)
                                .circleCrop()
                                .into(imgAvatar);
                    } else {
                        imgAvatar.setImageResource(R.drawable.ic_avatar);
                    }
                });
            }
        });
    }
}
