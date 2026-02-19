package com.aavita.mapper;

import com.aavita.dto.device.CreateDeviceRequest;
import com.aavita.dto.device.DeviceDigitalPinResponse;
import com.aavita.dto.device.DevicePwmPinResponse;
import com.aavita.dto.device.DeviceResponse;
import com.aavita.dto.device.UpdateDeviceRequest;
import com.aavita.entity.Device;
import com.aavita.entity.DeviceDigitalPin;
import com.aavita.entity.DevicePwmPin;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-19T23:47:35+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.5 (Amazon.com Inc.)"
)
@Component
public class DeviceMapperImpl implements DeviceMapper {

    @Override
    public DeviceResponse toResponse(Device device) {
        if ( device == null ) {
            return null;
        }

        DeviceResponse.DeviceResponseBuilder deviceResponse = DeviceResponse.builder();

        deviceResponse.id( device.getId() );
        deviceResponse.meshId( device.getMeshId() );
        deviceResponse.srcMac( device.getSrcMac() );
        deviceResponse.gatewayMac( device.getGatewayMac() );
        deviceResponse.subGatewayMac( device.getSubGatewayMac() );
        deviceResponse.boardType( device.getBoardType() );
        deviceResponse.deviceType( device.getDeviceType() );
        deviceResponse.deviceRole( device.getDeviceRole() );
        deviceResponse.lastActionCause( device.getLastActionCause() );
        deviceResponse.lastPktType( device.getLastPktType() );
        deviceResponse.lastCrc16( device.getLastCrc16() );
        deviceResponse.lastSeen( device.getLastSeen() );

        deviceResponse.siteId( device.getSite().getSiteId() );
        deviceResponse.digitalPins( mapDigitalPins(device) );
        deviceResponse.pwmPins( mapPwmPins(device) );

        return deviceResponse.build();
    }

    @Override
    public DeviceDigitalPinResponse toDigitalPinResponse(DeviceDigitalPin pin) {
        if ( pin == null ) {
            return null;
        }

        DeviceDigitalPinResponse deviceDigitalPinResponse = new DeviceDigitalPinResponse();

        if ( pin.getPinNumber() != null ) {
            deviceDigitalPinResponse.setPinNumber( pin.getPinNumber().intValue() );
        }
        deviceDigitalPinResponse.setState( pin.getState() );

        return deviceDigitalPinResponse;
    }

    @Override
    public DevicePwmPinResponse toPwmPinResponse(DevicePwmPin pin) {
        if ( pin == null ) {
            return null;
        }

        DevicePwmPinResponse devicePwmPinResponse = new DevicePwmPinResponse();

        if ( pin.getPinNumber() != null ) {
            devicePwmPinResponse.setPinNumber( pin.getPinNumber().intValue() );
        }
        devicePwmPinResponse.setValue( pin.getValue() );

        return devicePwmPinResponse;
    }

    @Override
    public Device toEntity(CreateDeviceRequest request) {
        if ( request == null ) {
            return null;
        }

        Device.DeviceBuilder device = Device.builder();

        device.srcMac( request.getSrcMac() );
        device.boardType( request.getBoardType() );
        device.deviceType( request.getDeviceType() );
        device.deviceRole( request.getDeviceRole() );

        device.pktId( 0 );

        return device.build();
    }

    @Override
    public void updateFromRequest(UpdateDeviceRequest request, Device device) {
        if ( request == null ) {
            return;
        }

        device.setMeshId( request.getMeshId() );
        device.setGatewayMac( request.getGatewayMac() );
        device.setSubGatewayMac( request.getSubGatewayMac() );
        device.setBoardType( request.getBoardType() );
        device.setDeviceType( request.getDeviceType() );
        device.setDeviceRole( request.getDeviceRole() );
    }
}
