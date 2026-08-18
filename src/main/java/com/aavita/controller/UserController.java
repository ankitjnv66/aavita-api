package com.aavita.controller;

import com.aavita.dto.user.*;
import com.aavita.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @GetMapping("/test")
    public String testMethod() {
        return "testing done!!!";
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        UserResponse user = userService.getById(id);
        if (user == null) {
            log.warn("User not found, id: {}", id);
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }
        return ResponseEntity.ok(user);
    }

    // Add after getById() endpoint
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal String email) {
        UserResponse user = userService.getByEmail(email);
        if (user == null) {
            log.warn("User not found for authenticated email: {}", email);
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserResponse created = userService.create(request);
            log.info("User created, id: {}", created.getId());
            return ResponseEntity
                    .created(ServletUriComponentsBuilder.fromCurrentRequest()
                            .path("/{id}")
                            .buildAndExpand(created.getId())
                            .toUri())
                    .body(created);
        } catch (IllegalArgumentException e) {
            // Expected/handled case, e.g. duplicate email or invalid input
            log.warn("User creation rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        UserResponse updated = userService.update(id, request);
        if (updated == null) {
            log.warn("User update failed, not found, id: {}", id);
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }
        log.info("User updated, id: {}", id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        boolean deleted = userService.delete(id);
        if (!deleted) {
            log.warn("User delete failed, not found, id: {}", id);
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }
        log.info("User deleted, id: {}", id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }
}