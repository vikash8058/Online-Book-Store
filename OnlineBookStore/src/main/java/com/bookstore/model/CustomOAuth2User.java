package com.bookstore.model;

import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/*
 * CustomOAuth2User — UC9
 *
 * Wrapper that combines:
 *   oauth2User → Google's user object (has Google attributes)
 *   user       → OUR User entity from DB (has our role, email etc)
 *
 * WHY this wrapper?
 * Spring Security OAuth2 expects OAuth2User from loadUser().
 * Our User entity implements UserDetails — not OAuth2User.
 * So we wrap both together:
 *   → OAuth2User interface satisfied (for Spring OAuth2 flow)
 *   → Our User accessible via getUser() (for JWT generation)
 *
 * KEY FIX:
 * getAuthorities() now returns OUR user's authorities (ROLE_CUSTOMER)
 * NOT Google's authorities — ensures consistent role handling.
 */
@AllArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private OAuth2User oauth2User;
    private User user;

    /*
     * getUser() — returns our User entity.
     * Used in OAuth2AuthenticationSuccessHandler
     * to generate JWT token.
     */
    public User getUser() {
        return user;
    }

    /*
     * getAttributes() — returns Google's user attributes.
     * Contains: sub, email, name, picture etc.
     * Required by OAuth2User interface.
     */
    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    /*
     * getAuthorities() — FIXED in UC9.
     * Returns OUR user's authorities (ROLE_CUSTOMER or ROLE_ADMIN)
     * NOT Google's authorities.
     *
     * WHY?
     * Google returns its own authority set which does not match
     * our ROLE_CUSTOMER / ROLE_ADMIN structure.
     * Using Google's authorities → SecurityConfig role checks fail.
     * Using our user's authorities → role checks work correctly.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
        // user.getAuthorities() → List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
    }

    /*
     * getName() — returns user's name from our DB.
     * Used by Spring Security for display purposes.
     */
    @Override
    public String getName() {
        return user.getEmail();
        // return email as name — consistent with getUsername() in User.java
        // ensures Spring Security identifies user by email everywhere
    }
}