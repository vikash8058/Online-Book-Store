package com.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * UpdatePasswordRequest — DTO for POST /auth/update-password
 *
 * Used by logged in user to change their password.
 * Requires JWT token in Authorization header.
 *
 * Two fields:
 *   currentPassword → must match what is stored in DB
 *   newPassword     → the new password to set
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordRequest {

    @NotBlank
    private String currentPassword;
    // must match BCrypt hash in DB

    @NotBlank(message="Password cannot be blank")
	@Size(min = 8,message="Password must contain at least 8 characters")
	@Pattern(
			regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
			message = "Password must contain letters(atleast 1 capital letter), special character and numbers"
			)
    private String newPassword;
    // new password — min 8 characters
}