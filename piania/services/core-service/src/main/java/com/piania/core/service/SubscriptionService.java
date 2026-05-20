package com.piania.core.service;

import com.piania.core.entity.Subscription;
import com.piania.core.enums.SubscriptionType;
import com.piania.core.exception.BadRequestException;
import com.piania.core.repository.PracticeSessionRepository;
import com.piania.core.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.piania.core.entity.SheetMusic;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PracticeSessionRepository practiceSessionRepository;

    public boolean isPremium(String userEmail) {

        return subscriptionRepository
                .findTopByUserEmailOrderByCreatedAtDesc(userEmail)
                .filter(Subscription::isActive)
                .filter(sub -> sub.getPlanType() == SubscriptionType.PREMIUM)
                .filter(sub -> sub.getExpiresAt() == null || sub.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    /**
     * Weekly limit deshabilitado.
     * Actualmente no se aplica ningún límite para el plan FREE.
     */
    public void validateWeeklyPracticeLimitForSheetMusic(String userEmail, SheetMusic sheetMusic) {
        // Límite semanal eliminado.
        // Todos los usuarios pueden crear prácticas sin restricción.
    }
}
