package com.bookstore.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

	private Long cartId;
	private List<CartItemResponse> items;
	private int totalItems; // total number of distinct books
	private double grandTotal; // sum of all itemTotals
}