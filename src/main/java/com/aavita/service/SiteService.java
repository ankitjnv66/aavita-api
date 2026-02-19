package com.aavita.service;

import com.aavita.dto.site.SiteCreateDto;
import com.aavita.dto.site.SiteDto;
import com.aavita.dto.site.SiteUpdateDto;
import com.aavita.entity.Site;
import com.aavita.entity.User;
import com.aavita.mapper.SiteMapper;
import com.aavita.repository.SiteRepository;
import com.aavita.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final SiteMapper siteMapper;

    public List<SiteDto> getAll() {
        return siteRepository.findAll().stream()
                .map(siteMapper::toDto)
                .collect(Collectors.toList());
    }

    public SiteDto getById(UUID id) {
        return siteRepository.findById(id)
                .map(siteMapper::toDto)
                .orElse(null);
    }

    public List<SiteDto> getByUserId(Long userId) {
        return siteRepository.findByUser_Id(userId).stream()
                .map(siteMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SiteDto create(SiteCreateDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Site site = siteMapper.toEntity(dto);
        site.setUser(user);
        site.setCreatedOn(java.time.Instant.now());
        site = siteRepository.save(site);

        return siteMapper.toDto(site);
    }

    @Transactional
    public SiteDto update(UUID id, SiteUpdateDto dto) {
        Site site = siteRepository.findById(id)
                .orElse(null);
        if (site == null) return null;

        siteMapper.updateFromDto(dto, site);
        site = siteRepository.save(site);
        return siteMapper.toDto(site);
    }

    @Transactional
    public boolean delete(UUID id) {
        if (!siteRepository.existsById(id)) return false;
        siteRepository.deleteById(id);
        return true;
    }
}
