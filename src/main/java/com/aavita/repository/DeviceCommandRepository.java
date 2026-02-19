package com.aavita.repository;

import com.aavita.entity.DeviceCommand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {

    List<DeviceCommand> findByDevice_Id(Long deviceId);
}
