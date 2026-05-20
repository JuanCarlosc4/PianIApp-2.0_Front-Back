package com.piania.core.dto.announcement;

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
public class AnnouncementResponse {

    private Long id;
    private String title;
    private String content;
    private boolean active;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
