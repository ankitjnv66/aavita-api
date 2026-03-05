package com.aavita.service.google;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Core service handling all three Google Smart Home intents:
 *
 *  - SYNC    → Tell Google what devices you have
 *  - QUERY   → Return current state of devices
 *  - EXECUTE → Perform commands on devices
 *
 * Wire your existing LightService and ThermostatService below.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSmartHomeService {

    // ---------------------------------------------------------------
    // TODO: Replace these with your actual service injections
    // Example:
    //   private final LightService lightService;
    //   private final ThermostatService thermostatService;
    // ---------------------------------------------------------------

    // =================================================================
    // INTENT 1: SYNC — Tell Google what devices exist in your system
    // =================================================================
    public Map<String, Object> handleSync(String requestId) {
        log.info("Handling SYNC for requestId: {}", requestId);

        List<Map<String, Object>> devices = new ArrayList<>();

        // ----- LIGHT DEVICE -----
        Map<String, Object> light = new HashMap<>();
        light.put("id", "light-001");                                   // Must be unique & stable
        light.put("type", "action.devices.types.LIGHT");
        light.put("traits", List.of(
                "action.devices.traits.OnOff",
                "action.devices.traits.Brightness"
        ));
        light.put("name", Map.of(
                "name", "Living Room Light",                            // Shown in Google Home app
                "nicknames", List.of("living room", "main light")
        ));
        light.put("willReportState", false);                            // Set true if using Report State API
        light.put("roomHint", "Living Room");
        light.put("attributes", Map.of(
                "commandOnlyBrightness", false
        ));
        devices.add(light);

        // ----- THERMOSTAT DEVICE -----
        Map<String, Object> thermostat = new HashMap<>();
        thermostat.put("id", "thermostat-001");
        thermostat.put("type", "action.devices.types.THERMOSTAT");
        thermostat.put("traits", List.of(
                "action.devices.traits.TemperatureSetting"
        ));
        thermostat.put("name", Map.of(
                "name", "Main Thermostat",
                "nicknames", List.of("thermostat", "AC", "heater")
        ));
        thermostat.put("willReportState", false);
        thermostat.put("roomHint", "Living Room");
        thermostat.put("attributes", Map.of(
                "availableThermostatModes", List.of("off", "heat", "cool", "auto"),
                "thermostatTemperatureUnit", "CELSIUS"
        ));
        devices.add(thermostat);

        return Map.of(
                "requestId", requestId,
                "payload", Map.of(
                        "agentUserId", "user-001",          // Unique ID for the user in your system
                        "devices", devices
                )
        );
    }

    // =================================================================
    // INTENT 2: QUERY — Return current state of requested devices
    // =================================================================
    public Map<String, Object> handleQuery(String requestId, List<Map<String, Object>> inputs) {
        log.info("Handling QUERY for requestId: {}", requestId);

        Map<String, Object> payload = (Map<String, Object>) inputs.get(0).get("payload");
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
    // INTENT 3: EXECUTE — Perform a command on one or more devices
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
                                "ids", List.of(deviceId),
                                "status", "SUCCESS",
                                "states", getCurrentState(deviceId)
                        ));
                    } catch (Exception e) {
                        log.error("Execute failed for device {} command {}: {}", deviceId, cmdName, e.getMessage());
                        results.add(Map.of(
                                "ids", List.of(deviceId),
                                "status", "ERROR",
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
     * Returns the current state of a device.
     * TODO: Replace stub calls with your actual service calls.
     *
     * Example replacement:
     *   if (deviceId.startsWith("light")) {
     *       return Map.of(
     *           "on",         lightService.isOn(deviceId),
     *           "brightness", lightService.getBrightness(deviceId),
     *           "online",     true
     *       );
     *   }
     */
    private Map<String, Object> getCurrentState(String deviceId) {
        if (deviceId.startsWith("light")) {
            return new HashMap<>(Map.of(
                    "online",     true,
                    "on",         false,          // TODO: lightService.isOn(deviceId)
                    "brightness", 100             // TODO: lightService.getBrightness(deviceId)
            ));
        } else if (deviceId.startsWith("thermostat")) {
            return new HashMap<>(Map.of(
                    "online",                          true,
                    "thermostatMode",                  "cool",   // TODO: thermostatService.getMode(deviceId)
                    "thermostatTemperatureSetpoint",   24.0,     // TODO: thermostatService.getSetpoint(deviceId)
                    "thermostatTemperatureAmbient",    26.0      // TODO: thermostatService.getAmbient(deviceId)
            ));
        }
        return Map.of("online", false);
    }

    /**
     * Dispatches a Google command to your device services.
     * TODO: Replace stub calls with your actual service calls.
     *
     * Example replacement:
     *   case "action.devices.commands.OnOff" ->
     *       lightService.setOnOff(deviceId, (Boolean) params.get("on"));
     */
    private void executeCommand(String deviceId, String command, Map<String, Object> params) {
        log.info("Executing command: {} on device: {} with params: {}", command, deviceId, params);

        switch (command) {

            // --- Light commands ---
            case "action.devices.commands.OnOff" -> {
                boolean on = (Boolean) params.get("on");
                // TODO: lightService.setOnOff(deviceId, on);
                log.info("Light {} turned {}", deviceId, on ? "ON" : "OFF");
            }
            case "action.devices.commands.BrightnessAbsolute" -> {
                int brightness = (Integer) params.get("brightness");
                // TODO: lightService.setBrightness(deviceId, brightness);
                log.info("Light {} brightness set to {}%", deviceId, brightness);
            }

            // --- Thermostat commands ---
            case "action.devices.commands.ThermostatTemperatureSetpoint" -> {
                double temp = ((Number) params.get("thermostatTemperatureSetpoint")).doubleValue();
                // TODO: thermostatService.setTemperature(deviceId, temp);
                log.info("Thermostat {} setpoint set to {}°C", deviceId, temp);
            }
            case "action.devices.commands.ThermostatSetMode" -> {
                String mode = (String) params.get("thermostatMode");
                // TODO: thermostatService.setMode(deviceId, mode);
                log.info("Thermostat {} mode set to {}", deviceId, mode);
            }

            default -> log.warn("Unhandled command: {} for device: {}", command, deviceId);
        }
    }
}