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
public class FanService {

    private final DeviceRepository           deviceRepository;
    private final DeviceDigitalPinRepository digitalPinRepository;
    private final DevicePwmPinRepository     pwmPinRepository;
    private final DeviceCommandRepository    deviceCommandRepository;
    private final MqttService                mqttService;
    private final ObjectMapper               objectMapper;
    private final HomeGraphReportService     homeGraphReportService;

    private static final byte FAN_ONOFF_PIN   = 0;
    private static final byte FAN_SPEED_PIN   = 0;

    private static final int  PKT_TYPE_FAN_CONTROL = 0x11;
    private static final byte ACTION_CAUSE_GOOGLE  = 0x05;

    private static final String MQTT_TOPIC_FORMAT = "%s/%s/pub";

    public boolean isOn(String deviceId) {
        Long id = parseDeviceId(deviceId);
        return digitalPinRepository
                .findByDevice_IdAndPinNumber(id, FAN_ONOFF_PIN)
                .map(pin -> pin.getState() == 1)
                .orElse(false);
    }

    public int getSpeed(String deviceId) {
        Long id = parseDeviceId(deviceId);
        return pwmPinRepository
                .findByDevice_IdAndPinNumber(id, FAN_SPEED_PIN)
                .map(pin -> (int) (pin.getValue() & 0xFF))
                .map(v  -> (int) Math.round(v / 255.0 * 100))
                .orElse(100);
    }

    @Transactional
    public void setOnOff(String deviceId, boolean on) {
        Long id       = parseDeviceId(deviceId);
        Device device = getDevice(id);

        DeviceDigitalPin pin = digitalPinRepository
                .findByDevice_IdAndPinNumber(id, FAN_ONOFF_PIN)
                .orElseGet(() -> DeviceDigitalPin.builder()
                        .device(device)
                        .pinNumber(FAN_ONOFF_PIN)
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

        log.info("FanService: Device {} turned {}", deviceId, on ? "ON" : "OFF");
        homeGraphReportService.reportState("fan-" + id, on, getSpeed(deviceId));
    }

    @Transactional
    public void setSpeed(String deviceId, int speedPercent) {
        Long id       = parseDeviceId(deviceId);
        Device device = getDevice(id);

        byte pwmValue = (byte) Math.round(speedPercent / 100.0 * 255);

        DevicePwmPin pin = pwmPinRepository
                .findByDevice_IdAndPinNumber(id, FAN_SPEED_PIN)
                .orElseGet(() -> DevicePwmPin.builder()
                        .device(device)
                        .pinNumber(FAN_SPEED_PIN)
                        .value((byte) 255)
                        .build());

        pin.setValue(pwmValue);
        pwmPinRepository.save(pin);

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId",  id);
        payload.put("meshId",    device.getMeshId());
        payload.put("dstMac",    device.getDstMac());
        payload.put("action",    "speed");
        payload.put("speed",     speedPercent);
        payload.put("pwmValue",  pwmValue & 0xFF);

        publishToMqtt(device, payload);
        saveDeviceCommand(device, payload);

        log.info("FanService: Device {} speed → {}%", deviceId, speedPercent);
        homeGraphReportService.reportState("fan-" + id, isOn(deviceId), speedPercent);
    }

    private Device getDevice(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + id));
    }

    private Long parseDeviceId(String deviceId) {
        try {
            return Long.parseLong(deviceId.replace("fan-", ""));
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
            log.error("FanService: MQTT publish failed for device {}: {}",
                    device.getId(), e.getMessage());
            throw new RuntimeException("MQTT publish failed", e);
        }
    }

    private void saveDeviceCommand(Device device, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            DeviceCommand command = DeviceCommand.builder()
                    .device(device)
                    .pktType(PKT_TYPE_FAN_CONTROL)
                    .actionCause(ACTION_CAUSE_GOOGLE)
                    .serializedPayload(json)
                    .jsonPayload(json)
                    .status("SENT")
                    .build();
            deviceCommandRepository.save(command);
        } catch (Exception e) {
            log.error("FanService: Failed to save DeviceCommand", e);
        }
    }
}
