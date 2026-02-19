package com.aavita.dto.device;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DeviceCommandCreateDto {

    @NotNull
    private Long deviceId;

    @NotNull
    private Integer pktType;

    @NotNull
    private Byte actionCause;

    @NotBlank
    private String serializedPayload;

    private String jsonPayload;

    @NotBlank
    private String status;
}
