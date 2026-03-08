package com.aavita.service.google;

import com.aavita.service.LightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * GoogleSmartHomeService — Fully wired with LightService.
 * Handles SYNC, QUERY, EXECUTE intents from Google Smart Home.
 *
 * Device ID convention:
 *   Google device ID "light-{dbId}" maps to Device.id in your DB
 *   e.g. "light-42" → Device with id=42
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSmartHomeService {

    private final LightService lightService;

    // =================================================================
    // INTENT 1: SYNC — Tell Google what light devices exist
    // =================================================================
    public Map<String, Object> handleSync(String requestId) {
        log.info("Handling SYNC for requestId: {}", requestId);

        List<Map<String, Object>> devices = new ArrayList<>();

        // ----- LIGHT DEVICE -----
        // TODO: Replace hardcoded device with a DB query to fetch all
        //       light-type devices for the authenticated user.
        // Example:
        //   List<Device> lights = deviceRepository.findByUserIdAndDeviceType(userId, LIGHT_TYPE);
        //   for (Device d : lights) { ... add to devices list ... }

        Map<String, Object> light = new HashMap<>();
        light.put("id", "light-1");                         // Format: "light-{Device.id}"
        light.put("type", "action.devices.types.LIGHT");
        light.put("traits", List.of(
                "action.devices.traits.OnOff",
                "action.devices.traits.Brightness",
                "action.devices.traits.ColorSetting"        // Added for color temperature
        ));
        light.put("name", Map.of(
                "name", "Smart Light",
                "nicknames", List.of("main light", "room light")
        ));
        light.put("willReportState", false);
        light.put("roomHint", "Living Room");
        light.put("attributes", Map.of(
                "commandOnlyBrightness", false,
                "colorModel", "temp",                       // Color temperature mode
                "colorTemperatureRange", Map.of(
                        "temperatureMinK", 2000,
                        "temperatureMaxK", 6500
                )
        ));
        devices.add(light);

        return Map.of(
                "requestId", requestId,
                "payload", Map.of(
                        "agentUserId", "user-001",          // TODO: use real authenticated userId
                        "devices", devices
                )
        );
    }

    // =================================================================
    // INTENT 2: QUERY — Return current state of requested devices
    // =================================================================
    public Map<String, Object> handleQuery(String requestId, List<Map<String, Object>> inputs) {
        log.info("Handling QUERY for requestId: {}", requestId);

        Map<String, Object> payload   = (Map<String, Object>) inputs.get(0).get("payload");
        List<Map<String, Object>> requestedDevices = (List<Map<String, Object>>) payload.get("devices");

        Map<String, Object> deviceStates = new HashMap<>();
        for (Map<String, Object> device : requestedDevices) {
            String deviceId = (String) device.get("id");
            deviceStates.put(deviceId, getCurrentState(deviceId));
        }

        return Map.of(
                "requestId", requestId,
                "payload", Map.of("devices", deviceStates)
        );
    }

    // =================================================================
    // INTENT 3: EXECUTE — Perform commands on devices
    // =================================================================
    public Map<String, Object> handleExecute(String requestId, List<Map<String, Object>> inputs) {
        log.info("Handling EXECUTE for requestId: {}", requestId);

        Map<String, Object> payload = (Map<String, Object>) inputs.get(0).get("payload");
        List<Map<String, Object>> commands = (List<Map<String, Object>>) payload.get("commands");

        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> command : commands) {
            List<Map<String, Object>> devices   = (List<Map<String, Object>>) command.get("devices");
            List<Map<String, Object>> execution = (List<Map<String, Object>>) command.get("execution");

            for (Map<String, Object> device : devices) {
                String deviceId = (String) device.get("id");

                for (Map<String, Object> exec : execution) {
                    String cmdName = (String) exec.get("command");
                    Map<String, Object> params = (Map<String, Object>) exec.getOrDefault("params", Map.of());

                    try {
                        executeCommand(deviceId, cmdName, params);
                        results.add(Map.of(
                                "ids",    List.of(deviceId),
                                "status", "SUCCESS",
                                "states", getCurrentState(deviceId)
                        ));
                    } catch (Exception e) {
                        log.error("Execute failed for device {} command {}: {}", deviceId, cmdName, e.getMessage());
                        results.add(Map.of(
                                "ids",       List.of(deviceId),
                                "status",    "ERROR",
                                "errorCode", "hardError"
                        ));
                    }
                }
            }
        }

        return Map.of(
                "requestId", requestId,
                "payload", Map.of("commands", results)
        );
    }

    // =================================================================
    // PRIVATE HELPERS
    // =================================================================

    /**
     * Fetches real-time device state from DB via LightService.
     */
    private Map<String, Object> getCurrentState(String deviceId) {
        if (deviceId.startsWith("light")) {
            return new HashMap<>(Map.of(
                    "online",      true,
                    "on",          lightService.isOn(deviceId),
                    "brightness",  lightService.getBrightness(deviceId),
                    "colorTemperatureK", kelvinFromPercent(lightService.getColorTemperature(deviceId))
            ));
        }
        return Map.of("online", false);
    }

    /**
     * Routes Google commands to LightService methods.
     */
    private void executeCommand(String deviceId, String command, Map<String, Object> params) {
        log.info("Executing command: {} on device: {} with params: {}", command, deviceId, params);

        switch (command) {

            case "action.devices.commands.OnOff" -> {
                boolean on = (Boolean) params.get("on");
                lightService.setOnOff(deviceId, on);
            }

            case "action.devices.commands.BrightnessAbsolute" -> {
                int brightness = ((Number) params.get("brightness")).intValue();
                lightService.setBrightness(deviceId, brightness);
            }

            case "action.devices.commands.ColorAbsolute" -> {
                // Google sends color temperature in Kelvin (2000K-6500K)
                Map<String, Object> color = (Map<String, Object>) params.get("color");
                if (color != null && color.containsKey("temperature")) {
                    int kelvin = ((Number) color.get("temperature")).intValue();
                    int percent = percentFromKelvin(kelvin);
                    lightService.setColorTemperature(deviceId, percent);
                }
            }

            default -> log.warn("Unhandled command: {} for device: {}", command, deviceId);
        }
    }

    /**
     * Converts Kelvin (2000-6500) to 0-100% for PWM storage.
     */
    private int percentFromKelvin(int kelvin) {
        int clamped = Math.max(2000, Math.min(6500, kelvin));
        return (int) Math.round((clamped - 2000.0) / (6500.0 - 2000.0) * 100);
    }

    /**
     * Converts 0-100% back to Kelvin for Google's response.
     */
    private int kelvinFromPercent(int percent) {
        return (int) Math.round(2000 + (percent / 100.0) * (6500 - 2000));
    }
}