package com.aavita.repository;

import com.aavita.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.digitalPins LEFT JOIN FETCH d.pwmPins LEFT JOIN FETCH d.statusHistory WHERE d.id = :id")
    Optional<Device> findFullDeviceById(@Param("id") Long id);

    Optional<Device> findByIdAndSite_SiteId(Long deviceId, UUID siteId);

    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.digitalPins LEFT JOIN FETCH d.pwmPins WHERE d.srcMac = :srcMac AND d.site.siteId = :siteId")
    Optional<Device> findBySrcMacAndSiteId(@Param("srcMac") String srcMac, @Param("siteId") UUID siteId);

    List<Device> findBySite_SiteId(UUID siteId);
}
