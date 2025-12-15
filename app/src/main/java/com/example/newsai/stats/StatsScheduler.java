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
        scheduleType(context, TYPE_DAY);
        scheduleType(context, TYPE_WEEK);
        scheduleType(context, TYPE_MONTH);
        scheduleType(context, TYPE_YEAR);
    }

    public static void scheduleType(Context context, String type) {
        long triggerAt = getNextTriggerTime(type);
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
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
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

    private static long getNextTriggerTime(String type) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long now = cal.getTimeInMillis();

        switch (type) {
            case TYPE_DAY:
                cal.set(Calendar.HOUR_OF_DAY, 8);
                cal.set(Calendar.MINUTE, 0);
                if (cal.getTimeInMillis() <= now) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
                break;
            case TYPE_WEEK:
                cal.set(Calendar.HOUR_OF_DAY, 8);
                cal.set(Calendar.MINUTE, 0);
                int currentDow = cal.get(Calendar.DAY_OF_WEEK);
                int diff = (Calendar.MONDAY - currentDow + 7) % 7;
                cal.add(Calendar.DAY_OF_YEAR, diff);
                if (cal.getTimeInMillis() <= now) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1);
                }
                break;
            case TYPE_MONTH:
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 8);
                cal.set(Calendar.MINUTE, 0);
                if (cal.getTimeInMillis() <= now) {
                    cal.add(Calendar.MONTH, 1);
                }
                break;
            case TYPE_YEAR:
                cal.set(Calendar.MONTH, Calendar.JANUARY);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 8);
                cal.set(Calendar.MINUTE, 0);
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

