package com.aavita.oauth.service;

import com.aavita.oauth.repository.OAuthTokenRepository;
import com.aavita.oauth.model.OAuthToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the full OAuth 2.0 Authorization Code flow for Google Smart Home.
 *
 * Flow:
 *  1. Google redirects user to /oauth/authorize
 *  2. User logs in → we generate an AUTH_CODE and redirect back to Google
 *  3. Google calls /oauth/token with the AUTH_CODE
 *  4. We exchange it for an ACCESS_TOKEN and return it to Google
 *  5. Google uses ACCESS_TOKEN in all future fulfillment requests
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final OAuthTokenRepository tokenRepository;

    @Value("${google.smarthome.client-id}")
    private String expectedClientId;

    @Value("${google.smarthome.client-secret}")
    private String expectedClientSecret;

    // Auth codes expire in 10 minutes (Google requirement)
    private static final int AUTH_CODE_EXPIRY_MINUTES = 10;

    // Access tokens expire in 1 hour
    private static final int ACCESS_TOKEN_EXPIRY_HOURS = 1;

    // ----------------------------------------------------------------
    // Step 1: Validate incoming authorize request from Google
    // ----------------------------------------------------------------
    public boolean isValidClient(String clientId, String redirectUri) {
        if (!expectedClientId.equals(clientId)) {
            log.warn("OAuth: Invalid client_id: {}", clientId);
            return false;
        }
        // Optional: validate redirectUri against whitelist
        if (redirectUri == null || redirectUri.isBlank()) {
            log.warn("OAuth: Missing redirect_uri");
            return false;
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Step 2: Generate authorization code after user logs in
    // ----------------------------------------------------------------
    @Transactional
    public String generateAuthCode(String userId, String clientId, String redirectUri) {
        String code = UUID.randomUUID().toString().replace("-", "");

        OAuthToken authCode = OAuthToken.builder()
                .tokenType("AUTH_CODE")
                .tokenValue(code)
                .userId(userId)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .expiresAt(LocalDateTime.now().plusMinutes(AUTH_CODE_EXPIRY_MINUTES))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        tokenRepository.save(authCode);
        log.info("OAuth: Auth code generated for userId: {}", userId);
        return code;
    }

    // ----------------------------------------------------------------
    // Step 3: Exchange auth code for access token
    // ----------------------------------------------------------------
    @Transactional
    public Optional<String> exchangeAuthCodeForToken(
            String code, String clientId, String clientSecret, String redirectUri) {

        // Validate client credentials
        if (!expectedClientId.equals(clientId) || !expectedClientSecret.equals(clientSecret)) {
            log.warn("OAuth: Invalid client credentials during token exchange");
            return Optional.empty();
        }

        // Find and validate auth code
        Optional<OAuthToken> authCodeOpt = tokenRepository
                .findByTokenValueAndTokenType(code, "AUTH_CODE");

        if (authCodeOpt.isEmpty()) {
            log.warn("OAuth: Auth code not found: {}", code);
            return Optional.empty();
        }

        OAuthToken authCode = authCodeOpt.get();

        // Check if already used (replay attack prevention)
        if (authCode.isUsed()) {
            log.warn("OAuth: Auth code already used: {}", code);
            return Optional.empty();
        }

        // Check expiry
        if (authCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OAuth: Auth code expired: {}", code);
            tokenRepository.delete(authCode);
            return Optional.empty();
        }

        // Validate redirect URI matches
        if (!authCode.getRedirectUri().equals(redirectUri)) {
            log.warn("OAuth: Redirect URI mismatch");
            return Optional.empty();
        }

        // Mark auth code as used
        authCode.setUsed(true);
        tokenRepository.save(authCode);

        // Generate access token
        String accessToken = UUID.randomUUID().toString().replace("-", "");

        OAuthToken token = OAuthToken.builder()
                .tokenType("ACCESS_TOKEN")
                .tokenValue(accessToken)
                .userId(authCode.getUserId())
                .clientId(clientId)
                .redirectUri(redirectUri)
                .expiresAt(LocalDateTime.now().plusHours(ACCESS_TOKEN_EXPIRY_HOURS))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        tokenRepository.save(token);
        log.info("OAuth: Access token issued for userId: {}", authCode.getUserId());

        return Optional.of(accessToken);
    }

    // ----------------------------------------------------------------
    // Step 4: Validate access token on fulfillment requests
    // ----------------------------------------------------------------
    public Optional<String> validateAccessToken(String tokenValue) {
        return tokenRepository
                .findByTokenValueAndTokenType(tokenValue, "ACCESS_TOKEN")
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(OAuthToken::getUserId);
    }
}