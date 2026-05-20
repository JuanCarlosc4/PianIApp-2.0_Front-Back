package com.piania.core.dto.usersettings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {

    private Long id;
    private String userEmail;
    private String language;
    private boolean notificationsEnabled;
    private boolean metronomeEnabled;
    private int defaultTempo;
    private boolean darkMode;
    private boolean cookiesAccepted;
    private boolean privacyPolicyAccepted;
    private boolean adsEnabled;
    private LocalDateTime createdAt;
}
