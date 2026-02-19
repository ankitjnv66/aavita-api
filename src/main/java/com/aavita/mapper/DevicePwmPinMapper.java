package com.aavita.mapper;

import com.aavita.dto.device.DevicePwmPinCreateDto;
import com.aavita.dto.device.DevicePwmPinDto;
import com.aavita.dto.device.DevicePwmPinResponse;
import com.aavita.entity.DevicePwmPin;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DevicePwmPinMapper {

    DevicePwmPinDto toDto(DevicePwmPin entity);

    DevicePwmPinResponse toResponse(DevicePwmPin entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "device", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    DevicePwmPin toEntity(DevicePwmPinCreateDto dto);
}
