package com.aavita.dto.site;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SiteCreateDto {

    @NotNull
    private Long userId;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Location is required")
    private String location;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
