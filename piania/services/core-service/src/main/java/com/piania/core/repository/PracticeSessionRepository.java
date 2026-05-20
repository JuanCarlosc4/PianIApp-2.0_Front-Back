package com.piania.core.repository;

import com.piania.core.entity.PracticeSession;
import com.piania.core.entity.SheetMusic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {

    long countByUserEmailAndCreatedAtBetween(String userEmail,
                                             java.time.LocalDateTime start,
                                             java.time.LocalDateTime end);

    Page<PracticeSession> findByUserEmailAndDeletedFalse(String userEmail, Pageable pageable);

    long countByUserEmailAndCreatedAtAfter(String userEmail, LocalDateTime date);

    long countByUserEmailAndSheetMusicAndCreatedAtBetween(
            String userEmail,
            SheetMusic sheetMusic,
            LocalDateTime start,
            LocalDateTime end
    );

    Page<PracticeSession> findBySheetMusicAndDeletedFalse(SheetMusic sheetMusic, Pageable pageable);
}
