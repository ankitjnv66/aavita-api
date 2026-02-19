package com.aavita.dto.device;

import lombok.Data;

@Data
public class UpdateDeviceRequest {

    private String meshId;
    private String gatewayMac;
    private String subGatewayMac;
    private Byte boardType;
    private Byte deviceType;
    private Byte deviceRole;
}
