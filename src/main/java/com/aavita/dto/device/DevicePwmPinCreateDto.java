package com.aavita.dto.device;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DevicePwmPinCreateDto {

    @NotNull
    private Long deviceId;

    @NotNull
    @Min(1) @Max(4)
    private Byte pinNumber;

    @NotNull
    @Min(0) @Max(255)
    private Byte value;

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public Byte getPinNumber() { return pinNumber; }
    public void setPinNumber(Byte pinNumber) { this.pinNumber = pinNumber; }
    public Byte getValue() { return value; }
    public void setValue(Byte value) { this.value = value; }
}
