package com.aavita.dto.device;

import lombok.Data;

@Data
public class DeviceDigitalPinResponse {

    private Integer pinNumber;
    private Byte state;
}
