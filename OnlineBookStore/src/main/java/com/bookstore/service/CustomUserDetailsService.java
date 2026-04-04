package com.bookstore.service;

import com.bookstore.model.AuthProvider;
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

        // Find user by email — throws if not found
        var user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + username));

        /*
         * UC9 check — prevent Google users from using password login.
         * If authProvider = GOOGLE → they have no password
         * → tell them to use Google login instead.
         */
        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new UsernameNotFoundException(
                    "This account uses Google login. Please use 'Login with Google'.");
        }

        return user;
    }
}