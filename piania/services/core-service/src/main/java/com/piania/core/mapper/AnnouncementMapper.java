package com.piania.core.mapper;

import com.piania.core.dto.announcement.AnnouncementResponse;
import com.piania.core.entity.Announcement;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementMapper {

    public AnnouncementResponse toResponse(Announcement entity) {
        return AnnouncementResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getMessage())
                .active(entity.isActive())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
