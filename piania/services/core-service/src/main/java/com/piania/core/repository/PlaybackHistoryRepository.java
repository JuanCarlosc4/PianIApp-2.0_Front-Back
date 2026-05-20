package com.piania.core.repository;

import com.piania.core.entity.PlaybackHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybackHistoryRepository extends JpaRepository<PlaybackHistory, Long> {

    Page<PlaybackHistory> findByUserEmail(String userEmail, Pageable pageable);
}
