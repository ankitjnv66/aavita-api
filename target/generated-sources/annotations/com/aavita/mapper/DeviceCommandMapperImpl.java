package com.aavita.mapper;

import com.aavita.dto.device.DeviceCommandDto;
import com.aavita.entity.DeviceCommand;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-19T23:47:35+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.5 (Amazon.com Inc.)"
)
@Component
public class DeviceCommandMapperImpl implements DeviceCommandMapper {

    @Override
    public DeviceCommandDto toDto(DeviceCommand entity) {
        if ( entity == null ) {
            return null;
        }

        DeviceCommandDto deviceCommandDto = new DeviceCommandDto();

        deviceCommandDto.setId( entity.getId() );
        deviceCommandDto.setPktType( entity.getPktType() );
        deviceCommandDto.setActionCause( entity.getActionCause() );
        deviceCommandDto.setSerializedPayload( entity.getSerializedPayload() );
        deviceCommandDto.setJsonPayload( entity.getJsonPayload() );
        deviceCommandDto.setStatus( entity.getStatus() );
        deviceCommandDto.setCreatedOn( entity.getCreatedOn() );
        deviceCommandDto.setExecutedOn( entity.getExecutedOn() );

        return deviceCommandDto;
    }
}
