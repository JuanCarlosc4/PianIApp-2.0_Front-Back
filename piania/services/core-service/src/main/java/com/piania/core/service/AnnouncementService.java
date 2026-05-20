package com.piania.core.service;

import com.piania.core.entity.Announcement;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Transactional
    public Announcement create(String title, String message, LocalDateTime expiresAt) {

        LocalDateTime finalExpiresAt = expiresAt != null ? expiresAt : LocalDateTime.now().plusYears(1);

        Announcement announcement = Announcement.builder()
                .title(title)
                .message(message)
                .expiresAt(finalExpiresAt)
                .active(true)
                .build();

        return announcementRepository.save(announcement);
    }

    @Transactional(readOnly = true)
    public Page<Announcement> getActiveAnnouncements(Pageable pageable) {
        return announcementRepository.findByActiveTrueAndExpiresAtAfter(LocalDateTime.now(), pageable);
    }

    @Transactional
    public Announcement deactivate(Long id) {

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Announcement not found"));

        announcement.setActive(false);
        return announcementRepository.save(announcement);
    }
}
