package com.aavita.mqtt;

import com.aavita.dto.device.DeviceCommandRequest;
import com.aavita.entity.Device;
import com.aavita.repository.DeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeviceCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeviceCommandPublisher.class);

    private final DeviceRepository deviceRepository;
    private final PinCommandBuilder pinBuilder;
    private final PwmCommandBuilder pwmBuilder;
    private final JsonCommandBuilder jsonBuilder;
    private final MqttService mqttService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String buildAndPublish(DeviceCommandRequest request) {
        if (request.getSiteId() == null) {
            throw new IllegalArgumentException("SiteId is required");
        }

        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device with Id " + request.getDeviceId() + " not found"));

        if (request.getPayload() == null || request.getPayload().isBlank()) {
            throw new IllegalArgumentException("Payload is required");
        }

        com.aavita.mqtt.model.DevicePayload payload;
        String command = request.getCommand().toUpperCase();
        switch (command) {
            case "SET_PIN" -> payload = buildPinCommand(request.getPayload(), device);
            case "SET_PWM" -> payload = buildPwmCommand(request.getPayload(), device);
            default -> throw new IllegalArgumentException("Unknown command: " + request.getCommand());
        }

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String base64 = Base64.getEncoder().encodeToString(payloadJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String json = jsonBuilder.build(request.getSiteId(), base64);
            String topic = request.getUsername() + "/" + request.getSiteId() + "/pub";
            mqttService.publish(topic, json, 0, false);
            return json;
        } catch (Exception ex) {
            log.error("Error processing device build and publish command", ex);
            throw new RuntimeException(ex);
        }
    }

    private com.aavita.mqtt.model.DevicePayload buildPinCommand(String payload, Device device) {
        String[] p = payload.split("=");
        int pin;
        boolean state;

        if (p.length == 3) {
            pin = Integer.parseInt(p[1]);
            state = "1".equals(p[2]);
        } else if (p.length == 2) {
            pin = Integer.parseInt(p[0]);
            state = "1".equals(p[1]);
        } else {
            throw new IllegalArgumentException("Invalid SET_PIN payload format");
        }
        return pinBuilder.build(pin, state, device);
    }

    private com.aavita.mqtt.model.DevicePayload buildPwmCommand(String payload, Device device) {
        String[] p = payload.split("=");
        int pwmIndex;
        byte value;

        if (p.length == 3) {
            pwmIndex = Integer.parseInt(p[1]);
            value = Byte.parseByte(p[2]);
        } else if (p.length == 2) {
            pwmIndex = Integer.parseInt(p[0]);
            value = Byte.parseByte(p[1]);
        } else {
            throw new IllegalArgumentException("Invalid SET_PWM payload format");
        }
        return pwmBuilder.build(pwmIndex, value, device);
    }
}
