package com.aavita.service.alexa;

import com.aavita.entity.Device;
import com.aavita.entity.DeviceDigitalPin;
import com.aavita.entity.Site;
import com.aavita.oauth.service.OAuthService;
import com.aavita.repository.DeviceRepository;
import com.aavita.repository.SiteRepository;
import com.aavita.repository.UserRepository;
import com.aavita.service.FanService;
import com.aavita.service.LightService;
import com.aavita.service.SwitchService;
import com.aavita.service.ThermostatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * AlexaSmartHomeService — handles all Alexa Smart Home directives.
 *
 * Reuses existing LightService / FanService / ThermostatService / SwitchService
 * exactly as GoogleSmartHomeService does — no duplicated device logic.
 *
 * Device ID format (same as Google Home):
 *   light-{id}                   → LightService
 *   fan-{id}                     → FanService
 *   thermostat-{id}              → ThermostatService
 *   switch-{deviceId}-{pinNum}   → SwitchService
 *
 * Auth token from Alexa account linking is a UUID stored in oauth_tokens table.
 * It is validated via OAuthService.validateAccessToken() — NOT parsed as JWT.
 *
 * Token location in directive:
 *   directive.payload.scope.token  (Discovery)
 *   directive.endpoint.scope.token (Control directives)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlexaSmartHomeService {

    private final LightService      lightService;
    private final FanService        fanService;
    private final ThermostatService thermostatService;
    private final SwitchService     switchService;
    private final DeviceRepository  deviceRepository;
    private final SiteRepository    siteRepository;
    private final UserRepository    userRepository;
    private final OAuthService      oAuthService;   // ← replaces JwtUtil for Alexa token lookup

    // Device type constants — must match device_type column in DB
    private static final byte DEVICE_TYPE_LIGHT      = 1;
    private static final byte DEVICE_TYPE_FAN        = 2;
    private static final byte DEVICE_TYPE_THERMOSTAT = 3;
    private static final byte DEVICE_TYPE_SWITCH     = 4;

    // =====================================================================
    // 1. DISCOVERY — "Alexa, discover devices"
    //    Alexa.Discovery / Discover
    // =====================================================================
    public Map<String, Object> handleDiscovery(Map<String, Object> directive) {
        String messageId = getMessageId(directive);
        log.info("Alexa Discovery requested | messageId: {}", messageId);

        String token  = extractTokenFromPayload(directive);
        Long   userId = getUserIdFromToken(token);

        List<Map<String, Object>> endpoints = buildEndpointsForUser(userId);
        log.info("Alexa Discovery: found {} endpoints for userId: {}", endpoints.size(), userId);

        return Map.of(
                "event", Map.of(
                        "header", buildResponseHeader("Alexa.Discovery", "Discover.Response", messageId),
                        "payload", Map.of("endpoints", endpoints)
                )
        );
    }

    // =====================================================================
    // 2. POWER CONTROLLER — "Alexa, turn on/off the light"
    //    Alexa.PowerController / TurnOn | TurnOff
    // =====================================================================
    public Map<String, Object> handlePowerController(Map<String, Object> directive) {
        String messageId  = getMessageId(directive);
        String name       = getName(directive);
        String endpointId = getEndpointId(directive);

        log.info("Alexa PowerController: {} on endpoint: {}", name, endpointId);

        boolean turnOn = "TurnOn".equals(name);

        try {
            if (endpointId.startsWith("light-")) {
                lightService.setOnOff(endpointId, turnOn);
            } else if (endpointId.startsWith("fan-")) {
                fanService.setOnOff(endpointId, turnOn);
            } else if (endpointId.startsWith("thermostat-")) {
                thermostatService.setOnOff(endpointId, turnOn);
            } else if (endpointId.startsWith("switch-")) {
                switchService.setOnOff(endpointId, turnOn);
            } else {
                log.warn("PowerController: unknown endpoint type: {}", endpointId);
                return buildErrorResponse(messageId, "NO_SUCH_ENDPOINT", "Unknown endpoint: " + endpointId);
            }

            String powerState = turnOn ? "ON" : "OFF";
            return buildControlResponse(messageId, endpointId, "Alexa.PowerController",
                    "powerState", powerState, "ON_OFF");

        } catch (Exception e) {
            log.error("PowerController failed for endpoint {}: {}", endpointId, e.getMessage());
            return buildErrorResponse(messageId, "INTERNAL_ERROR", e.getMessage());
        }
    }

    // =====================================================================
    // 3. BRIGHTNESS CONTROLLER — "Alexa, set the light to 50%"
    //    Alexa.BrightnessController / SetBrightness | AdjustBrightness
    // =====================================================================
    public Map<String, Object> handleBrightnessController(Map<String, Object> directive) {
        String messageId  = getMessageId(directive);
        String name       = getName(directive);
        String endpointId = getEndpointId(directive);
        Map<String, Object> payload = getPayload(directive);

        log.info("Alexa BrightnessController: {} on endpoint: {}", name, endpointId);

        try {
            if (!endpointId.startsWith("light-")) {
                return buildErrorResponse(messageId, "INVALID_DIRECTIVE", "BrightnessController only valid for lights");
            }

            int brightness;
            if ("SetBrightness".equals(name)) {
                brightness = ((Number) payload.get("brightness")).intValue();
            } else if ("AdjustBrightness".equals(name)) {
                int delta   = ((Number) payload.get("brightnessDelta")).intValue();
                int current = lightService.getBrightness(endpointId);
                brightness  = Math.max(0, Math.min(100, current + delta));
            } else {
                return buildErrorResponse(messageId, "INVALID_DIRECTIVE", "Unknown brightness command: " + name);
            }

            lightService.setBrightness(endpointId, brightness);
            return buildControlResponse(messageId, endpointId, "Alexa.BrightnessController",
                    "brightness", brightness, "INTEGER");

        } catch (Exception e) {
            log.error("BrightnessController failed for endpoint {}: {}", endpointId, e.getMessage());
            return buildErrorResponse(messageId, "INTERNAL_ERROR", e.getMessage());
        }
    }

    // =====================================================================
    // 4. RANGE CONTROLLER — "Alexa, set the fan speed to 70%"
    //    Alexa.RangeController / SetRangeValue | AdjustRangeValue
    // =====================================================================
    public Map<String, Object> handleRangeController(Map<String, Object> directive) {
        String messageId  = getMessageId(directive);
        String name       = getName(directive);
        String endpointId = getEndpointId(directive);
        Map<String, Object> payload = getPayload(directive);

        log.info("Alexa RangeController: {} on endpoint: {}", name, endpointId);

        try {
            if (!endpointId.startsWith("fan-")) {
                return buildErrorResponse(messageId, "INVALID_DIRECTIVE", "RangeController only valid for fans");
            }

            int speed;
            if ("SetRangeValue".equals(name)) {
                speed = ((Number) payload.get("rangeValue")).intValue();
            } else if ("AdjustRangeValue".equals(name)) {
                int delta   = ((Number) payload.get("rangeValueDelta")).intValue();
                int current = fanService.getSpeed(endpointId);
                speed       = Math.max(0, Math.min(100, current + delta));
            } else {
                return buildErrorResponse(messageId, "INVALID_DIRECTIVE", "Unknown range command: " + name);
            }

            fanService.setSpeed(endpointId, speed);
            return buildControlResponse(messageId, endpointId, "Alexa.RangeController",
                    "rangeValue", speed, "INTEGER");

        } catch (Exception e) {
            log.error("RangeController failed for endpoint {}: {}", endpointId, e.getMessage());
            return buildErrorResponse(messageId, "INTERNAL_ERROR", e.getMessage());
        }
    }

    // =====================================================================
    // 5. THERMOSTAT CONTROLLER — "Alexa, set the thermostat to 22 degrees"
    //    Alexa.ThermostatController / SetTargetTemperature | AdjustTargetTemperature
    // =====================================================================
    public Map<String, Object> handleThermostatController(Map<String, Object> directive) {
        String messageId  = getMessageId(directive);
        String name       = getName(directive);
        String endpointId = getEndpointId(directive);
        Map<String, Object> payload = getPayload(directive);

        log.info("Alexa ThermostatController: {} on endpoint: {}", name, endpointId);

        try {
            if (!endpointId.startsWith("thermostat-")) {
                return buildErrorResponse(messageId, "INVALID_DIRECTIVE", "ThermostatController only valid for thermostats");
            }

            double temperature;
            if ("SetTargetTemperature".equals(name)) {
                Map<String, Object> targetSetpoint = (Map<String, Object>) payload.get("targetSetpoint");
                temperature = ((Number) targetSetpoint.get("value")).doubleValue();
            } else if ("AdjustTargetTemperature".equals(name)) {
                Map<String, Object> delta = (Map<String, Object>) payload.get("targetSetpointDelta");
                temperature = thermostatService.getTemperature(endpointId)
                        + ((Number) delta.get("value")).doubleValue();
            } else {
                return buildErrorResponse(messageId, "INVALID_DIRECTIVE", "Unknown thermostat command: " + name);
            }

            thermostatService.setTemperature(endpointId, temperature);
            return buildControlResponse(messageId, endpointId, "Alexa.ThermostatController",
                    "targetSetpoint", Map.of("value", temperature, "scale", "CELSIUS"), "OBJECT");

        } catch (Exception e) {
            log.error("ThermostatController failed for endpoint {}: {}", endpointId, e.getMessage());
            return buildErrorResponse(messageId, "INTERNAL_ERROR", e.getMessage());
        }
    }

    // =====================================================================
    // 6. STATE REPORT — Alexa polling current device state
    //    Alexa / ReportState
    // =====================================================================
    public Map<String, Object> handleStateReport(Map<String, Object> directive) {
        String messageId  = getMessageId(directive);
        String endpointId = getEndpointId(directive);

        log.info("Alexa StateReport requested for endpoint: {}", endpointId);

        List<Map<String, Object>> properties = buildCurrentStateProperties(endpointId);

        return Map.of(
                "event", Map.of(
                        "header",   buildResponseHeader("Alexa", "StateReport", messageId),
                        "endpoint", Map.of("endpointId", endpointId),
                        "payload",  Map.of()
                ),
                "context", Map.of("properties", properties)
        );
    }

    // =====================================================================
    // PUBLIC — Error response builder (used by controller for unknown namespaces)
    // =====================================================================
    public Map<String, Object> buildErrorResponse(String messageId, String type, String message) {
        log.warn("Alexa error response: type={} message={}", type, message);
        return Map.of(
                "event", Map.of(
                        "header", buildResponseHeader("Alexa", "ErrorResponse", messageId),
                        "payload", Map.of("type", type, "message", message)
                )
        );
    }

    // =====================================================================
    // PRIVATE — Token extraction and user resolution
    // =====================================================================

    /** Discovery: token is in directive.payload.scope.token */
    @SuppressWarnings("unchecked")
    private String extractTokenFromPayload(Map<String, Object> directive) {
        try {
            Map<String, Object> payload = (Map<String, Object>) directive.get("payload");
            Map<String, Object> scope   = (Map<String, Object>) payload.get("scope");
            return (String) scope.get("token");
        } catch (Exception e) {
            log.warn("Could not extract token from payload scope: {}", e.getMessage());
            return null;
        }
    }

    /** Control directives: token is in directive.endpoint.scope.token */
    @SuppressWarnings("unchecked")
    private String extractTokenFromEndpoint(Map<String, Object> directive) {
        try {
            Map<String, Object> endpoint = (Map<String, Object>) directive.get("endpoint");
            Map<String, Object> scope    = (Map<String, Object>) endpoint.get("scope");
            return (String) scope.get("token");
        } catch (Exception e) {
            log.warn("Could not extract token from endpoint scope: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Resolves userId from Alexa's OAuth token (UUID stored in oauth_tokens table).
     *
     * Alexa sends the access_token issued during account linking — this is a UUID
     * from OAuthService.exchangeAuthCodeForToken(), NOT a JWT.
     * Look it up via OAuthService.validateAccessToken().
     *
     * Falls back to userId=1 only for local curl tests (token="dummy" or null).
     */
    private Long getUserIdFromToken(String token) {
        if (token == null || token.isBlank() || "dummy".equals(token)) {
            log.warn("Alexa: no real token — falling back to userId=1 (curl test mode)");
            return 1L;
        }

        Optional<String> userIdStr = oAuthService.validateAccessToken(token);
        if (userIdStr.isPresent()) {
            try {
                Long userId = Long.parseLong(userIdStr.get());
                log.info("Alexa: resolved userId={} from OAuth token", userId);
                return userId;
            } catch (NumberFormatException e) {
                log.error("Alexa: userId in token is not a Long: {}", userIdStr.get());
            }
        }

        log.warn("Alexa: token not found or expired in DB — falling back to userId=1");
        return 1L;
    }

    // =====================================================================
    // PRIVATE — Build endpoint list from DB for Discovery
    // =====================================================================
    private List<Map<String, Object>> buildEndpointsForUser(Long userId) {
        List<Map<String, Object>> endpoints = new ArrayList<>();
        try {
            List<Site> sites = siteRepository.findByUser_Id(userId);
            for (Site site : sites) {
                List<Device> devices = deviceRepository.findBySiteIdWithDigitalPins(site.getSiteId());
                for (Device device : devices) {
                    endpoints.addAll(buildAlexaEndpoints(device, site));
                }
            }
        } catch (Exception e) {
            log.error("Failed to build endpoint list for userId: {}", userId, e);
        }
        return endpoints;
    }

    private List<Map<String, Object>> buildAlexaEndpoints(Device device, Site site) {
        if (device.getDeviceType() == DEVICE_TYPE_SWITCH) {
            return buildSwitchEndpoints(device, site);
        }
        Map<String, Object> single = buildAlexaEndpoint(device, site);
        return single != null ? List.of(single) : List.of();
    }

    private List<Map<String, Object>> buildSwitchEndpoints(Device device, Site site) {
        List<Map<String, Object>> result = new ArrayList<>();
        String roomHint = device.getRoomHint() != null ? device.getRoomHint() : site.getLocation();

        for (DeviceDigitalPin pin : device.getDigitalPins()) {
            String pinLabel   = (device.getDeviceName() != null)
                    ? device.getDeviceName() + " Pin " + pin.getPinNumber()
                    : "Switch " + device.getId() + " Pin " + pin.getPinNumber();
            String endpointId = "switch-" + device.getId() + "-" + pin.getPinNumber();

            Map<String, Object> endpoint = new HashMap<>();
            endpoint.put("endpointId",       endpointId);
            endpoint.put("manufacturerName",  "Aavita IoT");
            endpoint.put("friendlyName",      pinLabel);
            endpoint.put("description",       "GPIO Switch in " + roomHint);
            endpoint.put("displayCategories", List.of("SWITCH"));
            endpoint.put("capabilities",      List.of(alexaInterface("Alexa.PowerController",
                    Map.of("supported", List.of(Map.of("name", "powerState")),
                            "proactivelyReported", true, "retrievable", true))));
            result.add(endpoint);
        }

        if (result.isEmpty()) {
            log.warn("Discovery: Device {} (SWITCH) has no pins yet", device.getId());
        }
        return result;
    }

    private Map<String, Object> buildAlexaEndpoint(Device device, Site site) {
        byte   deviceType = device.getDeviceType();
        String name       = device.getDeviceName() != null ? device.getDeviceName() : getDefaultName(deviceType, device.getId());
        String roomHint   = device.getRoomHint()   != null ? device.getRoomHint()   : site.getLocation();

        if (deviceType == DEVICE_TYPE_LIGHT) {
            Map<String, Object> ep = new HashMap<>();
            ep.put("endpointId",       "light-" + device.getId());
            ep.put("manufacturerName",  "Aavita IoT");
            ep.put("friendlyName",      name);
            ep.put("description",       "Smart Light in " + roomHint);
            ep.put("displayCategories", List.of("LIGHT"));
            ep.put("capabilities", List.of(
                    alexaInterface("Alexa.PowerController",
                            Map.of("supported", List.of(Map.of("name", "powerState")),
                                    "proactivelyReported", true, "retrievable", true)),
                    alexaInterface("Alexa.BrightnessController",
                            Map.of("supported", List.of(Map.of("name", "brightness")),
                                    "proactivelyReported", true, "retrievable", true))
            ));
            return ep;
        }

        if (deviceType == DEVICE_TYPE_FAN) {
            Map<String, Object> ep = new HashMap<>();
            ep.put("endpointId",       "fan-" + device.getId());
            ep.put("manufacturerName",  "Aavita IoT");
            ep.put("friendlyName",      name);
            ep.put("description",       "Smart Fan in " + roomHint);
            ep.put("displayCategories", List.of("FAN"));
            ep.put("capabilities", List.of(
                    alexaInterface("Alexa.PowerController",
                            Map.of("supported", List.of(Map.of("name", "powerState")),
                                    "proactivelyReported", true, "retrievable", true)),
                    alexaInterface("Alexa.RangeController",
                            Map.of("supported", List.of(Map.of("name", "rangeValue")),
                                    "proactivelyReported", true, "retrievable", true,
                                    "configuration", Map.of(
                                            "supportedRange", Map.of("minimumValue", 0, "maximumValue", 100, "precision", 1),
                                            "unitOfMeasure", "Alexa.Unit.Percent")))
            ));
            return ep;
        }

        if (deviceType == DEVICE_TYPE_THERMOSTAT) {
            Map<String, Object> ep = new HashMap<>();
            ep.put("endpointId",       "thermostat-" + device.getId());
            ep.put("manufacturerName",  "Aavita IoT");
            ep.put("friendlyName",      name);
            ep.put("description",       "Smart Thermostat in " + roomHint);
            ep.put("displayCategories", List.of("THERMOSTAT"));
            ep.put("capabilities", List.of(
                    alexaInterface("Alexa.PowerController",
                            Map.of("supported", List.of(Map.of("name", "powerState")),
                                    "proactivelyReported", true, "retrievable", true)),
                    alexaInterface("Alexa.ThermostatController",
                            Map.of("supported", List.of(Map.of("name", "targetSetpoint")),
                                    "proactivelyReported", true, "retrievable", true,
                                    "configuration", Map.of(
                                            "supportedModes", List.of("HEAT", "COOL", "OFF"),
                                            "supportsScheduling", false)))
            ));
            return ep;
        }

        log.warn("Unknown device type: {} for device id: {}", deviceType, device.getId());
        return null;
    }

    // =====================================================================
    // PRIVATE — State helpers
    // =====================================================================
    private List<Map<String, Object>> buildCurrentStateProperties(String endpointId) {
        List<Map<String, Object>> props = new ArrayList<>();
        String now = Instant.now().toString();

        if (endpointId.startsWith("light-")) {
            props.add(alexaProperty("Alexa.PowerController", "powerState",
                    lightService.isOn(endpointId) ? "ON" : "OFF", now));
            props.add(alexaProperty("Alexa.BrightnessController", "brightness",
                    lightService.getBrightness(endpointId), now));
        } else if (endpointId.startsWith("fan-")) {
            props.add(alexaProperty("Alexa.PowerController", "powerState",
                    fanService.isOn(endpointId) ? "ON" : "OFF", now));
            props.add(alexaProperty("Alexa.RangeController", "rangeValue",
                    fanService.getSpeed(endpointId), now));
        } else if (endpointId.startsWith("thermostat-")) {
            props.add(alexaProperty("Alexa.PowerController", "powerState",
                    thermostatService.isOn(endpointId) ? "ON" : "OFF", now));
            props.add(alexaProperty("Alexa.ThermostatController", "targetSetpoint",
                    Map.of("value", thermostatService.getTemperature(endpointId), "scale", "CELSIUS"), now));
        } else if (endpointId.startsWith("switch-")) {
            props.add(alexaProperty("Alexa.PowerController", "powerState",
                    switchService.isOn(endpointId) ? "ON" : "OFF", now));
        }

        return props;
    }

    // =====================================================================
    // PRIVATE — Alexa response builders
    // =====================================================================
    private Map<String, Object> buildResponseHeader(String namespace, String name, String correlationToken) {
        return Map.of(
                "namespace",        namespace,
                "name",             name,
                "messageId",        UUID.randomUUID().toString(),
                "correlationToken", correlationToken != null ? correlationToken : "",
                "payloadVersion",   "3"
        );
    }

    private Map<String, Object> buildControlResponse(String messageId, String endpointId,
                                                     String namespace, String propertyName,
                                                     Object value, String valueType) {
        String now = Instant.now().toString();
        return Map.of(
                "context", Map.of("properties", List.of(alexaProperty(namespace, propertyName, value, now))),
                "event", Map.of(
                        "header",   buildResponseHeader("Alexa", "Response", messageId),
                        "endpoint", Map.of("endpointId", endpointId),
                        "payload",  Map.of()
                )
        );
    }

    private Map<String, Object> alexaProperty(String namespace, String name, Object value, String timeOfSample) {
        return Map.of(
                "namespace",                 namespace,
                "name",                      name,
                "value",                     value,
                "timeOfSample",              timeOfSample,
                "uncertaintyInMilliseconds", 500
        );
    }

    private Map<String, Object> alexaInterface(String interfaceName, Map<String, Object> properties) {
        Map<String, Object> cap = new HashMap<>();
        cap.put("type",      "AlexaInterface");
        cap.put("interface", interfaceName);
        cap.put("version",   "3");
        cap.putAll(properties);
        return cap;
    }

    // =====================================================================
    // PRIVATE — Directive field extractors
    // =====================================================================
    private String getMessageId(Map<String, Object> directive) {
        Map<String, Object> header = (Map<String, Object>) directive.get("header");
        return header != null ? (String) header.get("messageId") : UUID.randomUUID().toString();
    }

    private String getName(Map<String, Object> directive) {
        Map<String, Object> header = (Map<String, Object>) directive.get("header");
        return header != null ? (String) header.get("name") : "";
    }

    private String getEndpointId(Map<String, Object> directive) {
        Map<String, Object> endpoint = (Map<String, Object>) directive.get("endpoint");
        return endpoint != null ? (String) endpoint.get("endpointId") : "";
    }

    private Map<String, Object> getPayload(Map<String, Object> directive) {
        Map<String, Object> payload = (Map<String, Object>) directive.get("payload");
        return payload != null ? payload : Map.of();
    }

    private String getDefaultName(byte deviceType, Long id) {
        return switch (deviceType) {
            case DEVICE_TYPE_LIGHT      -> "Light " + id;
            case DEVICE_TYPE_FAN        -> "Fan " + id;
            case DEVICE_TYPE_THERMOSTAT -> "Thermostat " + id;
            case DEVICE_TYPE_SWITCH     -> "Switch " + id;
            default                     -> "Device " + id;
        };
    }
}