package com.aavita.dto.device;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DevicePwmPinUpdateDto {

    @NotNull
    @jakarta.validation.constraints.Min(0)
    @jakarta.validation.constraints.Max(255)
    private Byte value;

    public Byte getValue() { return value; }
    public void setValue(Byte value) { this.value = value; }
}
