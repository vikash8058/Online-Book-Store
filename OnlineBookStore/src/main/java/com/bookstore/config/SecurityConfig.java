package com.bookstore.config;

import com.bookstore.filter.JwtAuthFilter;
import com.bookstore.service.CustomUserDetailsService;
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

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)

            .authorizeHttpRequests(auth -> auth

                //Public endpoints 
                .requestMatchers("/auth/register").permitAll()
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/send-otp").permitAll() 

                .requestMatchers("/auth/hash").permitAll()
                //ADMIN only

                // User management
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // Book management
                .requestMatchers("/api/books/create").hasRole("ADMIN")
                .requestMatchers("/api/books/update/**").hasRole("ADMIN")
                .requestMatchers("/api/books/partialUpdate/**").hasRole("ADMIN")
                .requestMatchers("/api/books/delete/**").hasRole("ADMIN")

                // Order management
                .requestMatchers("/api/orders/get").hasRole("ADMIN")
                .requestMatchers("/api/orders/delete/**").hasRole("ADMIN")
                .requestMatchers("/api/orders/status/**").hasRole("ADMIN")

                //  CUSTOMER only 
                .requestMatchers("/api/orders/create/**").hasRole("CUSTOMER")

                // ADMIN + CUSTOMER 

                // Book viewing
                .requestMatchers("/api/books/get/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers("/api/books/get").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers("/api/books/search").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers("/api/books/author").hasAnyRole("ADMIN", "CUSTOMER")

                // Order viewing
                .requestMatchers("/api/orders/get/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers("/api/orders/user/**").hasAnyRole("ADMIN", "CUSTOMER")

                //Everything else needs authentication 
                .anyRequest().authenticated()
            )

            //Stateless — no sessions 
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            //Register AuthenticationProvider 
            .authenticationProvider(authenticationProvider())

            //Add JwtAuthFilter before Spring's default filter 
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
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