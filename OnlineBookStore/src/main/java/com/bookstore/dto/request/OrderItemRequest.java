package com.bookstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

	@NotNull(message = "Book ID must not be null")
	private Long bookId;

	@NotNull(message = "Quantity must not be null")
	@Min(value = 1, message = "Quantity must be at least 1")
	private Integer quantity;
}
