package com.example.newsai.util;

import android.content.Context;

import com.example.newsai.data.UserProfileManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * VIP Manager - wrapper cho UserProfileManager
 * Cung cấp các phương thức static để kiểm tra VIP status
 * Dữ liệu được lưu trên Firestore
 */
public class VipManager {

    /**
     * Check if user has active VIP subscription (async)
     */
    public static void isVipActive(Context context, OnVipCheckListener listener) {
        UserProfileManager.isVipActive(context, listener::onResult);
    }

    /**
     * Synchronous check interface
     */
    public interface OnVipCheckListener {
        void onResult(boolean isVip);
    }

    /**
     * Get VIP expiry date as formatted string (async)
     */
    public static void getVipExpiryDate(Context context, OnVipInfoListener listener) {
        new UserProfileManager().loadProfile(new UserProfileManager.OnProfileLoadedListener() {
            @Override
            public void onSuccess(UserProfileManager.UserProfile profile) {
                if (profile.getVipExpiry() > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    listener.onResult(sdf.format(new Date(profile.getVipExpiry())));
                } else {
                    listener.onResult("");
                }
            }

            @Override
            public void onError(String error) {
                listener.onResult("");
            }
        });
    }

    /**
     * Get VIP package name (async)
     */
    public static void getVipPackage(Context context, OnVipInfoListener listener) {
        new UserProfileManager().loadProfile(new UserProfileManager.OnProfileLoadedListener() {
            @Override
            public void onSuccess(UserProfileManager.UserProfile profile) {
                listener.onResult(profile.getVipPackage() != null ? profile.getVipPackage() : "");
            }

            @Override
            public void onError(String error) {
                listener.onResult("");
            }
        });
    }

    /**
     * Get days remaining until VIP expires (async)
     */
    public static void getDaysRemaining(Context context, OnDaysRemainingListener listener) {
        new UserProfileManager().loadProfile(new UserProfileManager.OnProfileLoadedListener() {
            @Override
            public void onSuccess(UserProfileManager.UserProfile profile) {
                listener.onResult(profile.getDaysRemaining());
            }

            @Override
            public void onError(String error) {
                listener.onResult(0);
            }
        });
    }

    public interface OnVipInfoListener {
        void onResult(String value);
    }

    public interface OnDaysRemainingListener {
        void onResult(long days);
    }

    /**
     * Activate VIP (saves to Firestore)
     */
    public static void setVipStatus(Context context, String packageName, String amount, long durationDays,
            UserProfileManager.OnActionListener listener) {
        new UserProfileManager().activateVip(packageName, amount, durationDays, listener);
    }

    /**
     * Clear VIP status (saves to Firestore)
     */
    public static void clearVipStatus(Context context, UserProfileManager.OnActionListener listener) {
        new UserProfileManager().deactivateVip(listener);
    }

    /**
     * Check if VIP features are accessible (async)
     */
    public static void canAccessVipFeatures(Context context, OnVipCheckListener listener) {
        isVipActive(context, listener);
    }
}
