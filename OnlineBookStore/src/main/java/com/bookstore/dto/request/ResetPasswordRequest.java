package com.bookstore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * ResetPasswordRequest — DTO for POST /auth/reset-password
 *
 * Three fields required:
 *   email       → identify which user is resetting
 *   otp         → 6-digit code received on email
 *   newPassword → the new password to set (min 8 chars)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String otp;
    // 6-digit OTP received on email

    @NotBlank(message="Password cannot be blank")
	@Size(min = 8,message="Password must contain at least 8 characters")
	@Pattern(
			regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
			message = "Password must contain letters(atleast 1 capital letter), special character and numbers"
			)
    private String newPassword;
    // must be at least 8 characters
}