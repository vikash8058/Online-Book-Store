package com.bookstore.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookstore.dto.request.LoginRequest;
import com.bookstore.dto.response.LoginResponse;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session) {

        try {
            // Step 1 — verify credentials
            Authentication authentication=authenticationManager.authenticate(
            		new UsernamePasswordAuthenticationToken(
            				request.getEmail(),
            				request.getPassword()
            				)
            		);
            		

            // Step 2 — store authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Step 3 — store SecurityContext in session
            session.setAttribute(
                "SPRING_SECURITY_CONTEXT",
                SecurityContextHolder.getContext()
            );

            // Step 4 — get role from authentication
            String role = authentication.getAuthorities()
                    .iterator()
                    .next()
                    .getAuthority();

            return ResponseEntity.ok(LoginResponse.builder()
                    .message("Login successful")
                    .email(authentication.getName())
                    .role(role)
                    .build());

        } catch (BadCredentialsException ex) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.builder()
                            .message("Invalid email or password")
                            .email(request.getEmail())
                            .role("CUSTOMER")
                            .build());
        }
    }

    //  LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        // destroy session
        session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logged out successfully");
    }

    //  CHECK WHO IS LOGGED IN
    @GetMapping("/me")
    public ResponseEntity<LoginResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.builder()
                            .message("Not logged in")
                            .build());
        }

        String role = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        return ResponseEntity.ok(LoginResponse.builder()
                .message("Currently logged in")
                .email(authentication.getName())
                .role(role)
                .build());
    }
}