package com.aavita.repository;

import com.aavita.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiteRepository extends JpaRepository<Site, UUID> {

    List<Site> findByUser_Id(Long userId);

    Optional<Site> findByUsername(String username);
}
