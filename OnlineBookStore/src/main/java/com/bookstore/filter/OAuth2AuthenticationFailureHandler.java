package com.bookstore.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * OAuth2AuthenticationFailureHandler — UC9
 *
 * Called when Google OAuth2 login fails.
 * Instead of Spring's default error page,
 * we return a clean JSON error response.
 */
@Component
public class OAuth2AuthenticationFailureHandler
        extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(
                "{\"error\": \"OAuth2 login failed: " + exception.getMessage() + "\"}"
        );
        response.getWriter().flush();
    }
}