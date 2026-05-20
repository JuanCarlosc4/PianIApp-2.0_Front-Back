package com.piania.core.mapper;

import com.piania.core.dto.sharelink.ShareLinkResponse;
import com.piania.core.entity.ShareLink;
import org.springframework.stereotype.Component;

@Component
public class ShareLinkMapper {

    public ShareLinkResponse toResponse(ShareLink entity) {
        return new ShareLinkResponse(
                entity.getId(),
                entity.getToken(),
                entity.getSheetMusic().getId(),
                entity.getAccessType(),
                entity.getExpiresAt(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
