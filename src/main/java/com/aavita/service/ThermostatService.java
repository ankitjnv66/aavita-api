package com.aavita.service;

import com.aavita.entity.*;
import com.aavita.mqtt.MqttService;
import com.aavita.repository.*;
import com.aavita.service.google.HomeGraphReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThermostatService {

    private final DeviceRepository           deviceRepository;
    private final DeviceDigitalPinRepository digitalPinRepository;
    private final DevicePwmPinRepository     pwmPinRepository;
    private final DeviceCommandRepository    deviceCommandRepository;
    private final MqttService                mqttService;
    private final ObjectMapper               objectMapper;
    private final HomeGraphReportService     homeGraphReportService;

    // Pin conventions
    private static final byte THERMOSTAT_ONOFF_PIN = 0;
    private static final byte THERMOSTAT_TEMP_PIN  = 0;

    // Temperature range (Celsius)
    private static final double MIN_TEMP = 16.0;
    private static final double MAX_TEMP = 30.0;

    private static final int  PKT_TYPE_THERMOSTAT_CONTROL = 0x12;
    private static final byte ACTION_CAUSE_GOOGLE         = 0x05;

    private static final String MQTT_TOPIC_FORMAT = "%s/%s/pub";

    public boolean isOn(String deviceId) {
        Long id = parseDeviceId(deviceId);
        return digitalPinRepository
                .findByDevice_IdAndPinNumber(id, THERMOSTAT_ONOFF_PIN)
                .map(pin -> pin.getState() == 1)
                .orElse(false);
    }

    // Returns temperature in Celsius (16-30°C)
    public double getTemperature(String deviceId) {
        Long id = parseDeviceId(deviceId);
        return pwmPinRepository
                .findByDevice_IdAndPinNumber(id, THERMOSTAT_TEMP_PIN)
                .map(pin -> (int) (pin.getValue() & 0xFF))
                .map(v -> MIN_TEMP + (v / 255.0) * (MAX_TEMP - MIN_TEMP))
                .orElse(24.0);
    }

    @Transactional
    public void setOnOff(String deviceId, boolean on) {
        Long id       = parseDeviceId(deviceId);
        Device device = getDevice(id);

        DeviceDigitalPin pin = digitalPinRepository
                .findByDevice_IdAndPinNumber(id, THERMOSTAT_ONOFF_PIN)
                .orElseGet(() -> DeviceDigitalPin.builder()
                        .device(device)
                        .pinNumber(THERMOSTAT_ONOFF_PIN)
                        .state((byte) 0)
                        .build());

        pin.setState(on ? (byte) 1 : (byte) 0);
        digitalPinRepository.save(pin);

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", id);
        payload.put("meshId",   device.getMeshId());
        payload.put("dstMac",   device.getDstMac());
        payload.put("action",   "onOff");
        payload.put("state",    on ? 1 : 0);

        publishToMqtt(device, payload);
        saveDeviceCommand(device, payload);

        log.info("ThermostatService: Device {} turned {}", deviceId, on ? "ON" : "OFF");
        homeGraphReportService.reportThermostatState("thermostat-" + id, on, getTemperature(deviceId));
    }

    @Transactional
    public void setTemperature(String deviceId, double tempCelsius) {
        Long id       = parseDeviceId(deviceId);
        Device device = getDevice(id);

        // Clamp temperature to valid range
        double clampedTemp = Math.max(MIN_TEMP, Math.min(MAX_TEMP, tempCelsius));

        // Scale 16-30°C → 0-255 for PWM byte storage
        byte pwmValue = (byte) Math.round((clampedTemp - MIN_TEMP) / (MAX_TEMP - MIN_TEMP) * 255);

        DevicePwmPin pin = pwmPinRepository
                .findByDevice_IdAndPinNumber(id, THERMOSTAT_TEMP_PIN)
                .orElseGet(() -> DevicePwmPin.builder()
                        .device(device)
                        .pinNumber(THERMOSTAT_TEMP_PIN)
                        .value((byte) 128)
                        .build());

        pin.setValue(pwmValue);
        pwmPinRepository.save(pin);

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId",     id);
        payload.put("meshId",       device.getMeshId());
        payload.put("dstMac",       device.getDstMac());
        payload.put("action",       "setTemperature");
        payload.put("temperature",  clampedTemp);
        payload.put("pwmValue",     pwmValue & 0xFF);

        publishToMqtt(device, payload);
        saveDeviceCommand(device, payload);

        log.info("ThermostatService: Device {} temperature → {}°C", deviceId, clampedTemp);
        homeGraphReportService.reportThermostatState("thermostat-" + id, isOn(deviceId), clampedTemp);
    }

    private Device getDevice(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + id));
    }

    private Long parseDeviceId(String deviceId) {
        try {
            return Long.parseLong(deviceId.replace("thermostat-", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid device ID format: " + deviceId);
        }
    }

    private void publishToMqtt(Device device, Map<String, Object> payload) {
        try {
            String topic = String.format(MQTT_TOPIC_FORMAT,
                    device.getGatewayMac(), device.getMeshId());
            String json  = objectMapper.writeValueAsString(payload);
            mqttService.publish(topic, json, 1, false);
        } catch (Exception e) {
            log.error("ThermostatService: MQTT publish failed for device {}: {}",
                    device.getId(), e.getMessage());
            throw new RuntimeException("MQTT publish failed", e);
        }
    }

    private void saveDeviceCommand(Device device, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            DeviceCommand command = DeviceCommand.builder()
                    .device(device)
                    .pktType(PKT_TYPE_THERMOSTAT_CONTROL)
                    .actionCause(ACTION_CAUSE_GOOGLE)
                    .serializedPayload(json)
                    .jsonPayload(json)
                    .status("SENT")
                    .build();
            deviceCommandRepository.save(command);
        } catch (Exception e) {
            log.error("ThermostatService: Failed to save DeviceCommand", e);
        }
    }
}
