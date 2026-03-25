package com.bookstore.filter;

import com.bookstore.service.CustomUserDetailsService;
import com.bookstore.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        //Step 1: Read Authorization header
        final String authHeader = request.getHeader("Authorization");

        // Step 2: Check if header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        //Step 3: Extract token 
        final String token = authHeader.substring(7).trim();

        try {
            //Step 4: Extract username from token
            final String username = jwtService.extractUsername(token);

            // Step 5: Check Security Context
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                //Step 6: Load user from DB 
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                //  Step 7: Validate token 
                if (jwtService.isTokenValid(token, userDetails)) {

                    //Step 8: Create authentication object
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    // Step 9: Add request details 
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Step 10: Set Security Context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (Exception e) {
            // Token invalid/malformed/expired — Spring Security will block if needed
            System.out.println("JWT Filter error: " + e.getMessage());
        }

        //Step 11: Pass to next filter
        filterChain.doFilter(request, response);
    }
}