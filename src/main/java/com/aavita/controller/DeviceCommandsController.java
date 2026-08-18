package com.aavita.controller;

import com.aavita.dto.device.*;
import com.aavita.service.DeviceCommandCrudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RestController
@RequestMapping("/api/device-commands")
@RequiredArgsConstructor
public class DeviceCommandsController {

    private final DeviceCommandCrudService service;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        DeviceCommandDto cmd = service.getById(id);
        if (cmd == null) {
            log.warn("Device command not found, id: {}", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cmd);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DeviceCommandCreateDto dto) {
        DeviceCommandDto created = service.create(dto);
        log.info("Device command created, id: {}", created.getId());
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(created.getId())
                        .toUri())
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody DeviceCommandUpdateDto dto) {
        DeviceCommandDto updated = service.update(id, dto);
        if (updated == null) {
            log.warn("Device command update failed, not found, id: {}", id);
            return ResponseEntity.notFound().build();
        }
        log.info("Device command updated, id: {}", id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        boolean ok = service.delete(id);
        if (!ok) {
            log.warn("Device command delete failed, not found, id: {}", id);
            return ResponseEntity.notFound().build();
        }
        log.info("Device command deleted, id: {}", id);
        return ResponseEntity.noContent().build();
    }
}