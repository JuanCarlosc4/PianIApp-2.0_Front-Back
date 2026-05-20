package com.piania.core.service;

import com.piania.core.dto.usersettings.UserSettingsRequest;
import com.piania.core.dto.usersettings.UserSettingsResponse;
import com.piania.core.entity.UserSettings;
import com.piania.core.exception.NotFoundException;
import com.piania.core.mapper.UserSettingsMapper;
import com.piania.core.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository repository;
    private final UserSettingsMapper mapper;

    @Transactional(readOnly = true)
    public UserSettingsResponse getByUserEmail(String email) {
        UserSettings settings = repository.findByUserEmail(email)
                .orElseThrow(() -> new NotFoundException("User settings not found for: " + email));
        return mapper.toResponse(settings);
    }

    @Transactional
    public UserSettingsResponse upsert(String email, UserSettingsRequest request) {
        UserSettings settings = repository.findByUserEmail(email)
                .orElseGet(() -> UserSettings.builder()
                        .userEmail(email)
                        .language(request.getLanguage())
                        .cookiesAccepted(request.isCookiesAccepted())
                        .privacyPolicyAccepted(request.isPrivacyPolicyAccepted())
                        .notificationsEnabled(request.isNotificationsEnabled())
                        .metronomeEnabled(request.isMetronomeEnabled())
                        .defaultTempo(request.getDefaultTempo())
                        .adsEnabled(request.isAdsEnabled())
                        .darkMode(request.isDarkMode())
                        .build());

        settings.setLanguage(request.getLanguage());
        settings.setCookiesAccepted(request.isCookiesAccepted());
        settings.setPrivacyPolicyAccepted(request.isPrivacyPolicyAccepted());
        settings.setNotificationsEnabled(request.isNotificationsEnabled());
        settings.setMetronomeEnabled(request.isMetronomeEnabled());
        settings.setDefaultTempo(request.getDefaultTempo());
        settings.setAdsEnabled(request.isAdsEnabled());
        settings.setDarkMode(request.isDarkMode());

        return mapper.toResponse(repository.save(settings));
    }
}
