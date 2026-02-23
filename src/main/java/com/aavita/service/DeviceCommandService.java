package com.aavita.service;

import com.aavita.dto.device.DeviceCommandDto;
import com.aavita.dto.device.DeviceCommandRequest;
import com.aavita.entity.Device;
import com.aavita.entity.Site;
import com.aavita.mqtt.DeviceCommandPublisher;
import com.aavita.repository.DeviceRepository;
import com.aavita.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceCommandService {

    private final DeviceRepository deviceRepository;
    private final SiteRepository siteRepository;
    private final DeviceCommandPublisher deviceCommandPublisher;

    public Map<String, Object> sendCommand(DeviceCommandRequest request) {
        if (request.getDeviceId() == null || request.getDeviceId() <= 0) {
            throw new IllegalArgumentException("Invalid device id");
        }
        if (request.getCommand() == null || request.getCommand().isBlank()) {
            throw new IllegalArgumentException("Command is required");
        }
        if (request.getSiteId() == null) {
            throw new IllegalArgumentException("SiteId is required");
        }

        Device device = deviceRepository.findByIdAndSite_SiteId(request.getDeviceId(), request.getSiteId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        final UUID siteId = device.getSite().getSiteId();

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Site not found"));

        request.setUsername(site.getUsername());
        request.setSiteId(siteId);

        String payloadJson = deviceCommandPublisher.buildAndPublish(request);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Command successfully published to device");
        result.put("deviceId", request.getDeviceId());
        result.put("command", request.getCommand());
        result.put("payload", payloadJson);
        return result;
    }
}
