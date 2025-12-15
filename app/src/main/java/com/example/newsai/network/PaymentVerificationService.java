package com.example.newsai.network;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Service để xác minh thanh toán qua API ngân hàng
 */
public class PaymentVerificationService {
    private static final String TAG = "PaymentVerification";
    private static final String BASE_URL = "https://bank.dinhmanhhung.net/check-payment";
    private static final String ACCOUNT_NO = "669699669";

    public interface OnPaymentCheckListener {
        void onSuccess(PaymentResult result);

        void onError(String error);
    }

    public static class PaymentResult {
        public boolean paid;
        public String message;
        public String description;
        public long amount;
        public String date;
        public String refNo;

        public PaymentResult(boolean paid, String message) {
            this.paid = paid;
            this.message = message;
        }
    }

    /**
     * Kiểm tra thanh toán có thành công không
     * 
     * @param transferContent Nội dung chuyển khoản (userXXXXXXXX)
     * @param expectedAmount  Số tiền cần thanh toán
     * @param listener        Callback kết quả
     */
    public static void checkPayment(String transferContent, long expectedAmount, OnPaymentCheckListener listener) {
        new Thread(() -> {
            try {
                // Build URL with parameters
                String urlString = BASE_URL +
                        "?account_no=" + URLEncoder.encode(ACCOUNT_NO, "UTF-8") +
                        "&content=" + URLEncoder.encode(transferContent, "UTF-8") +
                        "&amount=" + expectedAmount;

                Log.d(TAG, "Checking payment: " + urlString);

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String jsonResponse = response.toString();
                    Log.d(TAG, "Response: " + jsonResponse);

                    // Parse JSON response
                    JSONObject json = new JSONObject(jsonResponse);
                    String status = json.optString("status", "");
                    boolean paid = json.optBoolean("paid", false);
                    String message = json.optString("message", "");

                    PaymentResult result = new PaymentResult(paid, message);

                    // Parse transaction details if available
                    if (paid && json.has("transaction")) {
                        JSONObject transaction = json.getJSONObject("transaction");
                        result.description = transaction.optString("description", "");
                        result.amount = transaction.optLong("amount", 0);
                        result.date = transaction.optString("date", "");
                        result.refNo = transaction.optString("refNo", "");
                    }

                    listener.onSuccess(result);
                } else {
                    // Read error response
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    Log.e(TAG, "Error response: " + response.toString());

                    // Try to parse error message
                    try {
                        JSONObject json = new JSONObject(response.toString());
                        String message = json.optString("message", "Lỗi kiểm tra thanh toán");
                        listener.onSuccess(new PaymentResult(false, message));
                    } catch (Exception e) {
                        listener.onError("HTTP Error: " + responseCode);
                    }
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error checking payment", e);
                listener.onError("Lỗi kết nối: " + e.getMessage());
            }
        }).start();
    }
}
