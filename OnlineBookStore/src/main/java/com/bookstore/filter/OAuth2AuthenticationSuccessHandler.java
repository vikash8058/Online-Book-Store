package com.bookstore.filter;

import com.bookstore.model.CustomOAuth2User;
import com.bookstore.model.User;
import com.bookstore.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * OAuth2AuthenticationSuccessHandler — UC9
 *
 * PURPOSE:
 * Called automatically by Spring Security AFTER
 * Google login succeeds and CustomOAuth2UserService
 * has saved/updated the user in DB.
 *
 * Our job here:
 * 1. Get the authenticated User object
 * 2. Generate our OWN JWT token for this user
 * 3. Return JWT to client
 *    (in real frontend → redirect with token in URL param)
 *    (for Postman testing → write token in response body)
 *
 * EXTENDS SimpleUrlAuthenticationSuccessHandler:
 * Spring's base success handler.
 * We override onAuthenticationSuccess() to add JWT generation.
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtService jwtService;

	/*
	 * onAuthenticationSuccess() — called after Google login succeeds.
	 *
	 * Authentication object contains our User (returned by
	 * CustomOAuth2UserService.loadUser())
	 *
	 * Steps: 1. Get User from Authentication principal 2. Generate JWT using our
	 * JwtService (same as UC7 login) 3. Write JWT to response body (so
	 * Postman/frontend can read it)
	 */
	@Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

	/*
	 * authentication.getPrincipal() → our User object Same User that
	 * CustomOAuth2UserService returned. Cast to User because we know that is what
	 * we saved.
	 */

		CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();

		User user = principal.getUser();

		// Generate JWT
		String token = jwtService.generateToken(user);

		// Example: send token in response

		/*
		 * Generate JWT token — EXACT same method as UC7 login. Token contains:
		 * sub=email, iat=now, exp=now+24h Signed with our SECRET_KEY from
		 * application.properties
		 */

		/*
		 * Write token to HTTP response body. Content type = JSON so client can parse
		 * it.
		 *
		 * In real frontend app: → redirect to frontend URL with token as query param →
		 * e.g. http://frontend.com/oauth2/callback?token=eyJhbGci...
		 *
		 * For Postman testing: → writing to response body is easiest to read
		 */
		response.setContentType("application/json");
		response.getWriter().write("{\"token\":\"" + token + "\"}");

	}
}