package com.aavita.mapper;

import com.aavita.dto.device.*;
import com.aavita.entity.Device;
import com.aavita.entity.DeviceDigitalPin;
import com.aavita.entity.DevicePwmPin;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    @Mapping(target = "siteId", expression = "java(device.getSite().getSiteId())")
    @Mapping(target = "digitalPins", expression = "java(mapDigitalPins(device))")
    @Mapping(target = "pwmPins", expression = "java(mapPwmPins(device))")
    @Mapping(target = "deviceName", source = "deviceName")
    @Mapping(target = "roomHint", source = "roomHint")
    DeviceResponse toResponse(Device device);

    default List<DeviceDigitalPinResponse> mapDigitalPins(Device device) {
        if (device.getDigitalPins() == null) return List.of();
        return device.getDigitalPins().stream()
                .map(this::toDigitalPinResponse)
                .collect(Collectors.toList());
    }

    default List<DevicePwmPinResponse> mapPwmPins(Device device) {
        if (device.getPwmPins() == null) return List.of();
        return device.getPwmPins().stream()
                .map(this::toPwmPinResponse)
                .collect(Collectors.toList());
    }

    DeviceDigitalPinResponse toDigitalPinResponse(DeviceDigitalPin pin);

    DevicePwmPinResponse toPwmPinResponse(DevicePwmPin pin);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "site", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "digitalPins", ignore = true)
    @Mapping(target = "pwmPins", ignore = true)
    @Mapping(target = "commands", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    @Mapping(target = "lastActionCause", ignore = true)
    @Mapping(target = "lastPktType", ignore = true)
    @Mapping(target = "lastCrc16", ignore = true)
    @Mapping(target = "lastSeen", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "pktId", constant = "0")
    @Mapping(target = "meshId", ignore = true)
    @Mapping(target = "dstMac", ignore = true)
    @Mapping(target = "gatewayMac", ignore = true)
    @Mapping(target = "subGatewayMac", ignore = true)
    Device toEntity(CreateDeviceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "site", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "srcMac", ignore = true)
    @Mapping(target = "dstMac", ignore = true)
    @Mapping(target = "digitalPins", ignore = true)
    @Mapping(target = "pwmPins", ignore = true)
    @Mapping(target = "commands", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    @Mapping(target = "pktId", ignore = true)
    @Mapping(target = "lastActionCause", ignore = true)
    @Mapping(target = "lastPktType", ignore = true)
    @Mapping(target = "lastCrc16", ignore = true)
    @Mapping(target = "lastSeen", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    void updateFromRequest(UpdateDeviceRequest request, @MappingTarget Device device);
}
