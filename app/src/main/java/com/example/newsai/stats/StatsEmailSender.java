package com.example.newsai.stats;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.example.newsai.util.UserPrefs;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatsEmailSender {
    private static final String TAG = "StatsEmailSender";

    public static void send(Context context, String type) {
        String email = UserPrefs.getEmail(context);
        String name = UserPrefs.getName(context);
        Log.d(TAG, "Bắt đầu gửi email thống kê " + type + " đến: " + email + " (tên: " + name + ")");
        if (TextUtils.isEmpty(email)) {
            Log.w(TAG, "Không có email người dùng, bỏ qua gửi thống kê.");
            return;
        }

        StatsApiService api = StatsApiClient.get();
        Call<StatsApiResponse> call;
        switch (type) {
            case StatsScheduler.TYPE_DAY:
                call = api.sendDailyEmail(email, name);
                break;
            case StatsScheduler.TYPE_WEEK:
                call = api.sendWeeklyEmail(email, name);
                break;
            case StatsScheduler.TYPE_MONTH:
                call = api.sendMonthlyEmail(email, name);
                break;
            case StatsScheduler.TYPE_YEAR:
                call = api.sendYearlyEmail(email, name);
                break;
            default:
                Log.w(TAG, "Loại thống kê không hợp lệ: " + type);
                return;
        }

        call.enqueue(new Callback<StatsApiResponse>() {
            @Override
            public void onResponse(Call<StatsApiResponse> call, Response<StatsApiResponse> response) {
                if (response.isSuccessful()) {
                    StatsApiResponse body = response.body();
                    Log.i(TAG, "Đã yêu cầu gửi email thống kê (" + type + "): " +
                            (body != null ? body.getMessage() : "success"));
                } else {
                    Log.e(TAG, "Gửi thống kê thất bại (" + type + "): HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<StatsApiResponse> call, Throwable t) {
                Log.e(TAG, "Lỗi khi gửi thống kê (" + type + ")", t);
            }
        });
    }
}

