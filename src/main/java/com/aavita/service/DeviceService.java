package com.aavita.service;

import com.aavita.dto.device.*;
import com.aavita.entity.Device;
import com.aavita.entity.Site;
import com.aavita.entity.User;
import com.aavita.mapper.DeviceMapper;
import com.aavita.repository.DeviceRepository;
import com.aavita.repository.SiteRepository;
import com.aavita.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final DeviceMapper deviceMapper;

    public List<DeviceResponse> getAll() {
        return deviceRepository.findAll().stream()
                .map(deviceMapper::toResponse)
                .collect(Collectors.toList());
    }

    public DeviceResponse getById(Long id) {
        return deviceRepository.findFullDeviceById(id)
                .map(deviceMapper::toResponse)
                .orElse(null);
    }

    // Add after getById() method
    @Transactional(readOnly = true)
    public List<DeviceResponse> getBySiteId(UUID siteId) {
        return deviceRepository.findBySiteIdWithDigitalPins(siteId).stream()
                .map(deviceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeviceResponse create(CreateDeviceRequest request) {
        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new IllegalArgumentException("Site not found"));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Device device = deviceMapper.toEntity(request);
        device.setSite(site);
        device.setUser(user);
        device.setCreatedOn(Instant.now());
        device.setUpdatedOn(Instant.now());
        device.setLastSeen(Instant.now());
        device.setMeshId(request.getMeshId() != null && !request.getMeshId().isBlank() ? request.getMeshId() : "");
        device.setDstMac(request.getDstMac() != null && !request.getDstMac().isBlank() ? request.getDstMac() : "");
        device.setGatewayMac(request.getGatewayMac() != null && !request.getGatewayMac().isBlank() ? request.getGatewayMac() : "");
        device.setSubGatewayMac(request.getSubGatewayMac() != null && !request.getSubGatewayMac().isBlank() ? request.getSubGatewayMac() : "");
        device.setLastPktType(0);

        device = deviceRepository.save(device);
        return deviceMapper.toResponse(device);
    }

    @Transactional
    public DeviceResponse update(Long id, UpdateDeviceRequest request) {
        Device device = deviceRepository.findById(id).orElse(null);
        if (device == null) return null;

        deviceMapper.updateFromRequest(request, device);
        device.setUpdatedOn(Instant.now());
        device = deviceRepository.save(device);
        return deviceMapper.toResponse(device);
    }

    @Transactional
    public boolean delete(Long id) {
        if (!deviceRepository.existsById(id)) return false;
        deviceRepository.deleteById(id);
        return true;
    }
}
