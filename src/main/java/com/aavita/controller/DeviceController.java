package com.aavita.controller;

import com.aavita.dto.device.*;
import com.aavita.service.DeviceCommandService;
import com.aavita.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceCommandService deviceCommandService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(deviceService.getAll());
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<?> getById(@PathVariable Long deviceId) {
        DeviceResponse device = deviceService.getById(deviceId);
        if (device == null) {
            log.warn("Device not found, id: {}", deviceId);
            return ResponseEntity.status(404).body(Map.of("message", "Device with id " + deviceId + " not found"));
        }
        return ResponseEntity.ok(device);
    }

    @GetMapping("/site/{siteId}")
    public ResponseEntity<?> getBySite(@PathVariable UUID siteId) {
        return ResponseEntity.ok(deviceService.getBySiteId(siteId));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateDeviceRequest request) {
        DeviceResponse created = deviceService.create(request);
        log.info("Device created, id: {}", created.getId());
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(created.getId())
                        .toUri())
                .body(created);
    }

    @PutMapping("/{deviceId}")
    public ResponseEntity<?> update(@PathVariable Long deviceId, @Valid @RequestBody UpdateDeviceRequest request) {
        DeviceResponse updated = deviceService.update(deviceId, request);
        if (updated == null) {
            log.warn("Device update failed, not found, id: {}", deviceId);
            return ResponseEntity.status(404).body(Map.of("message", "Device with id " + deviceId + " not found"));
        }
        log.info("Device updated, id: {}", deviceId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<?> delete(@PathVariable Long deviceId) {
        boolean deleted = deviceService.delete(deviceId);
        if (!deleted) {
            log.warn("Device delete failed, not found, id: {}", deviceId);
            return ResponseEntity.status(404).body(Map.of("message", "Device with id " + deviceId + " not found"));
        }
        log.info("Device deleted, id: {}", deviceId);
        return ResponseEntity.ok(Map.of("message", "Device with id " + deviceId + " deleted successfully"));
    }

    @PostMapping("/command")
    public ResponseEntity<?> sendCommand(@Valid @RequestBody DeviceCommandRequest request) {
        try {
            Map<String, Object> result = deviceCommandService.sendCommand(request);
            log.info("Device command sent successfully, deviceId: {}", request.getDeviceId());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            // Expected/handled case, e.g. invalid device id or malformed command payload
            log.warn("Device command rejected, deviceId: {} | reason: {}", request.getDeviceId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Unexpected failure (MQTT publish failure, serialization error, etc.)
            log.error("Failed to send device command, deviceId: {}", request.getDeviceId(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to send command",
                    "error", e.getMessage()));
        }
    }
}