package com.bookstore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * DTO for POST /auth/send-otp endpoint.
 * Only email needed — OTP will be sent to this address.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {

	@Email
	@NotBlank
	private String email; // email to send OTP to
}