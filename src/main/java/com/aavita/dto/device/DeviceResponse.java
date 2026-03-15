package com.aavita.dto.device;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class DeviceResponse {

    private Long id;
    private java.util.UUID siteId;
    private String meshId;
    private String srcMac;
    private String gatewayMac;
    private String subGatewayMac;
    private Byte boardType;
    private Byte deviceType;
    private Byte deviceRole;
    private Byte lastActionCause;
    private Integer lastPktType;
    private Integer lastCrc16;
    private Instant lastSeen;
    private List<DeviceDigitalPinResponse> digitalPins;
    private List<DevicePwmPinResponse> pwmPins;
    private String deviceName;
    private String roomHint;
}
