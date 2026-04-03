package com.bookstore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
 * Thrown when OTP verification fails for any of these reasons:
 *   → OTP not found for email (never requested)
 *   → OTP already used
 *   → OTP does not match what user entered
 * Results in 400 Bad Request response.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OtpInvalidException extends RuntimeException {
    public OtpInvalidException(String message) {
        super(message);
    }
}