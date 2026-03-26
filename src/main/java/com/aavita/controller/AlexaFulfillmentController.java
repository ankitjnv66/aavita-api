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
 * Alexa sends directives in this format:
 * {
 *   "directive": {
 *     "header": {
 *       "namespace": "Alexa.Discovery" | "Alexa.PowerController" | etc.,
 *       "name":      "Discover"        | "TurnOn" | "TurnOff"    | etc.,
 *       "messageId": "uuid"
 *     },
 *     "endpoint": { "endpointId": "light-1" },       // present for control directives
 *     "payload":  { "scope": { "token": "<jwt>" } }  // JWT from account linking
 *   }
 * }
 *
 * Security:
 *   - X-Alexa-Secret header validated against alexa.shared-secret in application.yml
 *   - JWT token extracted from directive.payload.scope.token and validated via JwtUtil
 *     to identify which user's devices to control
 *
 * Endpoint: POST /api/alexa/fulfillment  (permitAll in SecurityConfig — auth done here)
 */
@Slf4j
@RestController
@RequestMapping("/api/alexa")
@RequiredArgsConstructor
public class AlexaFulfillmentController {

    private final AlexaSmartHomeService alexaSmartHomeService;
    private final AlexaProperties       alexaProperties;

    @PostMapping("/fulfillment")
    public ResponseEntity<Map<String, Object>> fulfillment(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Alexa-Secret", required = false) String alexaSecret) {

        // Step 1: Validate shared secret — confirms request is from Alexa (or your test curl)
        if (alexaProperties.getSharedSecret() == null ||
                !alexaProperties.getSharedSecret().equals(alexaSecret)) {
            log.warn("Alexa fulfillment rejected: invalid or missing X-Alexa-Secret header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED"));
        }

        // Step 2: Extract directive
        Map<String, Object> directive = (Map<String, Object>) request.get("directive");
        if (directive == null) {
            log.warn("Alexa fulfillment rejected: missing directive");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing directive"));
        }

        Map<String, Object> header  = (Map<String, Object>) directive.get("header");
        String namespace            = (String) header.get("namespace");
        String name                 = (String) header.get("name");
        String messageId            = (String) header.get("messageId");

        log.info("Alexa directive received: {}/{} | messageId: {}", namespace, name, messageId);

        // Step 3: Route by namespace
        Map<String, Object> response = switch (namespace) {
            case "Alexa.Discovery"          -> alexaSmartHomeService.handleDiscovery(directive);
            case "Alexa.PowerController"    -> alexaSmartHomeService.handlePowerController(directive);
            case "Alexa.BrightnessController" -> alexaSmartHomeService.handleBrightnessController(directive);
            case "Alexa.RangeController"    -> alexaSmartHomeService.handleRangeController(directive);
            case "Alexa.ThermostatController" -> alexaSmartHomeService.handleThermostatController(directive);
            case "Alexa"                    -> alexaSmartHomeService.handleStateReport(directive);
            default -> {
                log.warn("Unknown Alexa namespace: {}/{}", namespace, name);
                yield alexaSmartHomeService.buildErrorResponse(messageId, "INVALID_DIRECTIVE",
                        "Unknown namespace: " + namespace);
            }
        };

        return ResponseEntity.ok(response);
    }
}