package com.aavita.mapper;

import com.aavita.dto.device.DevicePwmPinCreateDto;
import com.aavita.dto.device.DevicePwmPinDto;
import com.aavita.dto.device.DevicePwmPinResponse;
import com.aavita.entity.DevicePwmPin;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-19T23:47:35+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.5 (Amazon.com Inc.)"
)
@Component
public class DevicePwmPinMapperImpl implements DevicePwmPinMapper {

    @Override
    public DevicePwmPinDto toDto(DevicePwmPin entity) {
        if ( entity == null ) {
            return null;
        }

        DevicePwmPinDto devicePwmPinDto = new DevicePwmPinDto();

        devicePwmPinDto.setId( entity.getId() );
        devicePwmPinDto.setPinNumber( entity.getPinNumber() );
        devicePwmPinDto.setValue( entity.getValue() );
        devicePwmPinDto.setUpdatedOn( entity.getUpdatedOn() );

        return devicePwmPinDto;
    }

    @Override
    public DevicePwmPinResponse toResponse(DevicePwmPin entity) {
        if ( entity == null ) {
            return null;
        }

        DevicePwmPinResponse devicePwmPinResponse = new DevicePwmPinResponse();

        if ( entity.getPinNumber() != null ) {
            devicePwmPinResponse.setPinNumber( entity.getPinNumber().intValue() );
        }
        devicePwmPinResponse.setValue( entity.getValue() );

        return devicePwmPinResponse;
    }

    @Override
    public DevicePwmPin toEntity(DevicePwmPinCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        DevicePwmPin.DevicePwmPinBuilder devicePwmPin = DevicePwmPin.builder();

        devicePwmPin.pinNumber( dto.getPinNumber() );
        devicePwmPin.value( dto.getValue() );

        return devicePwmPin.build();
    }
}
