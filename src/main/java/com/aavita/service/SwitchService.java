package com.aavita.service;

import com.aavita.dto.device.DeviceCommandRequest;
import com.aavita.entity.Device;
import com.aavita.entity.DeviceDigitalPin;
import com.aavita.mqtt.DeviceCommandPublisher;
import com.aavita.repository.DeviceDigitalPinRepository;
import com.aavita.repository.DeviceRepository;
import com.aavita.service.google.HomeGraphReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * SwitchService — Google Home → individual GPIO pin control on ESP8266.
 *
 * Uses the EXISTING DeviceCommandPublisher infrastructure:
 *   command = "SET_PIN"
 *   payload = "{pinNumber}={espState}"
 *             e.g. "5=1" → pin 5 ON
 *                  "5=2" → pin 5 OFF
 *
 * PinCommandBuilder (fixed) reads ALL current pin states from DB first,
 * so only the target pin changes — other 17 pins are NOT affected.
 *
 * Google Home device ID format: "switch-{deviceId}-{pinNumber}"
 * e.g. "switch-42-5" → Device id=42, GPIO pin 5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SwitchService {

    private final DeviceRepository           deviceRepository;
    private final DeviceDigitalPinRepository digitalPinRepository;
    private final DeviceCommandPublisher     deviceCommandPublisher;
    private final HomeGraphReportService     homeGraphReportService;

    // ----------------------------------------------------------------
    // IS ON — read current state from DeviceDigitalPin table
    // ----------------------------------------------------------------
    public boolean isOn(String googleDeviceId) {
        ParsedId parsed = parseDeviceId(googleDeviceId);
        return digitalPinRepository
                .findByDevice_IdAndPinNumber(parsed.deviceId, parsed.pinNumber)
                .map(pin -> pin.getState() == 1)
                .orElse(false);
    }

    // ----------------------------------------------------------------
    // SET ON/OFF
    // 1. Update DeviceDigitalPin.state in DB
    // 2. Publish SET_PIN via DeviceCommandPublisher
    //    PinCommandBuilder reads ALL pin states from DB → sends full
    //    18-element array → only target pin changes on ESP8266
    // 3. Report state back to Google HomeGraph
    // ----------------------------------------------------------------
    @Transactional
    public void setOnOff(String googleDeviceId, boolean on) {
        ParsedId parsed = parseDeviceId(googleDeviceId);
        Device device   = getDevice(parsed.deviceId);

        // Step 1: Persist new state in DB BEFORE building payload
        // (PinCommandBuilder will read this updated state)
        DeviceDigitalPin pin = digitalPinRepository
                .findByDevice_IdAndPinNumber(parsed.deviceId, parsed.pinNumber)
                .orElseGet(() -> {
                    DeviceDigitalPin newPin = new DeviceDigitalPin();
                    newPin.setDevice(device);
                    newPin.setPinNumber(parsed.pinNumber);
                    newPin.setState((byte) 0);
                    newPin.setUpdatedOn(Instant.now());
                    return newPin;
                });

        pin.setState(on ? (byte) 1 : (byte) 0);
        pin.setUpdatedOn(Instant.now());
        digitalPinRepository.save(pin);

        // Step 2: Build and publish via existing DeviceCommandPublisher
        // Payload: "{pinNumber}={espState}"  (1=ON, 2=OFF — ESP8266 protocol)
        String payloadStr = parsed.pinNumber + "=" + (on ? "1" : "2");

        DeviceCommandRequest request = new DeviceCommandRequest();
        request.setDeviceId(parsed.deviceId);
        request.setSiteId(device.getSite().getSiteId());
        request.setUsername(device.getSite().getUsername());
        request.setCommand("SET_PIN");
        request.setPayload(payloadStr);

        try {
            deviceCommandPublisher.buildAndPublish(request);
            log.info("SwitchService: device={} pin={} → {} | MQTT published via DeviceCommandPublisher",
                    parsed.deviceId, parsed.pinNumber, on ? "ON" : "OFF");
        } catch (Exception e) {
            log.error("SwitchService: MQTT publish failed device={} pin={}: {}",
                    parsed.deviceId, parsed.pinNumber, e.getMessage());
            throw new RuntimeException("MQTT publish failed", e);
        }

        // Step 3: Report to Google HomeGraph so Google Home UI updates
        homeGraphReportService.reportSwitchState(googleDeviceId, on);
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private Device getDevice(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + id));
    }

    /**
     * Parses "switch-{deviceId}-{pinNumber}"
     * Uses lastIndexOf so deviceId with large numbers works correctly.
     * e.g. "switch-42-5"  → deviceId=42, pin=5
     *      "switch-100-12" → deviceId=100, pin=12
     */
    private ParsedId parseDeviceId(String googleDeviceId) {
        try {
            String stripped = googleDeviceId.replace("switch-", "");
            int lastDash    = stripped.lastIndexOf('-');
            if (lastDash < 0) throw new IllegalArgumentException("No pin separator found");
            Long deviceDbId = Long.parseLong(stripped.substring(0, lastDash));
            Byte pinNum     = Byte.parseByte(stripped.substring(lastDash + 1));
            return new ParsedId(deviceDbId, pinNum);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid switch ID: " + googleDeviceId + " — expected switch-{deviceId}-{pinNumber}");
        }
    }

    private record ParsedId(Long deviceId, Byte pinNumber) {}
}