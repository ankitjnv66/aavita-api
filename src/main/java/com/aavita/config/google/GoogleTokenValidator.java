package com.aavita.config.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Validates Google-issued OAuth Bearer tokens by calling Google's tokeninfo endpoint.
 * This is separate from your app's JwtAuthenticationFilter.
 */
@Component
public class GoogleTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenValidator.class);
    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?access_token=";

    @Value("${google.smarthome.client-id}")
    private String expectedClientId;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validates the Bearer token sent by Google in fulfillment requests.
     *
     * @param authorizationHeader Full "Bearer <token>" header value
     * @return true if token is valid and belongs to your Google Actions project
     */
    public boolean validate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Google fulfillment: Missing or malformed Authorization header");
            return false;
        }

        String token = authorizationHeader.substring(7);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_TOKEN_INFO_URL + token))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Google fulfillment: Token validation failed with status {}", response.statusCode());
                return false;
            }

            Map<String, Object> tokenInfo = objectMapper.readValue(response.body(), Map.class);

            // Verify the token was issued for your Google Actions project client ID
            String audience = (String) tokenInfo.get("aud");
            if (!expectedClientId.equals(audience)) {
                log.warn("Google fulfillment: Token audience mismatch. Expected: {}, Got: {}", expectedClientId, audience);
                return false;
            }

            log.debug("Google fulfillment: Token validated successfully");
            return true;

        } catch (Exception e) {
            log.error("Google fulfillment: Token validation error", e);
            return false;
        }
    }
}