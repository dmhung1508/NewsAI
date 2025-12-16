package com.example.newsai.stats;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.newsai.util.UserPrefs;

import java.util.Calendar;

public class StatsScheduler {
    public static final String EXTRA_TYPE = "extra_stats_type";
    public static final String TYPE_DAY = "day";
    public static final String TYPE_WEEK = "week";
    public static final String TYPE_MONTH = "month";
    public static final String TYPE_YEAR = "year";
    private static final String TAG = "StatsScheduler";

    public static void setupAll(Context context) {
        if (!UserPrefs.hasEmail(context)) {
            Log.w(TAG, "Chưa có email người dùng, bỏ qua setup lịch gửi thống kê.");
            return;
        }

        if (UserPrefs.isStatsDailyEnabled(context)) {
            scheduleType(context, TYPE_DAY);
        }
        if (UserPrefs.isStatsWeeklyEnabled(context)) {
            scheduleType(context, TYPE_WEEK);
        }
        if (UserPrefs.isStatsMonthlyEnabled(context)) {
            scheduleType(context, TYPE_MONTH);
        }
        if (UserPrefs.isStatsYearlyEnabled(context)) {
            scheduleType(context, TYPE_YEAR);
        }
    }

    public static void scheduleType(Context context, String type) {
        long triggerAt = getNextTriggerTime(context, type);
        if (triggerAt <= 0) {
            Log.w(TAG, "Không tính được thời gian cho " + type);
            return;
        }
        Intent intent = new Intent(context, StatsAlarmReceiver.class);
        intent.setAction("com.example.newsai.STATS_ALARM");
        intent.putExtra(EXTRA_TYPE, type);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                type.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            Log.w(TAG, "Không có AlarmManager");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Thiết bị chưa cho phép đặt exact alarm. Bỏ qua " + type);
            return;
        }
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            Calendar triggerCal = Calendar.getInstance();
            triggerCal.setTimeInMillis(triggerAt);
            String timeStr = String.format("%02d:%02d ngày %02d/%02d/%04d",
                    triggerCal.get(Calendar.HOUR_OF_DAY),
                    triggerCal.get(Calendar.MINUTE),
                    triggerCal.get(Calendar.DAY_OF_MONTH),
                    triggerCal.get(Calendar.MONTH) + 1,
                    triggerCal.get(Calendar.YEAR));
            Log.i(TAG, "Đã lên lịch " + type + " lúc " + timeStr + " (timestamp: " + triggerAt + ")");
        } catch (SecurityException e) {
            Log.e(TAG, "Không thể đặt lịch cho " + type, e);
        }
    }

    public static void rescheduleAll(Context context) {
        // Cancel all first
        cancelAll(context);
        // Then setup enabled ones
        setupAll(context);
    }

    private static void cancelAll(Context context) {
        cancelType(context, TYPE_DAY);
        cancelType(context, TYPE_WEEK);
        cancelType(context, TYPE_MONTH);
        cancelType(context, TYPE_YEAR);
    }

    private static void cancelType(Context context, String type) {
        Intent intent = new Intent(context, StatsAlarmReceiver.class);
        intent.setAction("com.example.newsai.STATS_ALARM");
        intent.putExtra(EXTRA_TYPE, type);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                type.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager != null) {
            alarmManager.cancel(pi);
        }
    }

    public static long getNextTriggerTime(Context context, String type) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long now = cal.getTimeInMillis();

        switch (type) {
            case TYPE_DAY:
                int dHour = UserPrefs.getStatsDailyHour(context);
                int dMinute = UserPrefs.getStatsDailyMinute(context);
                cal.set(Calendar.HOUR_OF_DAY, dHour);
                cal.set(Calendar.MINUTE, dMinute);
                if (cal.getTimeInMillis() <= now) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
                break;
            case TYPE_WEEK:
                int wDow = UserPrefs.getStatsWeeklyDow(context);
                int wHour = UserPrefs.getStatsWeeklyHour(context);
                int wMinute = UserPrefs.getStatsWeeklyMinute(context);
                cal.set(Calendar.HOUR_OF_DAY, wHour);
                cal.set(Calendar.MINUTE, wMinute);
                int currentDow = cal.get(Calendar.DAY_OF_WEEK);
                int diff = (wDow - currentDow + 7) % 7;
                cal.add(Calendar.DAY_OF_YEAR, diff);
                if (cal.getTimeInMillis() <= now) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1);
                }
                break;
            case TYPE_MONTH:
                int mDay = UserPrefs.getStatsMonthlyDay(context);
                int mHour = UserPrefs.getStatsMonthlyHour(context);
                int mMinute = UserPrefs.getStatsMonthlyMinute(context);
                cal.set(Calendar.DAY_OF_MONTH, mDay);
                cal.set(Calendar.HOUR_OF_DAY, mHour);
                cal.set(Calendar.MINUTE, mMinute);
                if (cal.getTimeInMillis() <= now) {
                    cal.add(Calendar.MONTH, 1);
                }
                break;
            case TYPE_YEAR:
                int yDay = UserPrefs.getStatsYearlyDay(context);
                int yMonth = UserPrefs.getStatsYearlyMonth(context);
                int yHour = UserPrefs.getStatsYearlyHour(context);
                int yMinute = UserPrefs.getStatsYearlyMinute(context);
                cal.set(Calendar.MONTH, yMonth);
                cal.set(Calendar.DAY_OF_MONTH, yDay);
                cal.set(Calendar.HOUR_OF_DAY, yHour);
                cal.set(Calendar.MINUTE, yMinute);
                if (cal.getTimeInMillis() <= now) {
                    cal.add(Calendar.YEAR, 1);
                }
                break;
            default:
                return -1;
        }
        return cal.getTimeInMillis();
    }

    /**
     * Test method: Trigger thủ công để gửi email thống kê ngay lập tức
     * Chỉ dùng để test, không lên lịch lại
     */
    public static void testSendNow(Context context, String type) {
        Log.i(TAG, "TEST: Trigger thủ công gửi email thống kê " + type);
        StatsEmailSender.send(context, type);
    }
}
