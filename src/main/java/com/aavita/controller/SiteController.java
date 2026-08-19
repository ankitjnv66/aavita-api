package com.aavita.controller;

import com.aavita.dto.site.SiteCreateDto;
import com.aavita.dto.site.SiteDto;
import com.aavita.dto.site.SiteUpdateDto;
import com.aavita.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(siteService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        SiteDto site = siteService.getById(id);
        if (site == null) {
            log.warn("Site not found, id: {}", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(site);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(siteService.getByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody SiteCreateDto dto) {
        SiteDto created = siteService.create(dto);
        log.info("Site created, id: {}", created.getSiteId());
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(created.getSiteId())
                        .toUri())
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody SiteUpdateDto dto) {
        SiteDto updated = siteService.update(id, dto);
        if (updated == null) {
            log.warn("Site update failed, not found, id: {}", id);
            return ResponseEntity.notFound().build();
        }
        log.info("Site updated, id: {}", id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        boolean ok = siteService.delete(id);
        if (!ok) {
            log.warn("Site delete failed, not found, id: {}", id);
            return ResponseEntity.notFound().build();
        }
        log.info("Site deleted, id: {}", id);
        return ResponseEntity.noContent().build();
    }
}