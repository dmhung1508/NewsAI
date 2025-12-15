package com.example.newsai.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Helper class for generating VietQR URLs
 * Documentation: https://www.vietqr.io/
 */
public class VietQRHelper {
    
    // Base URL for VietQR API
    private static final String BASE_URL = "https://img.vietqr.io/image";
    
    /**
     * Generate VietQR URL for bank transfer
     * 
     * @param bankId Bank ID (BIN code or short name) - e.g., "MB", "970415", "Vietinbank"
     * @param accountNo Account number (max 19 characters)
     * @param accountName Account holder name
     * @param amount Transfer amount (positive number, max 13 digits)
     * @param description Transfer description (max 50 characters, no special chars)
     * @param template QR template: "qr_only", "compact", "compact2", "print"
     * @return VietQR image URL
     */
    public static String generateQRUrl(
            String bankId,
            String accountNo,
            String accountName,
            long amount,
            String description,
            String template
    ) {
        try {
            // Encode parameters
            String encodedAccountName = URLEncoder.encode(accountName, "UTF-8");
            String encodedDescription = URLEncoder.encode(description, "UTF-8");
            
            // Build URL
            StringBuilder url = new StringBuilder(BASE_URL);
            url.append("/").append(bankId);
            url.append("-").append(accountNo);
            url.append("-").append(template).append(".jpg");
            url.append("?");
            
            // Add query parameters
            if (description != null && !description.isEmpty()) {
                url.append("addInfo=").append(encodedDescription).append("&");
            }
            
            if (accountName != null && !accountName.isEmpty()) {
                url.append("accountName=").append(encodedAccountName).append("&");
            }
            
            if (amount > 0) {
                url.append("amount=").append(amount);
            }
            
            return url.toString();
            
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generate VietQR URL with QR only template (no account info displayed)
     */
    public static String generateQROnlyUrl(
            String bankId,
            String accountNo,
            String accountName,
            long amount,
            String description
    ) {
        return generateQRUrl(bankId, accountNo, accountName, amount, description, "qronly");
    }
    
    /**
     * Generate VietQR URL with compact template (includes account info)
     */
    public static String generateCompactUrl(
            String bankId,
            String accountNo,
            String accountName,
            long amount,
            String description
    ) {
        return generateQRUrl(bankId, accountNo, accountName, amount, description, "compact");
    }
    
    /**
     * Generate VietQR URL with print template (optimized for printing)
     */
    public static String generatePrintUrl(
            String bankId,
            String accountNo,
            String accountName,
            long amount,
            String description
    ) {
        return generateQRUrl(bankId, accountNo, accountName, amount, description, "print");
    }
    
    /**
     * Validate bank account number
     * Account number can contain letters or numbers, max 19 characters
     */
    public static boolean isValidAccountNumber(String accountNo) {
        if (accountNo == null || accountNo.isEmpty()) {
            return false;
        }
        return accountNo.length() <= 19 && accountNo.matches("[a-zA-Z0-9]+");
    }
    
    /**
     * Validate transfer amount
     * Amount must be positive and max 13 digits
     */
    public static boolean isValidAmount(long amount) {
        return amount > 0 && String.valueOf(amount).length() <= 13;
    }
    
    /**
     * Validate transfer description
     * Max 50 characters, no special characters
     */
    public static boolean isValidDescription(String description) {
        if (description == null || description.isEmpty()) {
            return true; // Description is optional
        }
        return description.length() <= 50 && description.matches("[a-zA-Z0-9\\s]+");
    }
    
    /**
     * Convert USD to VND with approximate exchange rate
     * Note: For production, use real-time exchange rate API
     */
    public static long convertUSDtoVND(double usdAmount) {
        // Approximate rate: 1 USD = 24,000 VND
        return (long) (usdAmount * 24000);
    }
    
    /**
     * Parse amount from string (e.g., "$9.99" -> 9.99)
     */
    public static double parseAmountFromString(String amountString) {
        if (amountString == null || amountString.isEmpty()) {
            return 0;
        }
        // Remove currency symbols and commas
        String cleaned = amountString.replaceAll("[^0-9.]", "");
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

