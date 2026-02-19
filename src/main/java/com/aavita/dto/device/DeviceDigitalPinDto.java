package com.aavita.dto.device;

import lombok.Data;

import java.time.Instant;

@Data
public class DeviceDigitalPinDto {

    private Long id;
    private Long deviceId;
    private Byte pinNumber;
    private Byte state;
    private Instant updatedOn;
}
