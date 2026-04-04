package com.bookstore.model;

/*
 * AuthProvider — enum to track HOW a user registered.
 *
 * LOCAL  → registered via email + OTP (UC8)
 *          has password stored in DB
 *
 * GOOGLE → registered via Google OAuth2 (UC9)
 *          no password stored — Google handles auth
 *
 * Used in User.java to differentiate login types.
 * Prevents LOCAL user from logging in via Google and vice versa.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}