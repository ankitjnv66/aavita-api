package com.aavita.service;

import com.aavita.dto.device.*;
import com.aavita.entity.Device;
import com.aavita.entity.DeviceDigitalPin;
import com.aavita.mapper.DeviceDigitalPinMapper;
import com.aavita.repository.DeviceDigitalPinRepository;
import com.aavita.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceDigitalPinService {

    private final DeviceDigitalPinRepository repository;
    private final DeviceRepository deviceRepository;
    private final DeviceDigitalPinMapper mapper;

    public List<DeviceDigitalPinDto> getAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    public DeviceDigitalPinDto getById(Long id) {
        return repository.findById(id).map(mapper::toDto).orElse(null);
    }

    public List<DeviceDigitalPinDto> getByDeviceId(Long deviceId) {
        return repository.findByDevice_Id(deviceId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeviceDigitalPinDto create(DeviceDigitalPinCreateDto dto) {
        Device device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));
        DeviceDigitalPin pin = new DeviceDigitalPin();
        pin.setDevice(device);
        pin.setPinNumber(dto.getPinNumber());
        pin.setState(dto.getState());
        pin.setUpdatedOn(java.time.Instant.now());
        pin = repository.save(pin);
        return mapper.toDto(pin);
    }

    @Transactional
    public DeviceDigitalPinDto update(Long id, DeviceDigitalPinUpdateDto dto) {
        DeviceDigitalPin pin = repository.findById(id).orElse(null);
        if (pin == null) return null;
        pin.setState(dto.getState());
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
