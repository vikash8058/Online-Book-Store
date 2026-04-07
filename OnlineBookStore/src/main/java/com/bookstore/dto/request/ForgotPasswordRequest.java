package com.bookstore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * ForgotPasswordRequest — DTO for POST /auth/forgot-password
 *
 * Only email needed.
 * Server will find user by email and send OTP to it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    @Email
    @NotBlank
    private String email;
}