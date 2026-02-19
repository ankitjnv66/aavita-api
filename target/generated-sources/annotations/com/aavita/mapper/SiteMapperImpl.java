package com.aavita.mapper;

import com.aavita.dto.site.SiteCreateDto;
import com.aavita.dto.site.SiteDto;
import com.aavita.dto.site.SiteUpdateDto;
import com.aavita.entity.Site;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-19T23:47:35+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.5 (Amazon.com Inc.)"
)
@Component
public class SiteMapperImpl implements SiteMapper {

    @Override
    public SiteDto toDto(Site site) {
        if ( site == null ) {
            return null;
        }

        SiteDto siteDto = new SiteDto();

        siteDto.setSiteId( site.getSiteId() );
        siteDto.setUsername( site.getUsername() );
        siteDto.setLocation( site.getLocation() );
        siteDto.setCreatedOn( site.getCreatedOn() );

        siteDto.setUserId( site.getUser().getId() );

        return siteDto;
    }

    @Override
    public Site toEntity(SiteCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        Site.SiteBuilder site = Site.builder();

        site.username( dto.getUsername() );
        site.location( dto.getLocation() );

        return site.build();
    }

    @Override
    public void updateFromDto(SiteUpdateDto dto, Site site) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getUsername() != null ) {
            site.setUsername( dto.getUsername() );
        }
        if ( dto.getLocation() != null ) {
            site.setLocation( dto.getLocation() );
        }
    }
}
