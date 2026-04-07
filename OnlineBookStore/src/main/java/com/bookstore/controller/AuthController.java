package com.bookstore.controller;

import com.bookstore.dto.request.ForgotPasswordRequest;
import com.bookstore.dto.request.LoginRequest;
import com.bookstore.dto.request.RegisterRequest;
import com.bookstore.dto.request.ResetPasswordRequest;
import com.bookstore.dto.request.SendOtpRequest;
import com.bookstore.dto.request.UpdatePasswordRequest;
import com.bookstore.dto.response.AuthResponse;
import com.bookstore.exception.DuplicateEmailException;
import com.bookstore.model.Role;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.JwtService;
import com.bookstore.service.OtpService;
import com.bookstore.service.PasswordService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/*
 * AuthController — handles all authentication endpoints.
 * UC8 changes:
 *   → Added POST /auth/send-otp endpoint
 *   → Updated POST /auth/register to verify OTP before saving user
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService; // UC8 — injected for OTP operations
    private final PasswordService passwordService;

    /*
     * POST /auth/send-otp
     * UC8 — new endpoint.
     *
     * Flow:
     *  1. Check email not already registered
     *  2. Call OtpService.generateAndSendOtp()
     *     → deletes old OTP → generates new → saves → sends email
     *  3. Return confirmation message
     *
     * Public endpoint — no JWT needed (defined in SecurityConfig).
     */
    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@Valid @RequestBody SendOtpRequest request) {

        // Block if email already registered — no point sending OTP
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }

        // Generate OTP, save to DB, send to email
        otpService.generateAndSendOtp(request.getEmail());

        return ResponseEntity.ok("OTP sent to " + request.getEmail() + ". Valid for 5 minutes.");
    }

    /*
     * POST /auth/register
     * UC8 change → now requires OTP in request body.
     *
     * Flow:
     *  1. Check duplicate email
     *  2. Verify OTP via OtpService.verifyOtp()
     *     → throws OtpInvalidException or OtpExpiredException if fails
     *  3. Create user with BCrypt hashed password
     *  4. Save user to DB
     *  5. Return success message
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {

        // Duplicate email check
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }

        // UC8 — verify OTP before allowing registration
        // throws exception if OTP is wrong, expired, or already used
        otpService.verifyOtp(request.getEmail(), request.getOtp());

        // OTP verified — proceed to create account
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully. Please login to get your token.");
    }

    /*
     * POST /auth/login
     * No change from UC7.
     * AuthenticationManager verifies credentials → JWT token returned.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(token));
    }
    
    /*
     * POST /auth/forgot-password — UC11
     * Public endpoint — no JWT needed.
     *
     * Flow:
     *  1. Validate email
     *  2. PasswordService.forgotPassword()
     *     → find user → check LOCAL → generate OTP → send email
     *  3. Return success message
     *
     * Security note: always return same message whether
     * email exists or not — prevents email enumeration attack.
     * (We show "if account exists" message)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordService.forgotPassword(request.getEmail());

        return ResponseEntity.ok(
                "If an account exists with this email, an OTP has been sent. " +
                "Valid for 5 minutes.");
    }

    /*
     * POST /auth/reset-password — UC11
     * Public endpoint — no JWT needed.
     *
     * Flow:
     *  1. Validate request fields
     *  2. PasswordService.resetPassword()
     *     → find user → verify OTP → update password
     *  3. Return success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(
                "Password reset successfully. Please login with your new password.");
    }

    /*
     * POST /auth/update-password — UC11
     * PROTECTED endpoint — JWT token required.
     *
     * @AuthenticationPrincipal → gets currently logged in user
     * from Security Context (set by JwtAuthFilter).
     * No need to pass email in request body — we get it from token.
     *
     * Flow:
     *  1. Get logged in user from Security Context
     *  2. PasswordService.updatePassword()
     *     → verify current password → update to new password
     *  3. Return success message
     */
    @PostMapping("/update-password")
    public ResponseEntity<String> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
            /*
             * @AuthenticationPrincipal UserDetails userDetails
             * → Spring injects currently logged in user
             * → userDetails.getUsername() = email from JWT token
             * → No need to pass email in request body
             */

        passwordService.updatePassword(
                userDetails.getUsername(),
                // email from JWT token via Security Context
                request.getCurrentPassword(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(
                "Password updated successfully.");
    }
}