package com.bookstore.service;

import com.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/*
 * CustomUserDetailsService — updated for UC9.
 *
 * UC9 change:
 * Google users have null password.
 * If a GOOGLE user tries to login via email + password (/auth/login),
 * we throw an exception telling them to use Google login instead.
 * This prevents confusion between LOCAL and GOOGLE accounts.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        var user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + username));

        /*
         * UC9 fix — only block Google users from PASSWORD login.
         * Do NOT block here — this method is also called by JwtAuthFilter
         * for token validation. Google users with valid JWT must pass through.
         *
         * The Google user block is only in AuthController.login()
         * where password login is attempted.
         */
        return user;
    }
}