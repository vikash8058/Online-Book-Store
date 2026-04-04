package com.bookstore.service;

import com.bookstore.model.AuthProvider;
import com.bookstore.model.CustomOAuth2User;
import com.bookstore.model.Role;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/*
 * CustomOAuth2UserService — UC9
 *
 * PURPOSE:
 * Spring Security calls this automatically after Google
 * successfully authenticates the user.
 * We override loadUser() to:
 *   1. Get user info from Google (email, name, googleId)
 *   2. Check if user already exists in our DB
 *   3. If yes  → update their info
 *   4. If no   → create new user with GOOGLE provider
 *   5. Return  → our User object wrapped as OAuth2User
 *
 * EXTENDS DefaultOAuth2UserService:
 * Spring's default implementation that fetches user info
 * from Google's userinfo endpoint using Access Token.
 * We call super.loadUser() to get that info, then add our logic.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /*
     * loadUser() — called automatically by Spring Security
     * after Google redirects back with authorization code.
     *
     * OAuth2UserRequest → contains Access Token and client info
     * OAuth2User        → contains user attributes from Google
     *
     * Flow:
     * 1. Call super.loadUser() → fetches attributes from Google
     * 2. Extract email, name, googleId from attributes
     * 3. Find user in DB by email
     * 4. Exists  → update googleId and name
     * 5. New     → create user with GOOGLE provider
     * 6. Return  → our User object (implements OAuth2User via UserDetails)
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        /*
         * super.loadUser() — calls Google's userinfo endpoint
         * Returns OAuth2User with these attributes from Google:	
         *   sub     → Google's unique user ID
         *   email   → user's Gmail address
         *   name    → user's full name
         *   picture → profile picture URL
         */
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Extract user info from Google's response
        String googleId = oAuth2User.getAttribute("sub");
        // "sub" = subject = Google's unique ID for this user
        // same user always has same sub — never changes

        String email = oAuth2User.getAttribute("email");
        // Gmail address of the user

        String name = oAuth2User.getAttribute("name");
        // Full name from Google profile

      
        return userRepository.findByEmail(email)
                .map(existingUser -> {

                    existingUser.setGoogleId(googleId);
                    existingUser.setName(name);

                    User savedUser = userRepository.save(existingUser);

                    return new CustomOAuth2User(oAuth2User, savedUser);

                })
                .orElseGet(() -> {

                    User newUser = User.builder()
                            .name(name)
                            .email(email)
                            .password(null)
                            .role(Role.CUSTOMER)
                            .authProvider(AuthProvider.GOOGLE)
                            .googleId(googleId)
                            .build();

                    User savedUser = userRepository.save(newUser);

                    return new CustomOAuth2User(oAuth2User, savedUser);
                });
    }
}