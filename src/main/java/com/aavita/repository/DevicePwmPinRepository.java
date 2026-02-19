package com.aavita.repository;

import com.aavita.entity.DevicePwmPin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DevicePwmPinRepository extends JpaRepository<DevicePwmPin, Long> {

    List<DevicePwmPin> findByDevice_Id(Long deviceId);

    Optional<DevicePwmPin> findByDevice_IdAndPinNumber(Long deviceId, Byte pinNumber);
}
