package com.example.newsai.stats;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newsai.R;
import com.example.newsai.util.UserPrefs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatsSettingsActivity extends AppCompatActivity {

    private Switch switchDaily, switchWeekly, switchMonthly, switchYearly;
    private LinearLayout layoutDailySettings, layoutWeeklySettings, layoutMonthlySettings, layoutYearlySettings;
    private TextView tvDailyTime, tvWeeklyTime, tvMonthlyTime, tvYearlyTime, tvYearlyDate, tvCountdown;
    private TextView tvWeeklyDow, tvMonthlyDay;

    // Temporary state
    private int dailyHour, dailyMinute;
    private int weeklyDow, weeklyHour, weeklyMinute;
    private int monthlyDay, monthlyHour, monthlyMinute;
    private int yearlyDay, yearlyMonth, yearlyHour, yearlyMinute;

    private android.os.Handler handler = new android.os.Handler();
    private Runnable countdownRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats_settings);

        initViews();
        loadData();
        setupListeners();
        updateVisibility();
        startCountdown();
    }

    private void initViews() {
        switchDaily = findViewById(R.id.switchDaily);
        switchWeekly = findViewById(R.id.switchWeekly);
        switchMonthly = findViewById(R.id.switchMonthly);
        switchYearly = findViewById(R.id.switchYearly);

        layoutDailySettings = findViewById(R.id.layoutDailySettings);
        layoutWeeklySettings = findViewById(R.id.layoutWeeklySettings);
        layoutMonthlySettings = findViewById(R.id.layoutMonthlySettings);
        layoutYearlySettings = findViewById(R.id.layoutYearlySettings);

        tvDailyTime = findViewById(R.id.tvDailyTime);
        tvWeeklyTime = findViewById(R.id.tvWeeklyTime);
        tvMonthlyTime = findViewById(R.id.tvMonthlyTime);
        tvYearlyTime = findViewById(R.id.tvYearlyTime);
        tvYearlyDate = findViewById(R.id.tvYearlyDate);
        tvCountdown = findViewById(R.id.tvCountdown);

        tvWeeklyDow = findViewById(R.id.tvWeeklyDow);
        tvMonthlyDay = findViewById(R.id.tvMonthlyDay);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveSettings());
    }

    private void setupSpinners() {
        // This method is no longer needed as spinners are replaced by custom dialogs.
        // Keeping it empty or removing it based on the diff. The diff implies removal.
    }

    private void loadData() {
        // Switches
        switchDaily.setChecked(UserPrefs.isStatsDailyEnabled(this));
        switchWeekly.setChecked(UserPrefs.isStatsWeeklyEnabled(this));
        switchMonthly.setChecked(UserPrefs.isStatsMonthlyEnabled(this));
        switchYearly.setChecked(UserPrefs.isStatsYearlyEnabled(this));

        // Daily
        dailyHour = UserPrefs.getStatsDailyHour(this);
        dailyMinute = UserPrefs.getStatsDailyMinute(this);
        updateTimeText(tvDailyTime, dailyHour, dailyMinute);

        // Weekly
        weeklyDow = UserPrefs.getStatsWeeklyDow(this);
        weeklyHour = UserPrefs.getStatsWeeklyHour(this);
        weeklyMinute = UserPrefs.getStatsWeeklyMinute(this);
        updateWeeklyDowText();
        updateTimeText(tvWeeklyTime, weeklyHour, weeklyMinute);

        // Monthly
        monthlyDay = UserPrefs.getStatsMonthlyDay(this);
        monthlyHour = UserPrefs.getStatsMonthlyHour(this);
        monthlyMinute = UserPrefs.getStatsMonthlyMinute(this);
        updateMonthlyDayText();
        updateTimeText(tvMonthlyTime, monthlyHour, monthlyMinute);

        // Yearly
        yearlyDay = UserPrefs.getStatsYearlyDay(this);
        yearlyMonth = UserPrefs.getStatsYearlyMonth(this);
        yearlyHour = UserPrefs.getStatsYearlyHour(this);
        yearlyMinute = UserPrefs.getStatsYearlyMinute(this);
        updateDateText(tvYearlyDate, yearlyDay, yearlyMonth);
        updateTimeText(tvYearlyTime, yearlyHour, yearlyMinute);
    }

    private void setupListeners() {
        // Switches
        switchDaily.setOnCheckedChangeListener((v, c) -> {
            updateVisibility();
            startCountdown();
        });
        switchWeekly.setOnCheckedChangeListener((v, c) -> {
            updateVisibility();
            startCountdown();
        });
        switchMonthly.setOnCheckedChangeListener((v, c) -> {
            updateVisibility();
            startCountdown();
        });
        switchYearly.setOnCheckedChangeListener((v, c) -> {
            updateVisibility();
            startCountdown();
        });

        // Daily Time
        tvDailyTime.setOnClickListener(v -> showTimePicker(dailyHour, dailyMinute, (h, m) -> {
            dailyHour = h;
            dailyMinute = m;
            updateTimeText(tvDailyTime, h, m);
            startCountdown();
        }));

        // Weekly
        tvWeeklyDow.setOnClickListener(v -> showWeeklyPicker());
        tvWeeklyTime.setOnClickListener(v -> showTimePicker(weeklyHour, weeklyMinute, (h, m) -> {
            weeklyHour = h;
            weeklyMinute = m;
            updateTimeText(tvWeeklyTime, h, m);
            startCountdown();
        }));

        // Monthly
        tvMonthlyDay.setOnClickListener(v -> showMonthlyPicker());
        tvMonthlyTime.setOnClickListener(v -> showTimePicker(monthlyHour, monthlyMinute, (h, m) -> {
            monthlyHour = h;
            monthlyMinute = m;
            updateTimeText(tvMonthlyTime, h, m);
            startCountdown();
        }));

        // Yearly Time
        tvYearlyTime.setOnClickListener(v -> showTimePicker(yearlyHour, yearlyMinute, (h, m) -> {
            yearlyHour = h;
            yearlyMinute = m;
            updateTimeText(tvYearlyTime, h, m);
            startCountdown();
        }));

        // Yearly Date
        tvYearlyDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                yearlyDay = dayOfMonth;
                yearlyMonth = month;
                updateDateText(tvYearlyDate, dayOfMonth, month);
                startCountdown();
            }, Calendar.getInstance().get(Calendar.YEAR), yearlyMonth, yearlyDay);
            dpd.show();
        });
    }

    private void updateVisibility() {
        layoutDailySettings.setVisibility(switchDaily.isChecked() ? View.VISIBLE : View.GONE);
        layoutWeeklySettings.setVisibility(switchWeekly.isChecked() ? View.VISIBLE : View.GONE);
        layoutMonthlySettings.setVisibility(switchMonthly.isChecked() ? View.VISIBLE : View.GONE);
        layoutYearlySettings.setVisibility(switchYearly.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void showTimePicker(int initialHour, int initialMinute, TimePickerCallback callback) {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> callback.onTimePicked(hourOfDay, minute),
                initialHour, initialMinute, true).show();
    }

    private void showWeeklyPicker() {
        String[] days = { "Chủ Nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7" };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn ngày trong tuần")
                .setSingleChoiceItems(days, weeklyDow - 1, (dialog, which) -> {
                    weeklyDow = which + 1;
                    updateWeeklyDowText();
                    startCountdown();
                    dialog.dismiss();
                })
                .show();
    }

    private void showMonthlyPicker() {
        View view = getLayoutInflater().inflate(R.layout.dialog_month_picker, null);
        android.widget.GridLayout grid = view.findViewById(R.id.gridDays);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();

        for (int i = 1; i <= 28; i++) {
            final int day = i;
            TextView btn = new TextView(this);
            btn.setText(String.valueOf(day));
            btn.setTextSize(16);
            btn.setTextColor(day == monthlyDay ? 0xFFFFFFFF : 0xFF000000);
            btn.setBackgroundResource(day == monthlyDay ? R.drawable.bg_card : 0); // Reuse bg_card or create a circle
                                                                                   // drawable
            if (day == monthlyDay)
                btn.setBackgroundColor(0xFF2196F3); // Simple highlight
            btn.setGravity(android.view.Gravity.CENTER);

            android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
            params.width = 0;
            params.height = 120;
            params.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                monthlyDay = day;
                updateMonthlyDayText();
                startCountdown();
                dialog.dismiss();
            });
            grid.addView(btn);
        }
        dialog.show();
    }

    private void updateTimeText(TextView tv, int hour, int minute) {
        tv.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
    }

    private void updateDateText(TextView tv, int day, int month) {
        tv.setText(String.format(Locale.getDefault(), "%02d/%02d", day, month + 1));
    }

    private void updateWeeklyDowText() {
        String[] days = { "Chủ Nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7" };
        if (weeklyDow >= 1 && weeklyDow <= 7) {
            tvWeeklyDow.setText(days[weeklyDow - 1]);
        }
    }

    private void updateMonthlyDayText() {
        tvMonthlyDay.setText("Ngày " + monthlyDay);
    }

    private void saveSettings() {
        // Save Daily
        UserPrefs.setStatsDailyEnabled(this, switchDaily.isChecked());
        UserPrefs.setStatsDailyTime(this, dailyHour, dailyMinute);

        // Save Weekly
        UserPrefs.setStatsWeeklyEnabled(this, switchWeekly.isChecked());
        UserPrefs.setStatsWeeklySchedule(this, weeklyDow, weeklyHour, weeklyMinute);

        // Save Monthly
        UserPrefs.setStatsMonthlyEnabled(this, switchMonthly.isChecked());
        UserPrefs.setStatsMonthlySchedule(this, monthlyDay, monthlyHour, monthlyMinute);

        // Save Yearly
        UserPrefs.setStatsYearlyEnabled(this, switchYearly.isChecked());
        UserPrefs.setStatsYearlySchedule(this, yearlyDay, yearlyMonth, yearlyHour, yearlyMinute);

        // Reschedule
        StatsScheduler.rescheduleAll(this);

        Toast.makeText(this, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countdownRunnable != null)
            handler.removeCallbacks(countdownRunnable);
    }

    private void startCountdown() {
        if (countdownRunnable != null)
            handler.removeCallbacks(countdownRunnable);
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                updateCountdown();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(countdownRunnable);
    }

    private void updateCountdown() {
        long nearestTrigger = -1;
        long now = System.currentTimeMillis();

        if (switchDaily.isChecked()) {
            long t = calculateTriggerTime(StatsScheduler.TYPE_DAY);
            if (nearestTrigger == -1 || t < nearestTrigger)
                nearestTrigger = t;
        }
        if (switchWeekly.isChecked()) {
            long t = calculateTriggerTime(StatsScheduler.TYPE_WEEK);
            if (nearestTrigger == -1 || t < nearestTrigger)
                nearestTrigger = t;
        }
        if (switchMonthly.isChecked()) {
            long t = calculateTriggerTime(StatsScheduler.TYPE_MONTH);
            if (nearestTrigger == -1 || t < nearestTrigger)
                nearestTrigger = t;
        }
        if (switchYearly.isChecked()) {
            long t = calculateTriggerTime(StatsScheduler.TYPE_YEAR);
            if (nearestTrigger == -1 || t < nearestTrigger)
                nearestTrigger = t;
        }

        if (nearestTrigger != -1) {
            long diff = nearestTrigger - now;
            if (diff < 0)
                diff = 0;
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            String timeStr;
            if (days > 0) {
                timeStr = String.format(Locale.getDefault(), "%d ngày %02d:%02d:%02d", days, hours % 24, minutes % 60,
                        seconds % 60);
            } else {
                timeStr = String.format(Locale.getDefault(), "%02d giờ %02d phút %02d giây", hours % 24, minutes % 60,
                        seconds % 60);
            }
            tvCountdown.setText("Sẽ gửi thông báo sau: " + timeStr);
            tvCountdown.setVisibility(View.VISIBLE);
        } else {
            tvCountdown.setVisibility(View.GONE);
        }
    }

    // Logic to calculate trigger time based on current UI state (not saved prefs)
    private long calculateTriggerTime(String type) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long now = cal.getTimeInMillis();

        switch (type) {
            case StatsScheduler.TYPE_DAY:
                cal.set(Calendar.HOUR_OF_DAY, dailyHour);
                cal.set(Calendar.MINUTE, dailyMinute);
                if (cal.getTimeInMillis() <= now)
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case StatsScheduler.TYPE_WEEK:
                cal.set(Calendar.HOUR_OF_DAY, weeklyHour);
                cal.set(Calendar.MINUTE, weeklyMinute);
                int dow = weeklyDow; // Use the state variable directly
                int currentDow = cal.get(Calendar.DAY_OF_WEEK);
                int diff = (dow - currentDow + 7) % 7;
                cal.add(Calendar.DAY_OF_YEAR, diff);
                if (cal.getTimeInMillis() <= now)
                    cal.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case StatsScheduler.TYPE_MONTH:
                int day = monthlyDay; // Use the state variable directly
                cal.set(Calendar.DAY_OF_MONTH, day);
                cal.set(Calendar.HOUR_OF_DAY, monthlyHour);
                cal.set(Calendar.MINUTE, monthlyMinute);
                if (cal.getTimeInMillis() <= now)
                    cal.add(Calendar.MONTH, 1);
                break;
            case StatsScheduler.TYPE_YEAR:
                cal.set(Calendar.MONTH, yearlyMonth);
                cal.set(Calendar.DAY_OF_MONTH, yearlyDay);
                cal.set(Calendar.HOUR_OF_DAY, yearlyHour);
                cal.set(Calendar.MINUTE, yearlyMinute);
                if (cal.getTimeInMillis() <= now)
                    cal.add(Calendar.YEAR, 1);
                break;
        }
        return cal.getTimeInMillis();
    }

    interface TimePickerCallback {
        void onTimePicked(int hour, int minute);
    }
}
