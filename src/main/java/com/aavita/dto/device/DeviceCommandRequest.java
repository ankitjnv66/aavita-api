package com.aavita.dto.device;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCommandRequest {

    @NotNull
    @Positive
    private Long deviceId;

    @NotBlank(message = "Command is required")
    private String command;

    private String payload;

    private String username;
    private UUID siteId;

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public UUID getSiteId() { return siteId; }
    public void setSiteId(UUID siteId) { this.siteId = siteId; }
}
