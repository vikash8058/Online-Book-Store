package com.bookstore.service;

import com.bookstore.exception.OtpExpiredException;
import com.bookstore.exception.OtpInvalidException;
import com.bookstore.model.OtpVerification;
import com.bookstore.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

/*
 * OtpService — updated for UC11.
 *
 * UC11 change → added generateOtpAndSave() method.
 * Returns the generated OTP string so caller can
 * send their own custom email (registration vs password reset).
 *
 * Old generateAndSendOtp() → generates + sends REGISTRATION email
 * New generateOtpAndSave()  → generates + saves to DB, returns OTP
 *                             caller decides which email to send
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 5;

    /*
     * generateAndSendOtp() — UC8 registration flow.
     * Generates OTP + saves to DB + sends REGISTRATION email.
     * Called by AuthController for /auth/send-otp endpoint.
     */
    @Transactional
    public void generateAndSendOtp(String email) {
        String otp = generateOtpAndSave(email);
        emailService.sendOtpEmail(email, otp);
    }

    /*
     * generateOtpAndSave() — UC11 new method.
     * Generates OTP + saves to DB.
     * Returns the OTP string.
     * Caller decides which email template to use.
     *
     * Used by PasswordService.forgotPassword()
     * which needs to send password reset email
     * instead of registration email.
     */
    @Transactional
    public String generateOtpAndSave(String email) {

        // Delete old OTP for this email
        otpRepository.deleteByEmail(email);

        // Generate 6-digit OTP
        String otp = generateOtp();

        // Save to DB
        OtpVerification otpVerification = OtpVerification.builder()
                .email(email)
                .otp(otp)
                .expiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpRepository.save(otpVerification);

        return otp;
        // caller uses this OTP to send their custom email
    }

    /*
     * verifyOtp() — UC8 + UC11.
     * Same verification logic for both registration and password reset.
     * 4 checks: exists, not used, not expired, matches.
     * Marks OTP as used after verification.
     */
    @Transactional
    public void verifyOtp(String email, String otp) {

        OtpVerification otpVerification = otpRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new OtpInvalidException(
                        "No OTP found for this email. Please request a new OTP."));

        if (otpVerification.isUsed()) {
            throw new OtpInvalidException(
                    "OTP already used. Please request a new OTP.");
        }

        if (LocalDateTime.now().isAfter(otpVerification.getExpiry())) {
            throw new OtpExpiredException(
                    "OTP has expired. Please request a new OTP.");
        }

        if (!otpVerification.getOtp().equals(otp)) {
            throw new OtpInvalidException(
                    "Invalid OTP. Please check and try again.");
        }

        otpVerification.setUsed(true);
        otpRepository.save(otpVerification);
    }

    /*
     * generateOtp() — produces random 6-digit number.
     * 100000 + Random(0-899999) → always exactly 6 digits.
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}