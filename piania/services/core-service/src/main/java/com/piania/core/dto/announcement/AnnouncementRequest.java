package com.piania.core.dto.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AnnouncementRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String message;

    private LocalDateTime expiresAt;

    private Boolean active;
}
