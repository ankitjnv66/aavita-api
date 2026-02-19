package com.aavita.mapper;

import com.aavita.dto.site.SiteDto;
import com.aavita.dto.site.SiteCreateDto;
import com.aavita.dto.site.SiteUpdateDto;
import com.aavita.entity.Site;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SiteMapper {

    @Mapping(target = "siteId", source = "siteId")
    @Mapping(target = "userId", expression = "java(site.getUser().getId())")
    SiteDto toDto(Site site);

    @Mapping(target = "siteId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "devices", ignore = true)
    Site toEntity(SiteCreateDto dto);

    @Mapping(target = "siteId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "devices", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(SiteUpdateDto dto, @MappingTarget Site site);
}
