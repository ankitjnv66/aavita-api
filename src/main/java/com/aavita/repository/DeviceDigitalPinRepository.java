package com.aavita.repository;

import com.aavita.entity.DeviceDigitalPin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceDigitalPinRepository extends JpaRepository<DeviceDigitalPin, Long> {

    List<DeviceDigitalPin> findByDevice_Id(Long deviceId);

    Optional<DeviceDigitalPin> findByDevice_IdAndPinNumber(Long deviceId, Byte pinNumber);
}
