package com.aavita.service;

import com.aavita.dto.device.*;
import com.aavita.entity.Device;
import com.aavita.entity.DevicePwmPin;
import com.aavita.mapper.DevicePwmPinMapper;
import com.aavita.repository.DevicePwmPinRepository;
import com.aavita.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DevicePwmPinService {

    private final DevicePwmPinRepository repository;
    private final DeviceRepository deviceRepository;
    private final DevicePwmPinMapper mapper;

    public List<DevicePwmPinDto> getAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    public DevicePwmPinDto getById(Long id) {
        return repository.findById(id).map(mapper::toDto).orElse(null);
    }

    public List<DevicePwmPinDto> getByDeviceId(Long deviceId) {
        return repository.findByDevice_Id(deviceId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DevicePwmPinDto create(DevicePwmPinCreateDto dto) {
        Device device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));
        DevicePwmPin pin = new DevicePwmPin();
        pin.setDevice(device);
        pin.setPinNumber(dto.getPinNumber());
        pin.setValue(dto.getValue());
        pin.setUpdatedOn(java.time.Instant.now());
        pin = repository.save(pin);
        return mapper.toDto(pin);
    }

    @Transactional
    public DevicePwmPinDto update(Long id, DevicePwmPinUpdateDto dto) {
        DevicePwmPin pin = repository.findById(id).orElse(null);
        if (pin == null) return null;
        pin.setValue(dto.getValue());
        pin.setUpdatedOn(java.time.Instant.now());
        pin = repository.save(pin);
        return mapper.toDto(pin);
    }

    @Transactional
    public boolean delete(Long id) {
        if (!repository.existsById(id)) return false;
        repository.deleteById(id);
        return true;
    }
}
