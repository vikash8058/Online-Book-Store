package com.bookstore.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private Long cartItemId;
    private Long bookId;
    private String bookTitle;
    private String authorName;
    private double price;
    private int quantity;
    private double itemTotal;  // price × quantity
}