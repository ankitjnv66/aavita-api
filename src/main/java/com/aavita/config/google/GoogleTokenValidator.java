package com.aavita.config.google;

import com.aavita.oauth.service.OAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates Bearer tokens sent by Google in fulfillment requests.
 * Uses YOUR OAuth DB table — NOT Google's tokeninfo API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenValidator {

    private final OAuthService oAuthService;

    public boolean validate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Google fulfillment: Missing or malformed Authorization header");
            return false;
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isBlank()) {
            log.warn("Google fulfillment: Empty token");
            return false;
        }

        // Validate against your oauth_tokens table
        boolean valid = oAuthService.validateAccessToken(token).isPresent();

        if (!valid) {
            log.warn("Google fulfillment: Token not found or expired in DB");
        } else {
            log.info("Google fulfillment: Token validated successfully");
        }

        return valid;
    }
}