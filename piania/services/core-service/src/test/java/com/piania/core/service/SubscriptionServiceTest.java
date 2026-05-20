package com.piania.core.service;

import com.piania.core.entity.SheetMusic;
import com.piania.core.entity.Subscription;
import com.piania.core.enums.SubscriptionType;
import com.piania.core.exception.BadRequestException;
import com.piania.core.repository.PracticeSessionRepository;
import com.piania.core.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PracticeSessionRepository practiceSessionRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnTrueWhenUserIsPremiumAndActive() {
        Subscription subscription = Subscription.builder()
                .userEmail("user@test.com")
                .planType(SubscriptionType.PREMIUM)
                .active(true)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(subscriptionRepository.findTopByUserEmailOrderByCreatedAtDesc("user@test.com"))
                .thenReturn(Optional.of(subscription));

        assertTrue(subscriptionService.isPremium("user@test.com"));
    }

    @Test
    void shouldReturnFalseWhenSubscriptionExpired() {
        Subscription subscription = Subscription.builder()
                .userEmail("user@test.com")
                .planType(SubscriptionType.PREMIUM)
                .active(true)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(subscriptionRepository.findTopByUserEmailOrderByCreatedAtDesc("user@test.com"))
                .thenReturn(Optional.of(subscription));

        assertFalse(subscriptionService.isPremium("user@test.com"));
    }

    @Test
    void shouldThrowWhenFreeUserExceedsWeeklyLimitForSheetMusic() {

        when(subscriptionRepository.findTopByUserEmailOrderByCreatedAtDesc("free@test.com"))
                .thenReturn(Optional.empty());

        // el repositorio cuenta por usuario + partitura + rango semanal
        when(practiceSessionRepository.countByUserEmailAndSheetMusicAndCreatedAtBetween(
                anyString(), any(), any(), any()))
                .thenReturn(5L);

        SheetMusic sheetMusic = SheetMusic.builder()
                .id(1L)
                .ownerEmail("free@test.com")
                .isPublic(false)
                .build();

        assertThrows(BadRequestException.class,
                () -> subscriptionService.validateWeeklyPracticeLimitForSheetMusic("free@test.com", sheetMusic));
    }

    @Test
    void shouldNotThrowWhenPremiumUser() {

        Subscription subscription = Subscription.builder()
                .userEmail("premium@test.com")
                .planType(SubscriptionType.PREMIUM)
                .active(true)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(subscriptionRepository.findTopByUserEmailOrderByCreatedAtDesc("premium@test.com"))
                .thenReturn(Optional.of(subscription));

        SheetMusic sheetMusic = SheetMusic.builder()
                .id(1L)
                .ownerEmail("premium@test.com")
                .isPublic(false)
                .build();

        assertDoesNotThrow(() ->
                subscriptionService.validateWeeklyPracticeLimitForSheetMusic("premium@test.com", sheetMusic));
    }
}
