package com.bookstore.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;
    private String razorpayOrderId;  // frontend needs this to open Razorpay popup
    private Double amount;           // in INR
    private String paymentMode;
    private String status;
}