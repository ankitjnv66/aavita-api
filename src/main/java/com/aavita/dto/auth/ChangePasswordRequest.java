package com.aavita.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6)
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
