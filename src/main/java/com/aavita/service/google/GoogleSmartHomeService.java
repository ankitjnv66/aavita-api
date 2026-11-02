package com.aavita.service.google;

import com.aavita.entity.Device;
import com.aavita.entity.DeviceDigitalPin;
import com.aavita.entity.Site;
import com.aavita.repository.DeviceRepository;
import com.aavita.service.FanService;
import com.aavita.service.LightService;
import com.aavita.service.SwitchService;
import com.aavita.service.ThermostatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class GoogleSmartHomeService {

    private final LightService          lightService;
    private final FanService            fanService;
    private final ThermostatService     thermostatService;
    private final SwitchService         switchService;
    private final DeviceRepository      deviceRepository;

    // Device type constants — must match your device_type column values
    private static final byte DEVICE_TYPE_LIGHT      = 1;
    private static final byte DEVICE_TYPE_FAN        = 2;
    private static final byte DEVICE_TYPE_THERMOSTAT = 3;
    private static final byte DEVICE_TYPE_SWITCH     = 4;  // multi-pin GPIO controller

    // =================================================================
    // INTENT 1: SYNC — only the token owner's devices
    // =================================================================
    public Map<String, Object> handleSync(String requestId, Long userId) {
        log.info("Handling SYNC for requestId: {} userId: {}", requestId, userId);
        String agentUserId = "user-" + userId;
        List<Map<String, Object>> devices = buildDeviceListForUser(userId);
        return Map.of(
                "requestId", requestId,
                "payload", Map.of(
                        "agentUserId", agentUserId,
                        "devices", devices
                )
        );
    }

    // =================================================================
    // INTENT 2: QUERY — only the token owner's devices
    // =================================================================
    public Map<String, Object> handleQuery(String requestId, List<Map<String, Object>> inputs, Long userId) {
        log.info("Handling QUERY for requestId: {} userId: {}", requestId, userId);
        Map<String, Object> payload = (Map<String, Object>) inputs.get(0).get("payload");
        List<Map<String, Object>> requestedDevices = (List<Map<String, Object>>) payload.get("devices");

        Map<String, Object> deviceStates = new HashMap<>();
        for (Map<String, Object> device : requestedDevices) {
            String deviceId = (String) device.get("id");
            if (!isOwnedBy(deviceId, userId)) {
                log.warn("QUERY denied: device {} is not owned by userId {}", deviceId, userId);
                deviceStates.put(deviceId, Map.of(
                        "online",    false,
                        "status",    "ERROR",
                        "errorCode", "deviceNotFound"
                ));
                continue;
            }
            deviceStates.put(deviceId, getCurrentState(deviceId));
        }
        return Map.of("requestId", requestId, "payload", Map.of("devices", deviceStates));
    }

    // =================================================================
    // INTENT 3: EXECUTE — only the token owner's devices
    // =================================================================
    public Map<String, Object> handleExecute(String requestId, List<Map<String, Object>> inputs, Long userId) {
        log.info("Handling EXECUTE for requestId: {} userId: {}", requestId, userId);
        Map<String, Object> payload = (Map<String, Object>) inputs.get(0).get("payload");
        List<Map<String, Object>> commands = (List<Map<String, Object>>) payload.get("commands");
        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> command : commands) {
            List<Map<String, Object>> devices   = (List<Map<String, Object>>) command.get("devices");
            List<Map<String, Object>> execution = (List<Map<String, Object>>) command.get("execution");

            for (Map<String, Object> device : devices) {
                String deviceId = (String) device.get("id");

                // Ownership check: never send a command for a device the token owner does not own
                if (!isOwnedBy(deviceId, userId)) {
                    log.warn("EXECUTE denied: device {} is not owned by userId {}", deviceId, userId);
                    results.add(Map.of(
                            "ids",       List.of(String.valueOf(deviceId)),
                            "status",    "ERROR",
                            "errorCode", "deviceNotFound"
                    ));
                    continue;
                }

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
        return Map.of("requestId", requestId, "payload", Map.of("commands", results));
    }

    // =================================================================
    // PRIVATE — Ownership helpers
    // =================================================================

    /**
     * Google IDs look like "light-7", "fan-3", "thermostat-2" or "switch-4-5".
     * The database device id is always the number right after the first dash.
     */
    private Optional<Long> parseDeviceId(String googleId) {
        if (googleId == null) return Optional.empty();
        String[] parts = googleId.split("-");
        if (parts.length < 2) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(parts[1]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private boolean isOwnedBy(String googleId, Long userId) {
        Optional<Long> dbId = parseDeviceId(googleId);
        return dbId.isPresent() && deviceRepository.existsByIdAndUser_Id(dbId.get(), userId);
    }

    // =================================================================
    // PRIVATE — Build device list from DB
    // =================================================================
    private List<Map<String, Object>> buildDeviceListForUser(Long userId) {
        List<Map<String, Object>> devices = new ArrayList<>();
        try {
            List<Device> userDevices = new ArrayList<>(deviceRepository.findAllByUserIdWithPins(userId));
            userDevices.sort(Comparator.comparing(Device::getId));
            for (Device device : userDevices) {
                devices.addAll(buildGoogleDevices(device, device.getSite()));
            }
            log.info("SYNC: Found {} Google Home devices for userId: {}", devices.size(), userId);
        } catch (Exception e) {
            log.error("Failed to build device list for userId: {}", userId, e);
        }
        return devices;
    }

    /**
     * Returns a List because DEVICE_TYPE_SWITCH expands into N entries (one per digital pin),
     * while Light/Fan/Thermostat each return exactly one entry.
     */
    private List<Map<String, Object>> buildGoogleDevices(Device device, Site site) {
        if (device.getDeviceType() == DEVICE_TYPE_SWITCH) {
            return buildSwitchDevices(device, site);
        }
        Map<String, Object> single = buildGoogleDevice(device, site);
        return single != null ? List.of(single) : List.of();
    }

    /**
     * SWITCH: one Google Home entry per DeviceDigitalPin.
     *
     * Google Home ID : "switch-{deviceId}-{pinNumber}"
     * Example        : "switch-42-5" → Device 42, GPIO pin 5
     *
     * Pins are auto-created by DevicePinService when ESP8266 first reports.
     * If no pins yet, this device won't appear in Google Home until ESP8266 connects.
     */
    private List<Map<String, Object>> buildSwitchDevices(Device device, Site site) {
        List<Map<String, Object>> result = new ArrayList<>();
        String roomHint = device.getRoomHint() != null ? device.getRoomHint() : site.getLocation();

        for (DeviceDigitalPin pin : device.getDigitalPins()) {
            String pinLabel = (device.getDeviceName() != null)
                    ? device.getDeviceName() + " Pin " + pin.getPinNumber()
                    : "Switch " + device.getId() + " Pin " + pin.getPinNumber();

            String googleId = "switch-" + device.getId() + "-" + pin.getPinNumber();

            Map<String, Object> d = new HashMap<>();
            d.put("id",              googleId);
            d.put("type",            "action.devices.types.SWITCH");
            d.put("traits",          List.of("action.devices.traits.OnOff"));
            d.put("name", Map.of(
                    "name",      pinLabel,
                    "nicknames", List.of(
                            pinLabel.toLowerCase(),
                            roomHint.toLowerCase() + " switch " + pin.getPinNumber()
                    )
            ));
            d.put("willReportState", true);
            d.put("roomHint",        roomHint);
            d.put("deviceInfo", Map.of(
                    "manufacturer", "ESP8266",
                    "model",        "GPIO-" + pin.getPinNumber(),
                    "hwVersion",    device.getSrcMac()
            ));
            result.add(d);
        }

        if (result.isEmpty()) {
            log.warn("SYNC: Device {} (SWITCH) has no pins yet — will appear after ESP8266 first connects",
                    device.getId());
        } else {
            log.info("SYNC: Device {} → {} switch(es) in Google Home", device.getId(), result.size());
        }
        return result;
    }

    // Single-device builder for Light/Fan/Thermostat
    private Map<String, Object> buildGoogleDevice(Device device, Site site) {
        byte deviceType = device.getDeviceType();
        String name     = device.getDeviceName() != null ? device.getDeviceName() : getDefaultName(deviceType, device.getId());
        String roomHint = device.getRoomHint()   != null ? device.getRoomHint()   : site.getLocation();

        if (deviceType == DEVICE_TYPE_LIGHT) {
            Map<String, Object> d = new HashMap<>();
            d.put("id",     "light-" + device.getId());
            d.put("type",   "action.devices.types.LIGHT");
            d.put("traits", List.of(
                    "action.devices.traits.OnOff",
                    "action.devices.traits.Brightness",
                    "action.devices.traits.ColorSetting"
            ));
            d.put("name", Map.of(
                    "name",      name,
                    "nicknames", List.of(name.toLowerCase(), roomHint.toLowerCase() + " light")
            ));
            d.put("willReportState", true);
            d.put("roomHint",        roomHint);
            d.put("attributes", Map.of(
                    "commandOnlyBrightness", false,
                    "colorModel", "temp",
                    "colorTemperatureRange", Map.of("temperatureMinK", 2000, "temperatureMaxK", 6500)
            ));
            return d;
        }
        if (deviceType == DEVICE_TYPE_FAN) {
            Map<String, Object> d = new HashMap<>();
            d.put("id",     "fan-" + device.getId());
            d.put("type",   "action.devices.types.FAN");
            d.put("traits", List.of("action.devices.traits.OnOff", "action.devices.traits.FanSpeed"));
            d.put("name", Map.of(
                    "name",      name,
                    "nicknames", List.of(name.toLowerCase(), roomHint.toLowerCase() + " fan")
            ));
            d.put("willReportState", true);
            d.put("roomHint",        roomHint);
            d.put("attributes", Map.of("reversible", false, "supportsFanSpeedPercent", true));
            return d;
        }
        if (deviceType == DEVICE_TYPE_THERMOSTAT) {
            Map<String, Object> d = new HashMap<>();
            d.put("id",     "thermostat-" + device.getId());
            d.put("type",   "action.devices.types.THERMOSTAT");
            d.put("traits", List.of("action.devices.traits.OnOff", "action.devices.traits.TemperatureSetting"));
            d.put("name", Map.of(
                    "name",      name,
                    "nicknames", List.of(name.toLowerCase(), roomHint.toLowerCase() + " thermostat")
            ));
            d.put("willReportState", true);
            d.put("roomHint",        roomHint);
            d.put("attributes", Map.of(
                    "availableThermostatModes",   List.of("off", "heat", "cool"),
                    "thermostatTemperatureUnit",  "C"
            ));
            return d;
        }
        log.warn("Unknown device type: {} for device id: {}", deviceType, device.getId());
        return null;
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

    // =================================================================
    // PRIVATE — State and command routing
    // =================================================================
    private Map<String, Object> getCurrentState(String deviceId) {
        // Switch GPIO pins — read DeviceDigitalPin.state from DB
        if (deviceId.startsWith("switch-")) {
            return new HashMap<>(Map.of(
                    "online", true,
                    "on",     switchService.isOn(deviceId)
            ));
        }
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
                    "online",                 true,
                    "on",                     fanService.isOn(deviceId),
                    "currentFanSpeedPercent", fanService.getSpeed(deviceId)
            ));
        }
        if (deviceId.startsWith("thermostat")) {
            double temp = thermostatService.getTemperature(deviceId);
            return new HashMap<>(Map.of(
                    "online",                        true,
                    "on",                            thermostatService.isOn(deviceId),
                    "thermostatMode",                thermostatService.isOn(deviceId) ? "heat" : "off",
                    "thermostatTemperatureSetpoint", temp,
                    "thermostatTemperatureAmbient",  temp
            ));
        }
        return Map.of("online", false);
    }

    private void executeCommand(String deviceId, String command, Map<String, Object> params) {
        log.info("Executing command: {} on device: {} with params: {}", command, deviceId, params);

        // Switch GPIO pin toggle
        if (deviceId.startsWith("switch-")) {
            if ("action.devices.commands.OnOff".equals(command)) {
                switchService.setOnOff(deviceId, (Boolean) params.get("on"));
            } else {
                log.warn("Unhandled switch command: {}", command);
            }
            return;
        }

        if (deviceId.startsWith("light")) {
            switch (command) {
                case "action.devices.commands.OnOff" ->
                        lightService.setOnOff(deviceId, (Boolean) params.get("on"));
                case "action.devices.commands.BrightnessAbsolute" ->
                        lightService.setBrightness(deviceId, ((Number) params.get("brightness")).intValue());
                case "action.devices.commands.ColorAbsolute" -> {
                    Map<String, Object> color = (Map<String, Object>) params.get("color");
                    if (color != null && color.containsKey("temperature")) {
                        lightService.setColorTemperature(deviceId,
                                percentFromKelvin(((Number) color.get("temperature")).intValue()));
                    }
                }
                default -> log.warn("Unhandled light command: {}", command);
            }
            return;
        }
        if (deviceId.startsWith("fan")) {
            switch (command) {
                case "action.devices.commands.OnOff" ->
                        fanService.setOnOff(deviceId, (Boolean) params.get("on"));
                case "action.devices.commands.SetFanSpeed" ->
                        fanService.setSpeed(deviceId, ((Number) params.get("fanSpeedPercent")).intValue());
                default -> log.warn("Unhandled fan command: {}", command);
            }
            return;
        }
        if (deviceId.startsWith("thermostat")) {
            switch (command) {
                case "action.devices.commands.OnOff" ->
                        thermostatService.setOnOff(deviceId, (Boolean) params.get("on"));
                case "action.devices.commands.ThermostatTemperatureSetpoint" ->
                        thermostatService.setTemperature(deviceId,
                                ((Number) params.get("thermostatTemperatureSetpoint")).doubleValue());
                case "action.devices.commands.ThermostatSetMode" ->
                        thermostatService.setOnOff(deviceId, !params.get("thermostatMode").equals("off"));
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