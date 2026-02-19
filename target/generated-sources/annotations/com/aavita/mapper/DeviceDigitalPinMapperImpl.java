package com.aavita.mapper;

import com.aavita.dto.device.DeviceDigitalPinCreateDto;
import com.aavita.dto.device.DeviceDigitalPinDto;
import com.aavita.dto.device.DeviceDigitalPinResponse;
import com.aavita.entity.DeviceDigitalPin;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-19T23:47:35+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.5 (Amazon.com Inc.)"
)
@Component
public class DeviceDigitalPinMapperImpl implements DeviceDigitalPinMapper {

    @Override
    public DeviceDigitalPinDto toDto(DeviceDigitalPin entity) {
        if ( entity == null ) {
            return null;
        }

        DeviceDigitalPinDto deviceDigitalPinDto = new DeviceDigitalPinDto();

        deviceDigitalPinDto.setId( entity.getId() );
        deviceDigitalPinDto.setPinNumber( entity.getPinNumber() );
        deviceDigitalPinDto.setState( entity.getState() );
        deviceDigitalPinDto.setUpdatedOn( entity.getUpdatedOn() );

        return deviceDigitalPinDto;
    }

    @Override
    public DeviceDigitalPinResponse toResponse(DeviceDigitalPin entity) {
        if ( entity == null ) {
            return null;
        }

        DeviceDigitalPinResponse deviceDigitalPinResponse = new DeviceDigitalPinResponse();

        if ( entity.getPinNumber() != null ) {
            deviceDigitalPinResponse.setPinNumber( entity.getPinNumber().intValue() );
        }
        deviceDigitalPinResponse.setState( entity.getState() );

        return deviceDigitalPinResponse;
    }

    @Override
    public DeviceDigitalPin toEntity(DeviceDigitalPinCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        DeviceDigitalPin.DeviceDigitalPinBuilder deviceDigitalPin = DeviceDigitalPin.builder();

        deviceDigitalPin.pinNumber( dto.getPinNumber() );
        deviceDigitalPin.state( dto.getState() );

        return deviceDigitalPin.build();
    }
}
