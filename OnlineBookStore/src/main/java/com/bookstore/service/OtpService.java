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
 * OtpService — core logic for OTP generation and verification.
 * Two main responsibilities:
 *   1. generateAndSendOtp() → create OTP, save to DB, send email
 *   2. verifyOtp()          → validate OTP from user against DB record
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 5; // OTP valid for 5 minutes

    /*
     * generateAndSendOtp() — called when user hits POST /auth/send-otp
     *
     * @Transactional → deleteByEmail + save happen in one DB transaction.
     * If email sending fails, DB changes are rolled back automatically.
     *
     * Steps:
     *  1. Delete old OTP for this email (one active OTP per user)
     *  2. Generate new 6-digit OTP
     *  3. Save OTP to DB with 5-min expiry
     *  4. Send OTP via EmailService
     */
    @Transactional
    public void generateAndSendOtp(String email) {

        // Step 1 — remove any existing OTP for this email
        otpRepository.deleteByEmail(email);

        // Step 2 — generate 6-digit OTP
        String otp = generateOtp();

        // Step 3 — save to DB
        OtpVerification otpVerification = OtpVerification.builder()
                .email(email)
                .otp(otp)
                .expiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        otpRepository.save(otpVerification);

        // Step 4 — send email
        emailService.sendOtpEmail(email, otp);
    }

    /*
     * verifyOtp() — called inside POST /auth/register
     *
     * @Transactional → marking OTP as used is part of same transaction.
     *
     * Checks in order:
     *  1. Does an OTP exist for this email? → if not → OtpInvalidException
     *  2. Is it already used?               → if yes → OtpInvalidException
     *  3. Is it expired?                    → if yes → OtpExpiredException
     *  4. Does it match what user entered?  → if not → OtpInvalidException
     *  5. All pass → mark OTP as used → registration continues
     */
    @Transactional
    public void verifyOtp(String email, String otp) {

        // Step 1 — find latest OTP for this email
        OtpVerification otpVerification = otpRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new OtpInvalidException(
                        "No OTP found for this email. Please request a new OTP."));

        // Step 2 — already used check
        if (otpVerification.isUsed()) {
            throw new OtpInvalidException("OTP already used. Please request a new OTP.");
        }

        // Step 3 — expiry check
        // LocalDateTime.now().isAfter(expiry) → current time is past expiry
        if (LocalDateTime.now().isAfter(otpVerification.getExpiry())) {
            throw new OtpExpiredException("OTP has expired. Please request a new OTP.");
        }

        // Step 4 — OTP match check
        if (!otpVerification.getOtp().equals(otp)) {
            throw new OtpInvalidException("Invalid OTP. Please check and try again.");
        }

        // Step 5 — all checks passed → mark as used so it cannot be reused
        otpVerification.setUsed(true);
        otpRepository.save(otpVerification);
    }

    /*
     * generateOtp() — produces a random 6-digit number as String.
     *
     * Logic:
     *   100000 + random(0 to 899999)
     *   → minimum: 100000
     *   → maximum: 999999
     *   → always exactly 6 digits — no leading zeros issue
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}