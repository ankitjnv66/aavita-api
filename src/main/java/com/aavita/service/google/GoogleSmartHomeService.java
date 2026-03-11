package com.aavita.service.google;

import com.aavita.entity.Device;
import com.aavita.entity.Site;
import com.aavita.entity.User;
import com.aavita.oauth.model.OAuthToken;
import com.aavita.oauth.repository.OAuthTokenRepository;
import com.aavita.repository.DeviceRepository;
import com.aavita.repository.SiteRepository;
import com.aavita.repository.UserRepository;
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

    private final LightService          lightService;
    private final FanService            fanService;
    private final ThermostatService     thermostatService;
    private final DeviceRepository      deviceRepository;
    private final SiteRepository        siteRepository;
    private final UserRepository        userRepository;
    private final OAuthTokenRepository  oAuthTokenRepository;

    // Device type constants
    private static final byte DEVICE_TYPE_LIGHT      = 1;
    private static final byte DEVICE_TYPE_FAN        = 2;
    private static final byte DEVICE_TYPE_THERMOSTAT = 3;

    // =================================================================
    // INTENT 1: SYNC — Fetch devices dynamically from DB
    // =================================================================
    public Map<String, Object> handleSync(String requestId) {
        log.info("Handling SYNC for requestId: {}", requestId);

        // TODO: Extract userId from JWT/OAuth token when multi-user is needed
        // For now using default user-001
        String userId = "user-001";

        List<Map<String, Object>> devices = buildDeviceListForUser(userId);

        return Map.of(
                "requestId", requestId,
                "payload", Map.of(
                        "agentUserId", userId,
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
    // PRIVATE — Build device list from DB
    // =================================================================

    private List<Map<String, Object>> buildDeviceListForUser(String userId) {
        List<Map<String, Object>> devices = new ArrayList<>();

        try {
            // Get user by userId string (stored as "user-001" format in oauth_tokens)
            // Extract numeric id from "user-001" → 1L
            Long userIdLong = Long.parseLong(userId.replace("user-", ""));
            Optional<User> userOpt = userRepository.findById(userIdLong);

            if (userOpt.isEmpty()) {
                log.warn("User not found for userId: {}", userId);
                return devices;
            }

            // Get all sites for user
            List<Site> sites = siteRepository.findByUser_Id(userIdLong);

            for (Site site : sites) {
                // Get all devices for each site
                List<Device> siteDevices = deviceRepository.findBySite_SiteId(site.getSiteId());

                for (Device device : siteDevices) {
                    Map<String, Object> deviceMap = buildGoogleDevice(device, site);
                    if (deviceMap != null) {
                        devices.add(deviceMap);
                    }
                }
            }

            log.info("SYNC: Found {} devices for userId: {}", devices.size(), userId);

        } catch (Exception e) {
            log.error("Failed to build device list for userId: {}", userId, e);
        }

        return devices;
    }

    private Map<String, Object> buildGoogleDevice(Device device, Site site) {
        byte deviceType = device.getDeviceType();

        // Friendly name — use deviceName from DB or fallback to type-based name
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
            d.put("roomHint", roomHint);
            d.put("attributes", Map.of(
                    "commandOnlyBrightness", false,
                    "colorModel", "temp",
                    "colorTemperatureRange", Map.of(
                            "temperatureMinK", 2000,
                            "temperatureMaxK", 6500
                    )
            ));
            return d;
        }

        if (deviceType == DEVICE_TYPE_FAN) {
            Map<String, Object> d = new HashMap<>();
            d.put("id",     "fan-" + device.getId());
            d.put("type",   "action.devices.types.FAN");
            d.put("traits", List.of(
                    "action.devices.traits.OnOff",
                    "action.devices.traits.FanSpeed"
            ));
            d.put("name", Map.of(
                    "name",      name,
                    "nicknames", List.of(name.toLowerCase(), roomHint.toLowerCase() + " fan")
            ));
            d.put("willReportState", true);
            d.put("roomHint", roomHint);
            d.put("attributes", Map.of(
                    "reversible", false,
                    "supportsFanSpeedPercent", true
            ));
            return d;
        }

        if (deviceType == DEVICE_TYPE_THERMOSTAT) {
            Map<String, Object> d = new HashMap<>();
            d.put("id",     "thermostat-" + device.getId());
            d.put("type",   "action.devices.types.THERMOSTAT");
            d.put("traits", List.of(
                    "action.devices.traits.OnOff",
                    "action.devices.traits.TemperatureSetting"
            ));
            d.put("name", Map.of(
                    "name",      name,
                    "nicknames", List.of(name.toLowerCase(), roomHint.toLowerCase() + " thermostat")
            ));
            d.put("willReportState", true);
            d.put("roomHint", roomHint);
            d.put("attributes", Map.of(
                    "availableThermostatModes", List.of("off", "heat", "cool"),
                    "thermostatTemperatureUnit", "C"
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
            default                     -> "Device " + id;
        };
    }

    // =================================================================
    // PRIVATE — State and command helpers
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

        if (deviceId.startsWith("light")) {
            switch (command) {
                case "action.devices.commands.OnOff" ->
                        lightService.setOnOff(deviceId, (Boolean) params.get("on"));
                case "action.devices.commands.BrightnessAbsolute" ->
                        lightService.setBrightness(deviceId, ((Number) params.get("brightness")).intValue());
                case "action.devices.commands.ColorAbsolute" -> {
                    Map<String, Object> color = (Map<String, Object>) params.get("color");
                    if (color != null && color.containsKey("temperature")) {
                        lightService.setColorTemperature(deviceId, percentFromKelvin(((Number) color.get("temperature")).intValue()));
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
                        thermostatService.setTemperature(deviceId, ((Number) params.get("thermostatTemperatureSetpoint")).doubleValue());
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
