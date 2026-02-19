package com.aavita.controller;

import com.aavita.dto.device.*;
import com.aavita.service.DeviceCommandCrudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
        if (cmd == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(cmd);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DeviceCommandCreateDto dto) {
        DeviceCommandDto created = service.create(dto);
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
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        boolean ok = service.delete(id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
