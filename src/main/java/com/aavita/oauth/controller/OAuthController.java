package com.aavita.oauth.controller;

import com.aavita.entity.User;
import com.aavita.oauth.service.OAuthService;
import com.aavita.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth 2.0 endpoints required by Google Smart Home and Alexa Account Linking.
 *
 * Google Actions Console / Alexa Developer Console fields:
 *   Authorization URL → https://your-domain.com/oauth/authorize
 *   Token URL         → https://your-domain.com/oauth/token
 *
 * Notes:
 *   - Alexa sends client_id + client_secret via HTTP Basic Auth on /oauth/token
 *   - Google sends them as form params — both are handled here
 *   - /oauth/login uses real BCrypt password validation via UserRepository
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService     oAuthService;
    private final UserRepository   userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // ----------------------------------------------------------------
    // GET /oauth/authorize
    // Alexa / Google redirect user here to log in and grant access.
    // Returns an HTML login form.
    // ----------------------------------------------------------------
    @GetMapping("/authorize")
    public void authorize(
            @RequestParam("client_id")     String clientId,
            @RequestParam("redirect_uri")  String redirectUri,
            @RequestParam("state")         String state,
            @RequestParam(value = "response_type", defaultValue = "code") String responseType,
            HttpServletResponse response) throws IOException {

        if (!oAuthService.isValidClient(clientId, redirectUri)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid client_id or redirect_uri");
            return;
        }

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Link Your Smart Home Account</title>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                            background: #f5f5f5;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                        }
                        .card {
                            background: white;
                            border-radius: 12px;
                            padding: 40px;
                            width: 100%%;
                            max-width: 400px;
                            box-shadow: 0 4px 20px rgba(0,0,0,0.1);
                        }
                        h2 { color: #1a73e8; margin-bottom: 8px; font-size: 22px; }
                        p  { color: #666; margin-bottom: 28px; font-size: 14px; }
                        input {
                            width: 100%%;
                            padding: 12px 16px;
                            border: 1px solid #ddd;
                            border-radius: 8px;
                            font-size: 15px;
                            margin-bottom: 16px;
                            outline: none;
                            transition: border 0.2s;
                        }
                        input:focus { border-color: #1a73e8; }
                        button {
                            width: 100%%;
                            padding: 13px;
                            background: #1a73e8;
                            color: white;
                            border: none;
                            border-radius: 8px;
                            font-size: 16px;
                            cursor: pointer;
                            font-weight: 600;
                        }
                        button:hover { background: #1557b0; }
                        .error { color: #d93025; font-size: 13px; margin-top: 12px; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2>🏠 Aavita Smart Home</h2>
                        <p>Sign in to link your account</p>
                        <form method="POST" action="/oauth/login">
                            <input type="hidden" name="client_id"    value="%s" />
                            <input type="hidden" name="redirect_uri" value="%s" />
                            <input type="hidden" name="state"        value="%s" />
                            <input type="email"    name="username" placeholder="Email" required autofocus />
                            <input type="password" name="password" placeholder="Password" required />
                            <button type="submit">Sign In & Link Account</button>
                        </form>
                    </div>
                </body>
                </html>
                """.formatted(clientId, redirectUri, state);

        response.setContentType("text/html");
        response.getWriter().write(html);
    }

    // ----------------------------------------------------------------
    // POST /oauth/login
    // Handles login form submission. Validates credentials via
    // UserRepository + BCrypt, then redirects back with auth code.
    // ----------------------------------------------------------------
    @PostMapping("/login")
    public void login(
            @RequestParam("username")     String username,
            @RequestParam("password")     String password,
            @RequestParam("client_id")    String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("state")        String state,
            HttpServletResponse response) throws IOException {

        String errorRedirect = "/oauth/authorize?client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&state=" + state
                + "&error=invalid_credentials";

        // Real auth — look up user by email and verify password
        Optional<User> userOpt = userRepository.findByEmail(username);
        if (userOpt.isEmpty()) {
            log.warn("OAuth login failed: user not found for email: {}", username);
            response.sendRedirect(errorRedirect);
            return;
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("OAuth login failed: wrong password for email: {}", username);
            response.sendRedirect(errorRedirect);
            return;
        }

        String userId  = user.getId().toString();
        String authCode = oAuthService.generateAuthCode(userId, clientId, redirectUri);

        String redirectUrl = redirectUri
                + "?code="  + authCode
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

        log.info("OAuth login success: userId={} → redirecting with auth code", userId);
        response.sendRedirect(redirectUrl);
    }

    // ----------------------------------------------------------------
    // POST /oauth/token
    // Exchanges auth code for access token (authorization_code flow)
    // or validates refresh token (refresh_token flow).
    //
    // Supports both:
    //   - Form params: client_id + client_secret (Google)
    //   - HTTP Basic Auth header (Alexa)
    // ----------------------------------------------------------------
    @PostMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> token(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("grant_type")                               String grantType,
            @RequestParam(value = "code",          required = false)  String code,
            @RequestParam(value = "client_id",     required = false)  String clientId,
            @RequestParam(value = "client_secret", required = false)  String clientSecret,
            @RequestParam(value = "redirect_uri",  required = false)  String redirectUri,
            @RequestParam(value = "refresh_token", required = false)  String refreshToken) {

        // Alexa sends client credentials via HTTP Basic Auth header — extract if present
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String decoded = new String(Base64.getDecoder().decode(authHeader.substring(6)));
                String[] parts = decoded.split(":", 2);
                if (parts.length == 2) {
                    clientId     = parts[0];
                    clientSecret = parts[1];
                    log.info("OAuth: client credentials extracted from Basic Auth header");
                }
            } catch (Exception e) {
                log.warn("OAuth: failed to decode Basic Auth header: {}", e.getMessage());
            }
        }

        log.info("OAuth: Token request — grant_type: {}, clientId: {}", grantType, clientId);

        if ("authorization_code".equals(grantType)) {
            Optional<String> accessToken = oAuthService.exchangeAuthCodeForToken(
                    code, clientId, clientSecret, redirectUri);

            if (accessToken.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "invalid_grant",
                                "error_description", "Invalid or expired authorization code"));
            }

            Map<String, Object> body = new HashMap<>();
            body.put("access_token",  accessToken.get());
            body.put("token_type",    "Bearer");
            body.put("expires_in",    3600);
            body.put("refresh_token", accessToken.get()); // reused as refresh for simplicity
            return ResponseEntity.ok(body);

        } else if ("refresh_token".equals(grantType)) {
            Optional<String> userId = oAuthService.validateAccessToken(refreshToken);

            if (userId.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "invalid_grant",
                                "error_description", "Invalid or expired refresh token"));
            }

            Map<String, Object> body = new HashMap<>();
            body.put("access_token",  refreshToken); // still valid, reuse
            body.put("token_type",    "Bearer");
            body.put("expires_in",    3600);
            return ResponseEntity.ok(body);

        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "unsupported_grant_type"));
        }
    }
}