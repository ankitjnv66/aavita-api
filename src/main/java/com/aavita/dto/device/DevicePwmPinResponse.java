package com.aavita.dto.device;

import lombok.Data;

@Data
public class DevicePwmPinResponse {

    private Integer pinNumber;
    private Byte value;
}
