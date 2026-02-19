package com.aavita.repository;

import com.aavita.entity.DeviceStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceStatusHistoryRepository extends JpaRepository<DeviceStatusHistory, Long> {

    List<DeviceStatusHistory> findByDevice_IdOrderByReceivedOnDesc(Long deviceId, org.springframework.data.domain.Pageable pageable);
}
