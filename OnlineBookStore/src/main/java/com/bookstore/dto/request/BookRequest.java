package com.bookstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {

	@NotBlank(message = "Title must not be blank")
	private String title;

	@NotBlank(message = "Author name must not be blank")
	private String authorName;

	@NotNull(message = "Price must not be null")
	@Positive(message = "Price must be positive")
	private Double price;

	@NotNull(message = "Stock must not be null")
	@Min(value = 0, message = "Stock must be at least 0")
	private Integer stock;
}
