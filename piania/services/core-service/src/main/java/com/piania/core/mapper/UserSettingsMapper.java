package com.piania.core.mapper;

import com.piania.core.dto.usersettings.UserSettingsResponse;
import com.piania.core.entity.UserSettings;
import org.springframework.stereotype.Component;

@Component
public class UserSettingsMapper {

    public UserSettingsResponse toResponse(UserSettings entity) {
        return UserSettingsResponse.builder()
                .id(entity.getId())
                .userEmail(entity.getUserEmail())
                .language(entity.getLanguage())
                .notificationsEnabled(entity.isNotificationsEnabled())
                .metronomeEnabled(entity.isMetronomeEnabled())
                .defaultTempo(entity.getDefaultTempo())
                .darkMode(entity.isDarkMode())
                .cookiesAccepted(entity.isCookiesAccepted())
                .privacyPolicyAccepted(entity.isPrivacyPolicyAccepted())
                .adsEnabled(entity.isAdsEnabled())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
