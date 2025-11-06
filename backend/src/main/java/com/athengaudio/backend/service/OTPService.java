package com.athengaudio.backend.service;

import java.util.Date;

import org.springframework.stereotype.Service;

@Service
public class OTPService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_OTP_ATTEMPTS = 3;

    public String generateOTP() {
        // Generate 6-digit OTP
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }

    public Date calculateExpiryDate() {
        return new Date(System.currentTimeMillis() + OTP_EXPIRY_MINUTES * 60 * 1000);
    }

    public boolean isOTPExpired(Date expiryDate) {
        if (expiryDate == null) {
            return true; // Nếu không có expiry date, coi như đã hết hạn
        }
        return new Date().after(expiryDate);
    }

    public boolean validateOTP(String inputOTP, String storedOTP, Date expiryDate, Integer attempts) {
        // Kiểm tra null values
        if (inputOTP == null || storedOTP == null || expiryDate == null) {
            return false;
        }

        // Kiểm tra số lần thử
        if (attempts != null && attempts >= MAX_OTP_ATTEMPTS) {
            return false;
        }

        // Kiểm tra hết hạn
        if (isOTPExpired(expiryDate)) {
            return false;
        }

        // Kiểm tra mã OTP
        return inputOTP.equals(storedOTP);
    }
}