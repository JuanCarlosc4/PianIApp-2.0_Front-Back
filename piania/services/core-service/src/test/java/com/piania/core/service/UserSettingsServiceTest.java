package com.piania.core.service;

import com.piania.core.dto.usersettings.UserSettingsRequest;
import com.piania.core.dto.usersettings.UserSettingsResponse;
import com.piania.core.entity.UserSettings;
import com.piania.core.exception.NotFoundException;
import com.piania.core.mapper.UserSettingsMapper;
import com.piania.core.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserSettingsRepository repository;

    @Mock
    private UserSettingsMapper mapper;

    @InjectMocks
    private UserSettingsService service;

    private static final String EMAIL = "user@test.com";

    private UserSettings buildEntity() {
        return UserSettings.builder()
                .userEmail(EMAIL)
                .language("es")
                .cookiesAccepted(true)
                .privacyPolicyAccepted(true)
                .notificationsEnabled(true)
                .metronomeEnabled(false)
                .defaultTempo(120)
                .adsEnabled(true)
                .darkMode(false)
                .build();
    }

    private UserSettingsRequest buildRequest() {
        UserSettingsRequest req = new UserSettingsRequest();
        req.setLanguage("es");
        req.setCookiesAccepted(true);
        req.setPrivacyPolicyAccepted(true);
        req.setNotificationsEnabled(true);
        req.setMetronomeEnabled(false);
        req.setDefaultTempo(120);
        req.setAdsEnabled(true);
        req.setDarkMode(false);
        return req;
    }

    @Test
    void getByUserEmail_found() {
        UserSettings entity = buildEntity();
        UserSettingsResponse response = UserSettingsResponse.builder()
                .userEmail(EMAIL).language("es").build();

        when(repository.findByUserEmail(EMAIL)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        UserSettingsResponse result = service.getByUserEmail(EMAIL);

        assertThat(result.getUserEmail()).isEqualTo(EMAIL);
        verify(repository).findByUserEmail(EMAIL);
    }

    @Test
    void getByUserEmail_notFound_throws() {
        when(repository.findByUserEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByUserEmail(EMAIL))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void upsert_createsNew_whenNotFound() {
        UserSettingsRequest req = buildRequest();
        UserSettings saved = buildEntity();
        UserSettingsResponse response = UserSettingsResponse.builder()
                .userEmail(EMAIL).language("es").build();

        when(repository.findByUserEmail(EMAIL)).thenReturn(Optional.empty());
        when(repository.save(any(UserSettings.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        UserSettingsResponse result = service.upsert(EMAIL, req);

        assertThat(result.getUserEmail()).isEqualTo(EMAIL);
        verify(repository).save(any(UserSettings.class));
    }

    @Test
    void upsert_updatesExisting_whenFound() {
        UserSettingsRequest req = buildRequest();
        req.setLanguage("en");
        UserSettings existing = buildEntity();
        UserSettings saved = buildEntity();
        saved.setLanguage("en");
        UserSettingsResponse response = UserSettingsResponse.builder()
                .userEmail(EMAIL).language("en").build();

        when(repository.findByUserEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        UserSettingsResponse result = service.upsert(EMAIL, req);

        assertThat(result.getLanguage()).isEqualTo("en");
        verify(repository).save(existing);
    }
}
