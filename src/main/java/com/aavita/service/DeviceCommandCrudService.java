package com.aavita.service;

import com.aavita.dto.device.*;
import com.aavita.entity.Device;
import com.aavita.entity.DeviceCommand;
import com.aavita.mapper.DeviceCommandMapper;
import com.aavita.repository.DeviceCommandRepository;
import com.aavita.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceCommandCrudService {

    private final DeviceCommandRepository repository;
    private final DeviceRepository deviceRepository;
    private final DeviceCommandMapper mapper;

    public List<DeviceCommandDto> getAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    public DeviceCommandDto getById(Long id) {
        return repository.findById(id).map(mapper::toDto).orElse(null);
    }

    @Transactional
    public DeviceCommandDto create(DeviceCommandCreateDto dto) {
        Device device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));
        DeviceCommand cmd = DeviceCommand.builder()
                .device(device)
                .pktType(dto.getPktType())
                .actionCause(dto.getActionCause())
                .serializedPayload(dto.getSerializedPayload())
                .jsonPayload(dto.getJsonPayload())
                .status(dto.getStatus())
                .createdOn(Instant.now())
                .build();
        cmd = repository.save(cmd);
        return mapper.toDto(cmd);
    }

    @Transactional
    public DeviceCommandDto update(Long id, DeviceCommandUpdateDto dto) {
        DeviceCommand cmd = repository.findById(id).orElse(null);
        if (cmd == null) return null;
        if (dto.getPktType() != null) cmd.setPktType(dto.getPktType());
        if (dto.getActionCause() != null) cmd.setActionCause(dto.getActionCause());
        if (dto.getSerializedPayload() != null) cmd.setSerializedPayload(dto.getSerializedPayload());
        if (dto.getJsonPayload() != null) cmd.setJsonPayload(dto.getJsonPayload());
        if (dto.getStatus() != null) cmd.setStatus(dto.getStatus());
        if (dto.getExecutedOn() != null) cmd.setExecutedOn(dto.getExecutedOn());
        cmd = repository.save(cmd);
        return mapper.toDto(cmd);
    }

    @Transactional
    public boolean delete(Long id) {
        if (!repository.existsById(id)) return false;
        repository.deleteById(id);
        return true;
    }
}
