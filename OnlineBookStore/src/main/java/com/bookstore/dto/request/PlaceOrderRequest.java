package com.bookstore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceOrderRequest {

    @NotNull(message = "Address ID is required")
    private Long addressId;

    // ONLINE or COD
    @NotNull(message = "Payment mode is required")
    private String paymentMode;
}