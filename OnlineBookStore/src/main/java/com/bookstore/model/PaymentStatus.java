package com.bookstore.model;

public enum PaymentStatus {
    PENDING,   // COD default / ONLINE before payment
    SUCCESS,   // ONLINE after Razorpay verify
    FAILED     // ONLINE if payment fails
}