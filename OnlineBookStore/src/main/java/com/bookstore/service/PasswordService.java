package com.bookstore.service;

import com.bookstore.exception.OtpExpiredException;
import com.bookstore.exception.OtpInvalidException;
import com.bookstore.exception.UserNotFoundException;
import com.bookstore.model.AuthProvider;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * PasswordService — UC11
 *
 * Handles all password related operations:
 *   1. forgotPassword()  → send OTP to email for reset
 *   2. resetPassword()   → verify OTP + set new password
 *   3. updatePassword()  → change password for logged in user
 *
 * Reuses OtpService from UC8 for OTP generation and verification.
 * Only works for LOCAL users — GOOGLE users have no password.
 */
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /*
     * forgotPassword() — called when user hits POST /auth/forgot-password
     *
     * Steps:
     *  1. Find user by email → not found → UserNotFoundException
     *  2. Check LOCAL user → GOOGLE users cannot reset
     *  3. generateOtpAndSave() → generates OTP + saves to DB → returns OTP
     *  4. sendPasswordResetOtpEmail() → sends password reset specific email
     */
    @Transactional
    public void forgotPassword(String email) {

        // Step 1 — find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "No account found with email: " + email));

        // Step 2 — block Google users
        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new OtpInvalidException(
                    "This account uses Google login. Password reset is not available.");
        }

        /*
         * Step 3 — generate OTP and save to DB.
         * Returns the generated OTP string.
         * Does NOT send any email — just DB operation.
         */
        String otp = otpService.generateOtpAndSave(email);

        /*
         * Step 4 — send password reset specific email.
         * Uses different template than registration OTP email.
         * Subject clearly says "Password Reset" not "Registration".
         */
        emailService.sendPasswordResetOtpEmail(email, otp);
    }

    /*
     * resetPassword() — called when user hits POST /auth/reset-password
     *
     * Steps:
     *  1. Find user by email
     *  2. Check LOCAL user
     *  3. Verify OTP via OtpService.verifyOtp()
     *     → throws if wrong/expired/used
     *  4. BCrypt encode new password
     *  5. Update password in DB
     */
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {

        // Step 1 — find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "No account found with email: " + email));

        // Step 2 — block Google users
        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new OtpInvalidException(
                    "This account uses Google login. Password reset is not available.");
        }

        /*
         * Step 3 — verify OTP.
         * Reuses OtpService.verifyOtp() from UC8.
         * Same 4 checks: exists, not used, not expired, matches.
         * Marks OTP as used after verification.
         */
        otpService.verifyOtp(email, otp);

        /*
         * Step 4 — encode new password with BCrypt.
         * passwordEncoder.encode() generates new BCrypt hash.
         * Old password hash is completely replaced.
         */
        String encodedPassword = passwordEncoder.encode(newPassword);

        // Step 5 — update password in DB
        user.setPassword(encodedPassword);
        userRepository.save(user);
        // UPDATE users SET password = ? WHERE id = ?
    }

    /*
     * updatePassword() — called when logged in user hits POST /auth/update-password
     *
     * Steps:
     *  1. Find user by email (from Security Context)
     *  2. Check LOCAL user
     *  3. BCrypt.matches(currentPassword, storedHash)
     *     → no match → 400 Bad Request
     *  4. Check new password != current password
     *  5. BCrypt encode new password
     *  6. Update in DB
     */
    @Transactional
    public void updatePassword(String email, String currentPassword, String newPassword) {

        // Step 1 — find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "No account found with email: " + email));

        // Step 2 — block Google users
        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new OtpInvalidException(
                    "This account uses Google login. Password update is not available.");
        }

        /*
         * Step 3 — verify current password.
         * passwordEncoder.matches(rawPassword, storedHash):
         *   → extracts salt from storedHash
         *   → hashes rawPassword with same salt
         *   → compares → true = match / false = wrong
         */
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new OtpInvalidException(
                    "Current password is incorrect.");
        }

        /*
         * Step 4 — prevent setting same password.
         * passwordEncoder.matches(newPassword, storedHash)
         * → true = same password → reject
         * Good security practice — force a genuinely new password.
         */
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new OtpInvalidException(
                    "New password cannot be same as current password.");
        }

        // Step 5 — encode new password
        String encodedPassword = passwordEncoder.encode(newPassword);

        // Step 6 — update in DB
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }
}