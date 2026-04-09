package com.bookstore.dto.response;

import com.bookstore.model.OrderStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private LocalDateTime orderDate;
    private Double totalAmount;
    private OrderStatus status;
    private Long userId;
    private List<OrderItemResponse> items;

    // UC13 — delivery address
    private String deliveryFullName;
    private String deliveryPhone;
    private String deliveryAddressLine;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryPincode;

    // UC13 — payment info
    private String paymentMode;
    private String paymentStatus;
}