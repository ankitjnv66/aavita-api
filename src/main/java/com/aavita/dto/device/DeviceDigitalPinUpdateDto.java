package com.aavita.dto.device;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceDigitalPinUpdateDto {

    @NotNull
    private Byte state;
}
