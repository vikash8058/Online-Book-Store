package com.bookstore.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
	private Long id;
	private Long bookId;
	private String bookTitle;
	private Integer quantity;
	private Double price;
}