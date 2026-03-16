package com.bookstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookUpdateRequest {

	 // All fields are optional (no @NotBlank / @NotNull)
    // Only fields sent by the client will be updated
    // If a field is null, it means the client did not send it → skip it
	
	private String title;
	private String authorName;

	@Positive(message = "Price must be positive")
	private Double price;

	@Min(value = 0, message = "Stock must be at least 0")
	private Integer stock;

}
