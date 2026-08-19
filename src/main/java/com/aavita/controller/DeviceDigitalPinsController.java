package com.aavita.controller;

import com.aavita.dto.device.*;
import com.aavita.service.DeviceDigitalPinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RestController
@RequestMapping("/api/device-digital-pins")
@RequiredArgsConstructor
public class DeviceDigitalPinsController {

    private final DeviceDigitalPinService service;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        DeviceDigitalPinDto pin = service.getById(id);
        if (pin == null) {
            log.warn("Digital pin not found, id: {}", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pin);
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<?> getByDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(service.getByDeviceId(deviceId));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DeviceDigitalPinCreateDto dto) {
        DeviceDigitalPinDto created = service.create(dto);
        log.info("Digital pin created, id: {}", created.getId());
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(created.getId())
                        .toUri())
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody DeviceDigitalPinUpdateDto dto) {
        DeviceDigitalPinDto updated = service.update(id, dto);
        if (updated == null) {
            log.warn("Digital pin update failed, not found, id: {}", id);
            return ResponseEntity.notFound().build();
        }
        log.info("Digital pin updated, id: {}", id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        boolean ok = service.delete(id);
        if (!ok) {
            log.warn("Digital pin delete failed, not found, id: {}", id);
            return ResponseEntity.notFound().build();
        }
        log.info("Digital pin deleted, id: {}", id);
        return ResponseEntity.noContent().build();
    }
}