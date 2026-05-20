package com.piania.core.repository;

import com.piania.core.entity.PracticeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PracticeFeedbackRepository extends JpaRepository<PracticeFeedback, Long> {

    Optional<PracticeFeedback> findByPracticeSessionId(Long practiceSessionId);
}
