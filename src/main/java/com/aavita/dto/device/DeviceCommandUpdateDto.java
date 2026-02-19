package com.aavita.dto.device;

import lombok.Data;

import java.time.Instant;

@Data
public class DeviceCommandUpdateDto {

    private Integer pktType;
    private Byte actionCause;
    private String serializedPayload;
    private String jsonPayload;
    private String status;
    private Instant executedOn;
}
