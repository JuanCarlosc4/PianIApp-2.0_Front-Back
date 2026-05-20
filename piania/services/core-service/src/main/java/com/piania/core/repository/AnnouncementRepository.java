package com.piania.core.repository;

import com.piania.core.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Page<Announcement> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Announcement> findByActiveTrueAndExpiresAtAfter(LocalDateTime now, Pageable pageable);

}
