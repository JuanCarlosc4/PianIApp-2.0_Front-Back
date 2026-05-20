package com.piania.core.repository;

import com.piania.core.entity.SheetMusic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SheetMusicRepository extends JpaRepository<SheetMusic, Long> {

    Page<SheetMusic> findByOwnerEmailAndDeletedFalse(String ownerEmail, Pageable pageable);

    Page<SheetMusic> findByIsPublicTrueAndDeletedFalse(Pageable pageable);
}
