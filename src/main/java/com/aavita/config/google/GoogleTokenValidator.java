package com.aavita.config.google;

import com.aavita.oauth.service.OAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Validates Bearer tokens sent by Google in fulfillment requests.
 * Uses YOUR OAuth DB table — NOT Google's tokeninfo API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenValidator {

    private final OAuthService oAuthService;

    /**
     * Existing behaviour: true if the token is valid, false otherwise.
     */
    public boolean validate(String authorizationHeader) {
        return resolveUserId(authorizationHeader).isPresent();
    }

    /**
     * Validates the Bearer token and returns the numeric ID of the user it belongs to.
     * Returns empty if the header is missing or malformed, the token is unknown or expired,
     * or the stored user id cannot be read as a number.
     */
    public Optional<Long> resolveUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Google fulfillment: Missing or malformed Authorization header");
            return Optional.empty();
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isBlank()) {
            log.warn("Google fulfillment: Empty token");
            return Optional.empty();
        }

        // Look the token up in the oauth_tokens table
        Optional<String> storedUserId = oAuthService.validateAccessToken(token);

        if (storedUserId.isEmpty()) {
            log.warn("Google fulfillment: Token not found or expired in DB");
            return Optional.empty();
        }

        // oauth_tokens.user_id is a String; accept both "4" and "user-4"
        String raw = storedUserId.get().trim();
        if (raw.startsWith("user-")) {
            raw = raw.substring(5);
        }

        try {
            Long userId = Long.parseLong(raw);
            log.info("Google fulfillment: Token validated successfully for userId: {}", userId);
            return Optional.of(userId);
        } catch (NumberFormatException e) {
            log.warn("Google fulfillment: Token user id is not numeric: {}", storedUserId.get());
            return Optional.empty();
        }
    }
}