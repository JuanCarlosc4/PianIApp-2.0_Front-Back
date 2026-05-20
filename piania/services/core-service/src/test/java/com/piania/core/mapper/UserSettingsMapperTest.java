package com.piania.core.mapper;

import com.piania.core.dto.usersettings.UserSettingsResponse;
import com.piania.core.entity.UserSettings;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class UserSettingsMapperTest {

    private final UserSettingsMapper mapper = new UserSettingsMapper();

    @Test
    void toResponse_mapsAllFields() {
        LocalDateTime createdAt = LocalDateTime.now();

        UserSettings entity = UserSettings.builder()
                .id(1L)
                .userEmail("a@piania.com")
                .language("es")
                .cookiesAccepted(true)
                .adsEnabled(false)
                .darkMode(true)
                .createdAt(createdAt)
                .build();

        UserSettingsResponse response = mapper.toResponse(entity);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserEmail()).isEqualTo("a@piania.com");
        assertThat(response.getLanguage()).isEqualTo("es");
        assertThat(response.isCookiesAccepted()).isTrue();
        assertThat(response.isAdsEnabled()).isFalse();
        assertThat(response.isDarkMode()).isTrue();
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }
}
