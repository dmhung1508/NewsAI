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

    private static final String KEY_STATS_DAILY_ENABLED = "stats_daily_enabled";
    private static final String KEY_STATS_WEEKLY_ENABLED = "stats_weekly_enabled";
    private static final String KEY_STATS_MONTHLY_ENABLED = "stats_monthly_enabled";
    private static final String KEY_STATS_YEARLY_ENABLED = "stats_yearly_enabled";
    private static final String KEY_STATS_HOUR = "stats_hour";
    private static final String KEY_STATS_MINUTE = "stats_minute";

    public static boolean hasEmail(Context context) {
        return !TextUtils.isEmpty(getEmail(context));
    }

    public static void setStatsDailyEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_STATS_DAILY_ENABLED, enabled).apply();
    }

    public static boolean isStatsDailyEnabled(Context context) {
        return prefs(context).getBoolean(KEY_STATS_DAILY_ENABLED, true);
    }

    public static void setStatsWeeklyEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_STATS_WEEKLY_ENABLED, enabled).apply();
    }

    public static boolean isStatsWeeklyEnabled(Context context) {
        return prefs(context).getBoolean(KEY_STATS_WEEKLY_ENABLED, true);
    }

    public static void setStatsMonthlyEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_STATS_MONTHLY_ENABLED, enabled).apply();
    }

    public static boolean isStatsMonthlyEnabled(Context context) {
        return prefs(context).getBoolean(KEY_STATS_MONTHLY_ENABLED, true);
    }

    public static void setStatsYearlyEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_STATS_YEARLY_ENABLED, enabled).apply();
    }

    public static boolean isStatsYearlyEnabled(Context context) {
        return prefs(context).getBoolean(KEY_STATS_YEARLY_ENABLED, true);
    }

    private static final String KEY_STATS_DAILY_HOUR = "stats_daily_hour";
    private static final String KEY_STATS_DAILY_MINUTE = "stats_daily_minute";

    private static final String KEY_STATS_WEEKLY_DOW = "stats_weekly_dow";
    private static final String KEY_STATS_WEEKLY_HOUR = "stats_weekly_hour";
    private static final String KEY_STATS_WEEKLY_MINUTE = "stats_weekly_minute";

    private static final String KEY_STATS_MONTHLY_DAY = "stats_monthly_day";
    private static final String KEY_STATS_MONTHLY_HOUR = "stats_monthly_hour";
    private static final String KEY_STATS_MONTHLY_MINUTE = "stats_monthly_minute";

    private static final String KEY_STATS_YEARLY_DAY = "stats_yearly_day";
    private static final String KEY_STATS_YEARLY_MONTH = "stats_yearly_month";
    private static final String KEY_STATS_YEARLY_HOUR = "stats_yearly_hour";
    private static final String KEY_STATS_YEARLY_MINUTE = "stats_yearly_minute";

    // Daily
    public static void setStatsDailyTime(Context context, int hour, int minute) {
        prefs(context).edit().putInt(KEY_STATS_DAILY_HOUR, hour).putInt(KEY_STATS_DAILY_MINUTE, minute).apply();
    }

    public static int getStatsDailyHour(Context context) {
        return prefs(context).getInt(KEY_STATS_DAILY_HOUR, 8);
    }

    public static int getStatsDailyMinute(Context context) {
        return prefs(context).getInt(KEY_STATS_DAILY_MINUTE, 0);
    }

    // Weekly
    public static void setStatsWeeklySchedule(Context context, int dow, int hour, int minute) {
        prefs(context).edit()
                .putInt(KEY_STATS_WEEKLY_DOW, dow)
                .putInt(KEY_STATS_WEEKLY_HOUR, hour)
                .putInt(KEY_STATS_WEEKLY_MINUTE, minute)
                .apply();
    }

    public static int getStatsWeeklyDow(Context context) {
        return prefs(context).getInt(KEY_STATS_WEEKLY_DOW, java.util.Calendar.MONDAY);
    }

    public static int getStatsWeeklyHour(Context context) {
        return prefs(context).getInt(KEY_STATS_WEEKLY_HOUR, 8);
    }

    public static int getStatsWeeklyMinute(Context context) {
        return prefs(context).getInt(KEY_STATS_WEEKLY_MINUTE, 0);
    }

    // Monthly
    public static void setStatsMonthlySchedule(Context context, int day, int hour, int minute) {
        prefs(context).edit()
                .putInt(KEY_STATS_MONTHLY_DAY, day)
                .putInt(KEY_STATS_MONTHLY_HOUR, hour)
                .putInt(KEY_STATS_MONTHLY_MINUTE, minute)
                .apply();
    }

    public static int getStatsMonthlyDay(Context context) {
        return prefs(context).getInt(KEY_STATS_MONTHLY_DAY, 1);
    }

    public static int getStatsMonthlyHour(Context context) {
        return prefs(context).getInt(KEY_STATS_MONTHLY_HOUR, 8);
    }

    public static int getStatsMonthlyMinute(Context context) {
        return prefs(context).getInt(KEY_STATS_MONTHLY_MINUTE, 0);
    }

    // Yearly
    public static void setStatsYearlySchedule(Context context, int day, int month, int hour, int minute) {
        prefs(context).edit()
                .putInt(KEY_STATS_YEARLY_DAY, day)
                .putInt(KEY_STATS_YEARLY_MONTH, month)
                .putInt(KEY_STATS_YEARLY_HOUR, hour)
                .putInt(KEY_STATS_YEARLY_MINUTE, minute)
                .apply();
    }

    public static int getStatsYearlyDay(Context context) {
        return prefs(context).getInt(KEY_STATS_YEARLY_DAY, 1);
    }

    public static int getStatsYearlyMonth(Context context) {
        return prefs(context).getInt(KEY_STATS_YEARLY_MONTH, java.util.Calendar.JANUARY);
    }

    public static int getStatsYearlyHour(Context context) {
        return prefs(context).getInt(KEY_STATS_YEARLY_HOUR, 8);
    }

    public static int getStatsYearlyMinute(Context context) {
        return prefs(context).getInt(KEY_STATS_YEARLY_MINUTE, 0);
    }
}
