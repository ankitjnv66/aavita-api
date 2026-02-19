package com.aavita.dto.device;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDigitalPinCreateDto {

    @NotNull
    private Long deviceId;

    @NotNull
    @Min(1) @Max(18)
    private Byte pinNumber;

    @NotNull
    private Byte state;

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public Byte getPinNumber() { return pinNumber; }
    public void setPinNumber(Byte pinNumber) { this.pinNumber = pinNumber; }
    public Byte getState() { return state; }
    public void setState(Byte state) { this.state = state; }
}
