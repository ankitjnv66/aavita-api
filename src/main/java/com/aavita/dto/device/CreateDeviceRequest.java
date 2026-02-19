package com.aavita.dto.device;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateDeviceRequest {

    @NotNull
    private UUID siteId;

    @NotNull
    private Long userId;

    private String meshId;

    @NotBlank(message = "Source MAC is required")
    private String srcMac;

    private String dstMac;
    private String gatewayMac;
    private String subGatewayMac;

    @NotNull
    private Byte boardType;

    @NotNull
    private Byte deviceType;

    @NotNull
    private Byte deviceRole;
}
