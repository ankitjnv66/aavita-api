package com.aavita.service.google;

import com.aavita.service.FanService;
import com.aavita.service.LightService;
import com.aavita.service.ThermostatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSmartHomeService {

    private final LightService      lightService;
    private final FanService        fanService;
    private final ThermostatService thermostatService;

    // =================================================================
    // INTENT 1: SYNC — Tell Google what devices exist
    // =================================================================
    public Map<String, Object> handleSync(String requestId) {
        log.info("Handling SYNC for requestId: {}", requestId);

        List<Map<String, Object>> devices = new ArrayList<>();

        // ----- LIGHT -----
        Map<String, Object> light = new HashMap<>();
        light.put("id", "light-1");
        light.put("type", "action.devices.types.LIGHT");
        light.put("traits", List.of(
                "action.devices.traits.OnOff",
                "action.devices.traits.Brightness",
                "action.devices.traits.ColorSetting"
        ));
        light.put("name", Map.of(
                "name", "Smart Light",
                "nicknames", List.of("main light", "room light")
        ));
        light.put("willReportState", true);
        light.put("roomHint", "Living Room");
        light.put("attributes", Map.of(
                "commandOnlyBrightness", false,
                "colorModel", "temp",
                "colorTemperatureRange", Map.of(
                        "temperatureMinK", 2000,
                        "temperatureMaxK", 6500
                )
        ));
        devices.add(light);

        // ----- FAN -----
        Map<String, Object> fan = new HashMap<>();
        fan.put("id", "fan-2");
        fan.put("type", "action.devices.types.FAN");
        fan.put("traits", List.of(
                "action.devices.traits.OnOff",
                "action.devices.traits.FanSpeed"
        ));
        fan.put("name", Map.of(
                "name", "Smart Fan",
                "nicknames", List.of("ceiling fan", "room fan")
        ));
        fan.put("willReportState", true);
        fan.put("roomHint", "Living Room");
        fan.put("attributes", Map.of(
                "reversible", false,
                "supportsFanSpeedPercent", true
        ));
        devices.add(fan);

        // ----- THERMOSTAT -----
        Map<String, Object> thermostat = new HashMap<>();
        thermostat.put("id", "thermostat-3");
        thermostat.put("type", "action.devices.types.THERMOSTAT");
        thermostat.put("traits", List.of(
                "action.devices.traits.OnOff",
                "action.devices.traits.TemperatureSetting"
        ));
        thermostat.put("name", Map.of(
                "name", "Smart Thermostat",
                "nicknames", List.of("thermostat", "ac")
        ));
        thermostat.put("willReportState", true);
        thermostat.put("roomHint", "Living Room");
        thermostat.put("attributes", Map.of(
                "availableThermostatModes", List.of("off", "heat", "cool"),
                "thermostatTemperatureUnit", "C"
        ));
        devices.add(thermostat);

        return Map.of(
                "requestId", requestId,
                "payload", Map.of(
                        "agentUserId", "user-001",
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

    private Map<String, Object> getCurrentState(String deviceId) {
        if (deviceId.startsWith("light")) {
            return new HashMap<>(Map.of(
                    "online",            true,
                    "on",                lightService.isOn(deviceId),
                    "brightness",        lightService.getBrightness(deviceId),
                    "colorTemperatureK", kelvinFromPercent(lightService.getColorTemperature(deviceId))
            ));
        }
        if (deviceId.startsWith("fan")) {
            return new HashMap<>(Map.of(
                    "online",           true,
                    "on",               fanService.isOn(deviceId),
                    "currentFanSpeedPercent", fanService.getSpeed(deviceId)
            ));
        }
        if (deviceId.startsWith("thermostat")) {
            double temp = thermostatService.getTemperature(deviceId);
            return new HashMap<>(Map.of(
                    "online",                          true,
                    "on",                              thermostatService.isOn(deviceId),
                    "thermostatMode",                  thermostatService.isOn(deviceId) ? "heat" : "off",
                    "thermostatTemperatureSetpoint",   temp,
                    "thermostatTemperatureAmbient",    temp
            ));
        }
        return Map.of("online", false);
    }

    private void executeCommand(String deviceId, String command, Map<String, Object> params) {
        log.info("Executing command: {} on device: {} with params: {}", command, deviceId, params);

        // ----- LIGHT COMMANDS -----
        if (deviceId.startsWith("light")) {
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
                    Map<String, Object> color = (Map<String, Object>) params.get("color");
                    if (color != null && color.containsKey("temperature")) {
                        int kelvin  = ((Number) color.get("temperature")).intValue();
                        int percent = percentFromKelvin(kelvin);
                        lightService.setColorTemperature(deviceId, percent);
                    }
                }
                default -> log.warn("Unhandled light command: {}", command);
            }
            return;
        }

        // ----- FAN COMMANDS -----
        if (deviceId.startsWith("fan")) {
            switch (command) {
                case "action.devices.commands.OnOff" -> {
                    boolean on = (Boolean) params.get("on");
                    fanService.setOnOff(deviceId, on);
                }
                case "action.devices.commands.SetFanSpeed" -> {
                    int speed = ((Number) params.get("fanSpeedPercent")).intValue();
                    fanService.setSpeed(deviceId, speed);
                }
                default -> log.warn("Unhandled fan command: {}", command);
            }
            return;
        }

        // ----- THERMOSTAT COMMANDS -----
        if (deviceId.startsWith("thermostat")) {
            switch (command) {
                case "action.devices.commands.OnOff" -> {
                    boolean on = (Boolean) params.get("on");
                    thermostatService.setOnOff(deviceId, on);
                }
                case "action.devices.commands.ThermostatTemperatureSetpoint" -> {
                    double temp = ((Number) params.get("thermostatTemperatureSetpoint")).doubleValue();
                    thermostatService.setTemperature(deviceId, temp);
                }
                case "action.devices.commands.ThermostatSetMode" -> {
                    String mode = (String) params.get("thermostatMode");
                    thermostatService.setOnOff(deviceId, !mode.equals("off"));
                }
                default -> log.warn("Unhandled thermostat command: {}", command);
            }
            return;
        }

        log.warn("Unknown device type for deviceId: {}", deviceId);
    }

    private int percentFromKelvin(int kelvin) {
        int clamped = Math.max(2000, Math.min(6500, kelvin));
        return (int) Math.round((clamped - 2000.0) / (6500.0 - 2000.0) * 100);
    }

    private int kelvinFromPercent(int percent) {
        return (int) Math.round(2000 + (percent / 100.0) * (6500 - 2000));
    }
}