package com.aavita.oauth.controller;

import com.aavita.oauth.service.OAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth 2.0 endpoints required by Google Smart Home Account Linking.
 *
 * Google Actions Console fields:
 *   Authorization URL → https://your-domain.com/oauth/authorize
 *   Token URL         → https://your-domain.com/oauth/token
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;

    // ----------------------------------------------------------------
    // GET /oauth/authorize
    // Google redirects user here to log in and grant access.
    // Returns an HTML login form — user submits credentials here.
    // ----------------------------------------------------------------
    @GetMapping("/authorize")
    public void authorize(
            @RequestParam("client_id")     String clientId,
            @RequestParam("redirect_uri")  String redirectUri,
            @RequestParam("state")         String state,
            @RequestParam(value = "response_type", defaultValue = "code") String responseType,
            HttpServletResponse response) throws IOException {

        // Validate client
        if (!oAuthService.isValidClient(clientId, redirectUri)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid client_id or redirect_uri");
            return;
        }

        // Return a simple login page — styled to match your app branding
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
                        .error { color: #d93025; font-size: 13px; margin-top: 12px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2>🏠 Smart Home</h2>
                        <p>Sign in to link your account with Google Home</p>
                        <form method="POST" action="/oauth/login">
                            <input type="hidden" name="client_id"    value="%s" />
                            <input type="hidden" name="redirect_uri" value="%s" />
                            <input type="hidden" name="state"        value="%s" />
                            <input type="text"     name="username" placeholder="Email or Username" required autofocus />
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
    // Handles login form submission. Validates credentials using your
    // existing auth logic, then redirects back to Google with auth code.
    // ----------------------------------------------------------------
    @PostMapping("/login")
    public void login(
            @RequestParam("username")     String username,
            @RequestParam("password")     String password,
            @RequestParam("client_id")    String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("state")        String state,
            HttpServletResponse response) throws IOException {

        // TODO: Replace with your existing user authentication logic
        // Example:
        //   Optional<User> user = userService.authenticate(username, password);
        //   if (user.isEmpty()) { ... show error ... }
        //   String userId = user.get().getId().toString();

        // ---- STUB: Replace this block with your real auth ----
        boolean isValidUser = true;           // TODO: validate credentials
        String userId = "user-001";           // TODO: get real userId from DB
        // ------------------------------------------------------

        if (!isValidUser) {
            response.sendRedirect("/oauth/authorize?client_id=" + clientId
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                    + "&state=" + state
                    + "&error=invalid_credentials");
            return;
        }

        // Generate auth code and redirect back to Google
        String authCode = oAuthService.generateAuthCode(userId, clientId, redirectUri);

        String redirectUrl = redirectUri
                + "?code=" + authCode
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

        log.info("OAuth: Redirecting to Google with auth code for userId: {}", userId);
        response.sendRedirect(redirectUrl);
    }

    // ----------------------------------------------------------------
    // POST /oauth/token
    // Google calls this to exchange auth code for access token.
    // Also handles token refresh requests.
    // ----------------------------------------------------------------
    @PostMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam("grant_type")              String grantType,
            @RequestParam(value = "code",            required = false) String code,
            @RequestParam(value = "client_id",       required = false) String clientId,
            @RequestParam(value = "client_secret",   required = false) String clientSecret,
            @RequestParam(value = "redirect_uri",    required = false) String redirectUri,
            @RequestParam(value = "refresh_token",   required = false) String refreshToken) {

        log.info("OAuth: Token request - grant_type: {}", grantType);

        if ("authorization_code".equals(grantType)) {
            // Exchange auth code for access token
            Optional<String> accessToken = oAuthService.exchangeAuthCodeForToken(
                    code, clientId, clientSecret, redirectUri);

            if (accessToken.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "invalid_grant",
                                     "error_description", "Invalid or expired authorization code"));
            }

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("access_token",  accessToken.get());
            responseBody.put("token_type",    "Bearer");
            responseBody.put("expires_in",    3600);          // 1 hour in seconds
            // Note: For simplicity we reuse access token as refresh token
            // For production, generate a separate long-lived refresh token
            responseBody.put("refresh_token", accessToken.get());

            return ResponseEntity.ok(responseBody);

        } else if ("refresh_token".equals(grantType)) {
            // Validate existing token (reuse as refresh)
            Optional<String> userId = oAuthService.validateAccessToken(refreshToken);

            if (userId.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "invalid_grant",
                                     "error_description", "Invalid or expired refresh token"));
            }

            // Issue new access token
            // For simplicity, reuse same token if still valid
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("access_token",  refreshToken);
            responseBody.put("token_type",    "Bearer");
            responseBody.put("expires_in",    3600);

            return ResponseEntity.ok(responseBody);

        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "unsupported_grant_type"));
        }
    }
}