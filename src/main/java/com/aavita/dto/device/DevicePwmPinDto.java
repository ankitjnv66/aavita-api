package com.aavita.dto.device;

import lombok.Data;

import java.time.Instant;

@Data
public class DevicePwmPinDto {

    private Long id;
    private Long deviceId;
    private Byte pinNumber;
    private Byte value;
    private Instant updatedOn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
