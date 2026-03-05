package com.aavita.controller;

import com.aavita.config.google.GoogleTokenValidator;
import com.aavita.service.google.GoogleSmartHomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Single entry point for all Google Smart Home fulfillment requests.
 * Handles SYNC, QUERY, and EXECUTE intents from Google Assistant / Google Home.
 *
 * Security: Google's Bearer token is validated via GoogleTokenValidator
 * (separate from your app's JwtAuthenticationFilter).
 */
@Slf4j
@RestController
@RequestMapping("/api/google")
@RequiredArgsConstructor
public class FulfillmentController {

    private final GoogleSmartHomeService smartHomeService;
    private final GoogleTokenValidator tokenValidator;

    /**
     * POST /api/google/fulfillment
     * Google calls this endpoint for all smart home actions.
     */
    @PostMapping("/fulfillment")
    public ResponseEntity<Map<String, Object>> fulfillment(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Step 1: Validate Google's OAuth token
        if (!tokenValidator.validate(authHeader)) {
            log.warn("Fulfillment request rejected: invalid Google token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED"));
        }

        // Step 2: Extract intent from request
        String requestId = (String) request.get("requestId");
        List<Map<String, Object>> inputs = (List<Map<String, Object>>) request.get("inputs");

        if (inputs == null || inputs.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("requestId", requestId, "error", "No inputs provided"));
        }

        String intent = (String) inputs.get(0).get("intent");
        log.info("Fulfillment intent received: {} | requestId: {}", intent, requestId);

        // Step 3: Route to appropriate handler
        Map<String, Object> response = switch (intent) {
            case "action.devices.SYNC"    -> smartHomeService.handleSync(requestId);
            case "action.devices.QUERY"   -> smartHomeService.handleQuery(requestId, inputs);
            case "action.devices.EXECUTE" -> smartHomeService.handleExecute(requestId, inputs);
            default -> {
                log.warn("Unknown intent received: {}", intent);
                yield Map.of("requestId", requestId, "error", "Unknown intent: " + intent);
            }
        };

        return ResponseEntity.ok(response);
    }
}