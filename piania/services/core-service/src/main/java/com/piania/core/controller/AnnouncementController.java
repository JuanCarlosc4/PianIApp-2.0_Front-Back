package com.piania.core.controller;

import com.piania.core.dto.announcement.AnnouncementRequest;
import com.piania.core.dto.announcement.AnnouncementResponse;
import com.piania.core.entity.Announcement;
import com.piania.core.mapper.AnnouncementMapper;
import com.piania.core.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/piania/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final AnnouncementMapper mapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AnnouncementResponse create(@Valid @RequestBody AnnouncementRequest request) {

        Announcement announcement = announcementService.create(
                request.getTitle(),
                request.getMessage(),
                request.getExpiresAt()
        );

        return mapper.toResponse(announcement);
    }

    @GetMapping
    public Page<AnnouncementResponse> getActive(Pageable pageable) {

        return announcementService.getActiveAnnouncements(pageable)
                .map(mapper::toResponse);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AnnouncementResponse deactivate(@PathVariable Long id) {

        Announcement announcement = announcementService.deactivate(id);
        return mapper.toResponse(announcement);
    }
}
