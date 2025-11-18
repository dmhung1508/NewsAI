package com.example.newsai.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class UserPrefs {
    private static final String PREF_NAME = "user_profile_prefs";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void save(Context context, String name, String email) {
        prefs(context).edit()
                .putString(KEY_NAME, name != null ? name : "")
                .putString(KEY_EMAIL, email != null ? email : "")
                .apply();
    }

    public static String getEmail(Context context) {
        return prefs(context).getString(KEY_EMAIL, "");
    }

    public static String getName(Context context) {
        return prefs(context).getString(KEY_NAME, "");
    }

    public static boolean hasEmail(Context context) {
        return !TextUtils.isEmpty(getEmail(context));
    }
}

