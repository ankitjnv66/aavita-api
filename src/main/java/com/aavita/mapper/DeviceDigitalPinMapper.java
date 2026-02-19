package com.aavita.mapper;

import com.aavita.dto.device.DeviceDigitalPinDto;
import com.aavita.dto.device.DeviceDigitalPinResponse;
import com.aavita.entity.DeviceDigitalPin;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeviceDigitalPinMapper {

    DeviceDigitalPinDto toDto(DeviceDigitalPin entity);

    DeviceDigitalPinResponse toResponse(DeviceDigitalPin entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "device", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    DeviceDigitalPin toEntity(com.aavita.dto.device.DeviceDigitalPinCreateDto dto);
}
