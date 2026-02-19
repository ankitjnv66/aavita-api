package com.aavita.service;

import com.aavita.entity.Device;
import com.aavita.entity.DeviceDigitalPin;
import com.aavita.entity.DevicePwmPin;
import com.aavita.entity.DeviceStatusHistory;
import com.aavita.mqtt.model.DevicePayload;
import com.aavita.repository.DeviceDigitalPinRepository;
import com.aavita.repository.DevicePwmPinRepository;
import com.aavita.repository.DeviceRepository;
import com.aavita.repository.DeviceStatusHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DevicePinService {

    private final DeviceDigitalPinRepository digitalPinRepository;
    private final DevicePwmPinRepository pwmPinRepository;
    private final DeviceStatusHistoryRepository statusHistoryRepository;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void updateDigitalPins(Long deviceId, byte[] digitalValues) {
        for (int i = 0; i < 18; i++) {
            byte pinNumber = (byte) (i + 1);
            byte value = i < digitalValues.length ? digitalValues[i] : 0;

            digitalPinRepository.findByDevice_IdAndPinNumber((long) deviceId, pinNumber)
                    .ifPresentOrElse(
                            pin -> {
                                pin.setState(value);
                                pin.setUpdatedOn(Instant.now());
                                digitalPinRepository.save(pin);
                            },
                            () -> {
                                Device device = deviceRepository.findById((long) deviceId)
                                        .orElseThrow(() -> new IllegalArgumentException("Device not found"));
                                DeviceDigitalPin pin = new DeviceDigitalPin();
                                pin.setDevice(device);
                                pin.setPinNumber(pinNumber);
                                pin.setState(value);
                                pin.setUpdatedOn(Instant.now());
                                digitalPinRepository.save(pin);
                            });
        }
    }

    @Transactional
    public void updatePwmPins(Long deviceId, byte[] pwmValues) {
        for (int i = 0; i < 4; i++) {
            byte pinNumber = (byte) (i + 1);
            byte value = i < pwmValues.length ? pwmValues[i] : 0;

            pwmPinRepository.findByDevice_IdAndPinNumber((long) deviceId, pinNumber)
                    .ifPresentOrElse(
                            pin -> {
                                pin.setValue(value);
                                pin.setUpdatedOn(Instant.now());
                                pwmPinRepository.save(pin);
                            },
                            () -> {
                                Device device = deviceRepository.findById((long) deviceId)
                                        .orElseThrow(() -> new IllegalArgumentException("Device not found"));
                                DevicePwmPin pin = new DevicePwmPin();
                                pin.setDevice(device);
                                pin.setPinNumber(pinNumber);
                                pin.setValue(value);
                                pin.setUpdatedOn(Instant.now());
                                pwmPinRepository.save(pin);
                            });
        }
    }

    @Transactional
    public void saveStatusHistory(Long deviceId, String encodedPayload, DevicePayload payload) {
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            jsonPayload = "{}";
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        DeviceStatusHistory history = new DeviceStatusHistory();
        history.setDevice(device);
        history.setPktType(payload.getRoutingData().getPktType());
        history.setActionCause((byte) payload.getPayloadData().getActionCause().getValue());
        history.setSerializedPayload(encodedPayload);
        history.setJsonPayload(jsonPayload);
        history.setReceivedOn(Instant.now());

        statusHistoryRepository.save(history);
    }
}
