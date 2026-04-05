package com.bookstore.config;

import com.bookstore.filter.JwtAuthFilter;
import com.bookstore.filter.OAuth2AuthenticationFailureHandler;
import com.bookstore.filter.OAuth2AuthenticationSuccessHandler;
import com.bookstore.service.CustomOAuth2UserService;
import com.bookstore.service.CustomUserDetailsService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    /*
     * UC9 — two new beans injected:
     * CustomOAuth2UserService      → loads/creates user from Google
     * OAuth2AuthenticationSuccessHandler → generates JWT after Google login
     */
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)

            .authorizeHttpRequests(auth -> auth

                // Public endpoints — no token needed
                .requestMatchers("/auth/register").permitAll()
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/send-otp").permitAll()

                /*
                 * UC9 — OAuth2 login endpoints — must be public
                 * /oauth2/authorization/google → triggers Google login redirect
                 * /login/oauth2/code/google    → Google redirects back here with code
                 */
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/login/oauth2/**").permitAll()

                // ADMIN only endpoints
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/books/create").hasRole("ADMIN")
                .requestMatchers("/api/books/update/**").hasRole("ADMIN")
                .requestMatchers("/api/books/partialUpdate/**").hasRole("ADMIN")
                .requestMatchers("/api/books/delete/**").hasRole("ADMIN")
                .requestMatchers("/api/orders/get").hasRole("ADMIN")
                .requestMatchers("/api/orders/delete/**").hasRole("ADMIN")
                .requestMatchers("/api/orders/status/**").hasRole("ADMIN")

                // CUSTOMER only
                .requestMatchers("/api/orders/create/**").hasRole("CUSTOMER")

                // ADMIN + CUSTOMER
                .requestMatchers("/api/books/get/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers("/api/books/search").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers("/api/books/author").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers("/api/orders/get/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers("/api/orders/user/**").hasAnyRole("ADMIN", "CUSTOMER")

                .anyRequest().authenticated()
            )

            /*
             * UC9 — OAuth2 login configuration.
             * oauth2Login() → enables Spring's built-in OAuth2 flow
             *
             * userInfoEndpoint().userService(customOAuth2UserService)
             * → tells Spring: after Google login, use OUR service
             *   to load/create user instead of default behavior
             *
             * successHandler(oAuth2SuccessHandler)
             * → tells Spring: after success, use OUR handler
             *   to generate JWT and return it
             */
            .oauth2Login(oauth2 -> oauth2
            	    .userInfoEndpoint(userInfo -> userInfo
            	        .userService(customOAuth2UserService)
            	    )
            	    .successHandler(oAuth2SuccessHandler)
            	    .failureHandler(oAuth2FailureHandler)
            	)
            	/*
            	 * When JWT is missing/expired on API calls
            	 * → return 401 JSON instead of redirecting to Google login page
            	 * This prevents Postman from receiving HTML instead of JSON
            	 */
            	.exceptionHandling(ex -> ex
            	    .authenticationEntryPoint((request, response, authException) -> {
            	        response.setContentType("application/json");
            	        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            	        response.getWriter().write(
            	            "{\"error\": \"Unauthorized. Please login to get a valid token.\"}"
            	        );
            	    })
            	)

            // Stateless — no sessions (same as UC7)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authenticationProvider(authenticationProvider())

            // JwtAuthFilter runs before Spring's filter (same as UC7)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}