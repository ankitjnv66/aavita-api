package com.aavita.mapper;

import com.aavita.dto.device.DeviceCommandDto;
import com.aavita.entity.DeviceCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeviceCommandMapper {

    DeviceCommandDto toDto(DeviceCommand entity);
}
