package com.aavita.controller;

import com.aavita.config.alexa.AlexaProperties;
import com.aavita.service.alexa.AlexaSmartHomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Single entry point for all Alexa Smart Home fulfillment requests.
 *
 * Auth strategy (dual mode):
 *   - Dev/curl:      X-Alexa-Secret header matches alexa.shared-secret → allowed
 *   - Real Alexa:    No X-Alexa-Secret, but Bearer token present in directive payload/endpoint
 *                    → token validated inside AlexaSmartHomeService via OAuthService
 *
 * Endpoint: POST /api/alexa/fulfillment  (permitAll in SecurityConfig)
 */
@Slf4j
@RestController
@RequestMapping("/api/alexa")
@RequiredArgsConstructor
public class AlexaFulfillmentController {

    private final AlexaSmartHomeService alexaSmartHomeService;
    private final AlexaProperties       alexaProperties;

    @PostMapping("/fulfillment")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> fulfillment(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Alexa-Secret", required = false) String alexaSecret) {

        // ── Step 1: Parse directive ────────────────────────────────────────────
        Map<String, Object> directive = (Map<String, Object>) request.get("directive");
        if (directive == null) {
            log.warn("Alexa fulfillment rejected: missing directive");
            return ResponseEntity.badRequest().body(Map.of("error", "Missing directive"));
        }

        Map<String, Object> header = (Map<String, Object>) directive.get("header");
        String namespace = (String) header.get("namespace");
        String name      = (String) header.get("name");
        String messageId = (String) header.get("messageId");

        // ── Step 2: Auth ───────────────────────────────────────────────────────
        // Path A: curl / dev testing — X-Alexa-Secret header present and matches
        boolean authenticatedBySecret = alexaProperties.getSharedSecret() != null
                && alexaProperties.getSharedSecret().equals(alexaSecret);

        if (!authenticatedBySecret) {
            // Path B: real Alexa cloud — no X-Alexa-Secret, must have a Bearer token
            String token = extractToken(directive, namespace);
            if (token == null || "dummy".equals(token)) {
                log.warn("Alexa fulfillment rejected: no valid secret or token | ns: {}", namespace);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "UNAUTHORIZED"));
            }
            // Token is validated inside each service method via OAuthService.validateAccessToken()
            log.info("Alexa: authenticated via Bearer token");
        } else {
            log.info("Alexa: authenticated via X-Alexa-Secret (dev/test mode)");
        }

        // ── Step 3: Route by namespace ─────────────────────────────────────────
        log.info("Alexa directive: {}/{} | messageId: {}", namespace, name, messageId);

        Map<String, Object> response = switch (namespace) {
            case "Alexa.Discovery"            -> alexaSmartHomeService.handleDiscovery(directive);
            case "Alexa.PowerController"      -> alexaSmartHomeService.handlePowerController(directive);
            case "Alexa.BrightnessController" -> alexaSmartHomeService.handleBrightnessController(directive);
            case "Alexa.RangeController"      -> alexaSmartHomeService.handleRangeController(directive);
            case "Alexa.ThermostatController" -> alexaSmartHomeService.handleThermostatController(directive);
            case "Alexa"                      -> alexaSmartHomeService.handleStateReport(directive);
            default -> {
                log.warn("Unknown Alexa namespace: {}/{}", namespace, name);
                yield alexaSmartHomeService.buildErrorResponse(messageId,
                        "INVALID_DIRECTIVE", "Unknown namespace: " + namespace);
            }
        };

        return ResponseEntity.ok(response);
    }

    /**
     * Extracts the Bearer token from wherever Alexa puts it:
     *   Discovery:         directive.payload.scope.token
     *   Control directives: directive.endpoint.scope.token
     */
    @SuppressWarnings("unchecked")
    private String extractToken(Map<String, Object> directive, String namespace) {
        try {
            if ("Alexa.Discovery".equals(namespace)) {
                Map<String, Object> payload = (Map<String, Object>) directive.get("payload");
                Map<String, Object> scope   = (Map<String, Object>) payload.get("scope");
                return (String) scope.get("token");
            } else {
                Map<String, Object> endpoint = (Map<String, Object>) directive.get("endpoint");
                Map<String, Object> scope    = (Map<String, Object>) endpoint.get("scope");
                return (String) scope.get("token");
            }
        } catch (Exception e) {
            log.warn("Could not extract token from directive: {}", e.getMessage());
            return null;
        }
    }
}