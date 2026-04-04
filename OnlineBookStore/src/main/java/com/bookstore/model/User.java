package com.bookstore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(nullable = false)
	private String name;

	@Email
	@NotBlank
	@Column(nullable = false, unique = true)
	private String email;

	/*
	 * UC9 change — password is now nullable. LOCAL users → have BCrypt hashed
	 * password GOOGLE users → password is null (Google handles auth)
	 * Removed @NotBlank and nullable = false
	 */
	@Size(min = 8)
	@Column(nullable = true)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	/*
	 * UC9 — new field. Tracks how user registered: LOCAL → email + OTP registration
	 * GOOGLE → Google OAuth2 login Default = LOCAL for all existing users.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private AuthProvider authProvider = AuthProvider.LOCAL;

	/*
	 * UC9 — new field. Stores Google's unique user ID (sub claim from Google's ID
	 * Token). Used to find existing Google users on subsequent logins. null for
	 * LOCAL users.
	 */
	@Column(nullable = true)
	private String googleId;

	//UserDetails methods

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public String getPassword() {
		return password;
	}

	// All return true — no account locking/expiry logic needed
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}