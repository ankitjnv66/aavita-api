package com.aavita.oauth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores OAuth authorization codes and access tokens issued to Google.
 * One table handles both — differentiated by the 'tokenType' field.
 */
@Entity
@Table(name = "oauth_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "AUTH_CODE" or "ACCESS_TOKEN"
    @Column(nullable = false)
    private String tokenType;

    @Column(nullable = false, unique = true)
    private String tokenValue;

    // The user this token belongs to (matches your existing users table)
    @Column(nullable = false)
    private String userId;

    // Google's client ID (from Actions Console)
    @Column(nullable = false)
    private String clientId;

    // For auth codes: the redirect_uri Google sent
    private String redirectUri;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}