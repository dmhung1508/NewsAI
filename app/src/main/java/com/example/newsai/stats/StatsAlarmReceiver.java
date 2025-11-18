package com.example.newsai.stats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class StatsAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "StatsAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive được gọi với action: " + intent.getAction());
        String type = intent.getStringExtra(StatsScheduler.EXTRA_TYPE);
        if (TextUtils.isEmpty(type)) {
            Log.w(TAG, "Không có type trong intent, bỏ qua");
            return;
        }
        Log.i(TAG, "Bắt đầu gửi email thống kê loại: " + type);
        Context appContext = context.getApplicationContext();
        StatsEmailSender.send(appContext, type);
        StatsScheduler.scheduleType(appContext, type);
        Log.d(TAG, "Đã yêu cầu gửi email và lên lịch lại cho " + type);
    }
}

