package com.piania.core.dto.usersettings;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsRequest {

    @NotBlank
    private String language;

    private boolean notificationsEnabled;
    private boolean metronomeEnabled;
    private int defaultTempo;
    private boolean darkMode;
    private boolean cookiesAccepted;
    private boolean privacyPolicyAccepted;
    private boolean adsEnabled;
}
