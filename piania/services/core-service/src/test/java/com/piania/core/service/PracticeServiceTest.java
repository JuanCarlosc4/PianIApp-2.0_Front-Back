package com.piania.core.service;

import com.piania.core.dto.practice.PracticeSessionRequest;
import com.piania.core.entity.PracticeSession;
import com.piania.core.entity.SheetMusic;
import com.piania.core.exception.ForbiddenException;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.PracticeSessionRepository;
import com.piania.core.repository.SheetMusicRepository;
import com.piania.core.repository.UserProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PracticeServiceTest {

    @Mock
    private PracticeSessionRepository practiceSessionRepository;

    @Mock
    private SheetMusicRepository sheetMusicRepository;

    @Mock
    private UserProgressRepository userProgressRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private PracticeService practiceService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldThrowWhenSheetMusicNotFound() {

        when(sheetMusicRepository.findById(1L))
                .thenReturn(Optional.empty());

        PracticeSessionRequest request = PracticeSessionRequest.builder()
                .sheetMusicId(1L)
                .audioUrl("url")
                .durationSeconds(60)
                .build();

        assertThrows(NotFoundException.class,
                () -> practiceService.createPractice("user@test.com", request));
    }

    @Test
    void shouldThrowWhenUserNotOwnerAndNotPublic() {

        SheetMusic sheetMusic = SheetMusic.builder()
                .id(1L)
                .ownerEmail("other@test.com")
                .isPublic(false)
                .build();

        when(sheetMusicRepository.findById(1L))
                .thenReturn(Optional.of(sheetMusic));

        PracticeSessionRequest request = PracticeSessionRequest.builder()
                .sheetMusicId(1L)
                .audioUrl("url")
                .durationSeconds(60)
                .build();

        assertThrows(ForbiddenException.class,
                () -> practiceService.createPractice("user@test.com", request));
    }

    @Test
    void shouldCreatePracticeSuccessfully() {

        SheetMusic sheetMusic = SheetMusic.builder()
                .id(1L)
                .ownerEmail("user@test.com")
                .isPublic(false)
                .build();

        when(sheetMusicRepository.findById(1L))
                .thenReturn(Optional.of(sheetMusic));

        when(practiceSessionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(subscriptionService)
                .validateWeeklyPracticeLimitForSheetMusic(eq("user@test.com"), any());

        PracticeSessionRequest request = PracticeSessionRequest.builder()
                .sheetMusicId(1L)
                .audioUrl("url")
                .durationSeconds(60)
                .build();

        PracticeSession session =
                practiceService.createPractice("user@test.com", request);

        assertNotNull(session);
        verify(subscriptionService).validateWeeklyPracticeLimitForSheetMusic(eq("user@test.com"), any());
        verify(practiceSessionRepository).save(any());
    }
}
