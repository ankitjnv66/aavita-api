package com.aavita.controller;

import com.aavita.dto.device.*;
import com.aavita.service.DeviceCommandService;
import com.aavita.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

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
            return ResponseEntity.status(404).body(Map.of("message", "Device with id " + deviceId + " not found"));
        }
        return ResponseEntity.ok(device);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateDeviceRequest request) {
        DeviceResponse created = deviceService.create(request);
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
            return ResponseEntity.status(404).body(Map.of("message", "Device with id " + deviceId + " not found"));
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<?> delete(@PathVariable Long deviceId) {
        boolean deleted = deviceService.delete(deviceId);
        if (!deleted) {
            return ResponseEntity.status(404).body(Map.of("message", "Device with id " + deviceId + " not found"));
        }
        return ResponseEntity.ok(Map.of("message", "Device with id " + deviceId + " deleted successfully"));
    }

    @PostMapping("/command")
    public ResponseEntity<?> sendCommand(@Valid @RequestBody DeviceCommandRequest request) {
        try {
            Map<String, Object> result = deviceCommandService.sendCommand(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to send command",
                    "error", e.getMessage()));
        }
    }
}
