package com.bookstore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class UserRegisterRequest {
	
	@NotBlank(message = "Name must not be blank")
	private String name;
	
	@Email(message = "Must be a valid email address")
    @NotBlank(message = "Email must not be blank")
	private String email;
	
	@NotBlank(message="Password cannot be blank")
	@Size(min = 8,message="Password must contain at least 8 characters")
	@Pattern(
			regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
			message = "Password must contain letters(atleast 1 capital letter), special character and numbers"
			)
	private String password;
}
