package com.bookstore.controller;

import com.bookstore.dto.request.PaymentVerifyRequest;
import com.bookstore.dto.request.PlaceOrderRequest;
import com.bookstore.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /*
     * POST /api/payment/initiate
     * COD  → returns OrderResponse directly
     * ONLINE → returns PaymentResponse with razorpayOrderId
     */
    @PostMapping("/initiate")
    public ResponseEntity<Object> initiatePayment(
            @Valid @RequestBody PlaceOrderRequest request) throws Exception {
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    /*
     * POST /api/payment/verify?addressId=1
     * Called after Razorpay payment success
     * Verifies signature → creates order
     */
    @PostMapping("/verify")
    public ResponseEntity<Object> verifyPayment(
            @RequestBody PaymentVerifyRequest request,
            @RequestParam Long addressId) throws Exception {
        return ResponseEntity.ok(paymentService.verifyPayment(request, addressId));
    }
}