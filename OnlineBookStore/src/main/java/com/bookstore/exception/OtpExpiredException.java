package com.bookstore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
 * Thrown when OTP expiry time has passed.
 * expiry = createdAt + 5 minutes.
 * LocalDateTime.now().isAfter(expiry) → true → this exception thrown.
 * Results in 400 Bad Request response.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OtpExpiredException extends RuntimeException {
    public OtpExpiredException(String message) {
        super(message);
    }
}