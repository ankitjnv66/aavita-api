package com.aavita.service;

import com.aavita.entity.*;
import com.aavita.mqtt.MqttService;
import com.aavita.service.google.HomeGraphReportService;
import com.aavita.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * LightService — Controls light devices via your existing infrastructure:
 *
 *   DeviceDigitalPin  → ON/OFF  (pin state: 0=OFF, 1=ON)
 *   DevicePwmPin      → Brightness (pin 0) and Color temperature (pin 1)
 *   DeviceCommand     → Persists every command sent to the device
 *   MqttService       → Publishes control commands to Mosquitto broker
 *
 * Google sends device IDs as "light-{Device.id}" e.g. "light-42"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LightService {

    private final DeviceRepository           deviceRepository;
    private final DeviceDigitalPinRepository digitalPinRepository;
    private final DevicePwmPinRepository     pwmPinRepository;
    private final DeviceCommandRepository    deviceCommandRepository;
    private final MqttService                mqttService;
    private final ObjectMapper               objectMapper;
    private final HomeGraphReportService     homeGraphReportService;

    // Pin number conventions — adjust if your device uses different pin numbers
    private static final byte LIGHT_ONOFF_PIN      = 0;   // Digital pin 0 = ON/OFF
    private static final byte LIGHT_BRIGHTNESS_PIN  = 0;   // PWM pin 0     = Brightness
    private static final byte LIGHT_COLOR_PIN        = 1;   // PWM pin 1     = Color temperature

    // Match these to your existing protocol values
    private static final int  PKT_TYPE_LIGHT_CONTROL = 0x10;
    private static final byte ACTION_CAUSE_GOOGLE     = 0x05;

    // MQTT publish topic — your subscribe is "+/+/sub"
    // so publish likely follows: "{gatewayMac}/{meshId}/pub"
    // Adjust if your actual publish topic differs
    private static final String MQTT_TOPIC_FORMAT = "%s/%s/pub";

    // ----------------------------------------------------------------
    // 1. IS ON — Check if light is currently ON from DB state
    // ----------------------------------------------------------------
    public boolean isOn(String deviceId) {
        Long id = parseDeviceId(deviceId);
        return digitalPinRepository
                .findByDevice_IdAndPinNumber(id, LIGHT_ONOFF_PIN)
                .map(pin -> pin.getState() == 1)
                .orElse(false);
    }

    // ----------------------------------------------------------------
    // 2. GET BRIGHTNESS — Returns brightness as 0-100%
    // ----------------------------------------------------------------
    public int getBrightness(String deviceId) {
        Long id = parseDeviceId(deviceId);
        return pwmPinRepository
                .findByDevice_IdAndPinNumber(id, LIGHT_BRIGHTNESS_PIN)
                .map(pin -> (int) (pin.getValue() & 0xFF))          // byte → unsigned 0-255
                .map(v  -> (int) Math.round(v / 255.0 * 100))       // scale → 0-100%
                .orElse(100);
    }

    // ----------------------------------------------------------------
    // 3. GET COLOR TEMPERATURE — Returns value 0-100%
    // ----------------------------------------------------------------
    public int getColorTemperature(String deviceId) {
        Long id = parseDeviceId(deviceId);
        return pwmPinRepository
                .findByDevice_IdAndPinNumber(id, LIGHT_COLOR_PIN)
                .map(pin -> (int) (pin.getValue() & 0xFF))
                .map(v  -> (int) Math.round(v / 255.0 * 100))
                .orElse(50);
    }

    // ----------------------------------------------------------------
    // 4. SET ON/OFF — Turns light on or off
    // ----------------------------------------------------------------
    @Transactional
    public void setOnOff(String deviceId, boolean on) {
        Long id       = parseDeviceId(deviceId);
        Device device = getDevice(id);

        // Update pin state in DB
        DeviceDigitalPin pin = digitalPinRepository
                .findByDevice_IdAndPinNumber(id, LIGHT_ONOFF_PIN)
                .orElseGet(() -> DeviceDigitalPin.builder()
                        .device(device)
                        .pinNumber(LIGHT_ONOFF_PIN)
                        .state((byte) 0)
                        .build());

        pin.setState(on ? (byte) 1 : (byte) 0);
        digitalPinRepository.save(pin);

        // Build and publish MQTT payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", id);
        payload.put("meshId",   device.getMeshId());
        payload.put("dstMac",   device.getDstMac());
        payload.put("action",   "onOff");
        payload.put("state",    on ? 1 : 0);

        publishToMqtt(device, payload);
        saveDeviceCommand(device, payload);

        log.info("LightService: Device {} turned {}", deviceId, on ? "ON" : "OFF");
        homeGraphReportService.reportState("light-" + id, on, getBrightness(deviceId));
    }

    // ----------------------------------------------------------------
    // 5. SET BRIGHTNESS — Accepts 0-100% from Google
    // ----------------------------------------------------------------
    @Transactional
    public void setBrightness(String deviceId, int brightnessPercent) {
        Long id       = parseDeviceId(deviceId);
        Device device = getDevice(id);

        // Scale 0-100% → 0-255 for PWM byte storage
        byte pwmValue = (byte) Math.round(brightnessPercent / 100.0 * 255);

        DevicePwmPin pin = pwmPinRepository
                .findByDevice_IdAndPinNumber(id, LIGHT_BRIGHTNESS_PIN)
                .orElseGet(() -> DevicePwmPin.builder()
                        .device(device)
                        .pinNumber(LIGHT_BRIGHTNESS_PIN)
                        .value((byte) 255)
                        .build());

        pin.setValue(pwmValue);
        pwmPinRepository.save(pin);

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId",   id);
        payload.put("meshId",     device.getMeshId());
        payload.put("dstMac",     device.getDstMac());
        payload.put("action",     "brightness");
        payload.put("brightness", brightnessPercent);
        payload.put("pwmValue",   pwmValue & 0xFF);

        publishToMqtt(device, payload);
        saveDeviceCommand(device, payload);

        log.info("LightService: Device {} brightness → {}%", deviceId, brightnessPercent);
        homeGraphReportService.reportState("light-" + id, isOn(deviceId), brightnessPercent);
    }

    // ----------------------------------------------------------------
    // 6. SET COLOR TEMPERATURE — Accepts 0-100% (converted from Kelvin)
    // ----------------------------------------------------------------
    @Transactional
    public void setColorTemperature(String deviceId, int colorPercent) {
        Long id       = parseDeviceId(deviceId);
        Device device = getDevice(id);

        byte pwmValue = (byte) Math.round(colorPercent / 100.0 * 255);

        DevicePwmPin pin = pwmPinRepository
                .findByDevice_IdAndPinNumber(id, LIGHT_COLOR_PIN)
                .orElseGet(() -> DevicePwmPin.builder()
                        .device(device)
                        .pinNumber(LIGHT_COLOR_PIN)
                        .value((byte) 128)
                        .build());

        pin.setValue(pwmValue);
        pwmPinRepository.save(pin);

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId",     id);
        payload.put("meshId",       device.getMeshId());
        payload.put("dstMac",       device.getDstMac());
        payload.put("action",       "colorTemperature");
        payload.put("colorPercent", colorPercent);
        payload.put("pwmValue",     pwmValue & 0xFF);

        publishToMqtt(device, payload);
        saveDeviceCommand(device, payload);

        log.info("LightService: Device {} color temperature → {}%", deviceId, colorPercent);
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private Device getDevice(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + id));
    }

    /**
     * Parses Google's device ID format "light-{id}" → Long DB id.
     * e.g. "light-42" → 42L
     */
    private Long parseDeviceId(String deviceId) {
        try {
            return Long.parseLong(deviceId.replace("light-", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid device ID format: " + deviceId);
        }
    }

    /**
     * Publishes JSON payload to MQTT broker using your existing MqttService.
     * Topic format: "{gatewayMac}/{meshId}/pub"
     * Adjust MQTT_TOPIC_FORMAT constant above if your publish topic differs.
     */
    private void publishToMqtt(Device device, Map<String, Object> payload) {
        try {
            String topic = String.format(MQTT_TOPIC_FORMAT,
                    device.getGatewayMac(), device.getMeshId());
            String json  = objectMapper.writeValueAsString(payload);
            mqttService.publish(topic, json, 1, false);
        } catch (Exception e) {
            log.error("LightService: MQTT publish failed for device {}: {}",
                    device.getId(), e.getMessage());
            throw new RuntimeException("MQTT publish failed", e);
        }
    }

    /**
     * Persists every command sent to the device in device_commands table.
     */
    private void saveDeviceCommand(Device device, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            DeviceCommand command = DeviceCommand.builder()
                    .device(device)
                    .pktType(PKT_TYPE_LIGHT_CONTROL)
                    .actionCause(ACTION_CAUSE_GOOGLE)
                    .serializedPayload(json)
                    .jsonPayload(json)
                    .status("SENT")
                    .build();
            deviceCommandRepository.save(command);
        } catch (Exception e) {
            log.error("LightService: Failed to save DeviceCommand", e);
        }
    }
}