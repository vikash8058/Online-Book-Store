package com.bookstore.dto.response;

import com.bookstore.model.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
	private Long id;
	private String name;
	private String email;
	private Role role;
}