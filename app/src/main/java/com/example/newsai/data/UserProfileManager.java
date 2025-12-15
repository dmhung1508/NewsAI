package com.example.newsai.data;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý thông tin user và VIP trong Firestore
 * Collection: users/{userId}
 */
public class UserProfileManager {
    private static final String TAG = "UserProfileManager";
    private static final String COLLECTION_USERS = "users";

    // User profile fields
    private static final String FIELD_PHONE = "phone";
    private static final String FIELD_DISPLAY_NAME = "displayName";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_PHOTO_URL = "photoUrl";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    // VIP fields
    private static final String FIELD_IS_VIP = "isVip";
    private static final String FIELD_VIP_PACKAGE = "vipPackage";
    private static final String FIELD_VIP_EXPIRY = "vipExpiry";
    private static final String FIELD_VIP_AMOUNT = "vipAmount";
    private static final String FIELD_VIP_STARTED_AT = "vipStartedAt";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    // Callbacks
    public interface OnProfileLoadedListener {
        void onSuccess(UserProfile profile);

        void onError(String error);
    }

    public interface OnActionListener {
        void onSuccess();

        void onError(String error);
    }

    // User Profile Model
    public static class UserProfile {
        private String phone;
        private String displayName;
        private String email;
        private String photoUrl;
        private boolean isVip;
        private String vipPackage;
        private long vipExpiry;
        private String vipAmount;
        private long vipStartedAt;

        public UserProfile() {
        }

        // Getters
        public String getPhone() {
            return phone;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getEmail() {
            return email;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }

        public boolean isVip() {
            return isVip;
        }

        public String getVipPackage() {
            return vipPackage;
        }

        public long getVipExpiry() {
            return vipExpiry;
        }

        public String getVipAmount() {
            return vipAmount;
        }

        public long getVipStartedAt() {
            return vipStartedAt;
        }

        // Setters
        public void setPhone(String phone) {
            this.phone = phone;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setPhotoUrl(String photoUrl) {
            this.photoUrl = photoUrl;
        }

        public void setVip(boolean vip) {
            isVip = vip;
        }

        public void setVipPackage(String vipPackage) {
            this.vipPackage = vipPackage;
        }

        public void setVipExpiry(long vipExpiry) {
            this.vipExpiry = vipExpiry;
        }

        public void setVipAmount(String vipAmount) {
            this.vipAmount = vipAmount;
        }

        public void setVipStartedAt(long vipStartedAt) {
            this.vipStartedAt = vipStartedAt;
        }

        // Check if VIP is still active
        public boolean isVipActive() {
            if (!isVip)
                return false;
            return System.currentTimeMillis() < vipExpiry;
        }

        // Get days remaining
        public long getDaysRemaining() {
            if (!isVipActive())
                return 0;
            long diff = vipExpiry - System.currentTimeMillis();
            return diff / (1000 * 60 * 60 * 24);
        }
    }

    public UserProfileManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private DocumentReference getUserDoc() {
        String userId = getCurrentUserId();
        if (userId == null)
            return null;
        return db.collection(COLLECTION_USERS).document(userId);
    }

    // =============== PROFILE METHODS ===============

    /**
     * Load user profile từ Firestore
     */
    public void loadProfile(OnProfileLoadedListener listener) {
        DocumentReference userDoc = getUserDoc();
        if (userDoc == null) {
            listener.onError("Chưa đăng nhập");
            return;
        }

        userDoc.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                UserProfile profile = new UserProfile();

                if (doc.exists()) {
                    profile.setPhone(doc.getString(FIELD_PHONE));
                    profile.setDisplayName(doc.getString(FIELD_DISPLAY_NAME));
                    profile.setEmail(doc.getString(FIELD_EMAIL));
                    profile.setPhotoUrl(doc.getString(FIELD_PHOTO_URL));
                    profile.setVip(doc.getBoolean(FIELD_IS_VIP) != null && doc.getBoolean(FIELD_IS_VIP));
                    profile.setVipPackage(doc.getString(FIELD_VIP_PACKAGE));
                    profile.setVipExpiry(doc.getLong(FIELD_VIP_EXPIRY) != null ? doc.getLong(FIELD_VIP_EXPIRY) : 0);
                    profile.setVipAmount(doc.getString(FIELD_VIP_AMOUNT));
                    profile.setVipStartedAt(
                            doc.getLong(FIELD_VIP_STARTED_AT) != null ? doc.getLong(FIELD_VIP_STARTED_AT) : 0);
                }

                // Merge with Firebase Auth data
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    if (profile.getDisplayName() == null || profile.getDisplayName().isEmpty()) {
                        profile.setDisplayName(user.getDisplayName());
                    }
                    if (profile.getEmail() == null || profile.getEmail().isEmpty()) {
                        profile.setEmail(user.getEmail());
                    }
                    if (profile.getPhotoUrl() == null && user.getPhotoUrl() != null) {
                        profile.setPhotoUrl(user.getPhotoUrl().toString());
                    }
                }

                listener.onSuccess(profile);
            } else {
                listener.onError(task.getException() != null ? task.getException().getMessage() : "Lỗi tải profile");
            }
        });
    }

    /**
     * Cập nhật số điện thoại
     */
    public void updatePhone(String phone, OnActionListener listener) {
        DocumentReference userDoc = getUserDoc();
        if (userDoc == null) {
            listener.onError("Chưa đăng nhập");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_PHONE, phone);
        updates.put(FIELD_UPDATED_AT, System.currentTimeMillis());

        userDoc.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Cập nhật toàn bộ profile
     */
    public void updateProfile(String displayName, String phone, OnActionListener listener) {
        DocumentReference userDoc = getUserDoc();
        if (userDoc == null) {
            listener.onError("Chưa đăng nhập");
            return;
        }

        FirebaseUser user = auth.getCurrentUser();

        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_DISPLAY_NAME, displayName);
        updates.put(FIELD_PHONE, phone);
        updates.put(FIELD_UPDATED_AT, System.currentTimeMillis());

        if (user != null) {
            updates.put(FIELD_EMAIL, user.getEmail());
            if (user.getPhotoUrl() != null) {
                updates.put(FIELD_PHOTO_URL, user.getPhotoUrl().toString());
            }
        }

        userDoc.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // =============== VIP METHODS ===============

    /**
     * Kích hoạt VIP
     */
    public void activateVip(String packageName, String amount, long durationDays, OnActionListener listener) {
        DocumentReference userDoc = getUserDoc();
        if (userDoc == null) {
            listener.onError("Chưa đăng nhập");
            return;
        }

        long now = System.currentTimeMillis();
        long expiryTime = now + (durationDays * 24 * 60 * 60 * 1000L);

        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_IS_VIP, true);
        updates.put(FIELD_VIP_PACKAGE, packageName);
        updates.put(FIELD_VIP_AMOUNT, amount);
        updates.put(FIELD_VIP_EXPIRY, expiryTime);
        updates.put(FIELD_VIP_STARTED_AT, now);
        updates.put(FIELD_UPDATED_AT, now);

        userDoc.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "VIP activated: " + packageName);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Hủy VIP
     */
    public void deactivateVip(OnActionListener listener) {
        DocumentReference userDoc = getUserDoc();
        if (userDoc == null) {
            listener.onError("Chưa đăng nhập");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_IS_VIP, false);
        updates.put(FIELD_VIP_PACKAGE, null);
        updates.put(FIELD_VIP_AMOUNT, null);
        updates.put(FIELD_VIP_EXPIRY, null);
        updates.put(FIELD_UPDATED_AT, System.currentTimeMillis());

        userDoc.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Kiểm tra VIP status (static method for easy access)
     */
    public void checkVipStatus(OnProfileLoadedListener listener) {
        loadProfile(listener);
    }

    /**
     * Quick check VIP from cache (for UI that needs fast response)
     * Falls back to Firestore if not cached
     */
    public static void isVipActive(Context context, OnVipCheckListener listener) {
        new UserProfileManager().loadProfile(new OnProfileLoadedListener() {
            @Override
            public void onSuccess(UserProfile profile) {
                listener.onResult(profile.isVipActive());
            }

            @Override
            public void onError(String error) {
                listener.onResult(false);
            }
        });
    }

    public interface OnVipCheckListener {
        void onResult(boolean isVip);
    }
}
