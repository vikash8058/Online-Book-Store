package com.bookstore.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/*
 * JPA Entity — maps to 'otp_verifications' table in MySQL.
 * Stores OTP records for email verification during registration.
 * One row per OTP request. Old OTP deleted when new one is requested.
 */
@Entity
@Table(name = "otp_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;       // whose OTP is this

    @Column(nullable = false)
    private String otp;         // 6-digit code sent to email

    @Column(nullable = false)
    private LocalDateTime expiry;   // now + 5 minutes — after this → expired

    @Column(nullable = false)
    private boolean used;       // true = already verified → cannot reuse

    @Column(nullable = false)
    private LocalDateTime createdAt; // when OTP was generated
}