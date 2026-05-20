package com.piania.core.repository;

import com.piania.core.entity.UserProgress;
import com.piania.core.entity.SheetMusic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    Optional<UserProgress> findByUserEmailAndSheetMusic(String userEmail, SheetMusic sheetMusic);
}
