package com.piania.core.service;

import com.piania.core.entity.Announcement;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.AnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository repository;

    @InjectMocks
    private AnnouncementService service;

    @Test
    void create_buildsActiveAnnouncementAndSaves() {
        when(repository.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);

        Announcement created = service.create("t", "m", expiresAt);

        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(repository).save(captor.capture());

        Announcement saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("t");
        assertThat(saved.getMessage()).isEqualTo("m");
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(saved.isActive()).isTrue();

        assertThat(created).isSameAs(saved);
    }

    @Test
    void getActiveAnnouncements_delegatesToRepository() {
        Page<Announcement> page = new PageImpl<>(List.of(Announcement.builder().id(1L).build()));
        when(repository.findByActiveTrueAndExpiresAtAfter(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);

        Page<Announcement> result = service.getActiveAnnouncements(Pageable.ofSize(10));

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findByActiveTrueAndExpiresAtAfter(any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void deactivate_whenNotFound_throwsNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(9L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Announcement not found");

        verify(repository, never()).save(any());
    }

    @Test
    void deactivate_whenFound_setsInactiveAndSaves() {
        Announcement a = Announcement.builder().id(9L).active(true).build();
        when(repository.findById(9L)).thenReturn(Optional.of(a));
        when(repository.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));

        Announcement result = service.deactivate(9L);

        assertThat(a.isActive()).isFalse();
        verify(repository).save(a);
        assertThat(result).isSameAs(a);
    }
}
