package com.piania.core.dto.classinvitation;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassInvitationResponse {

    private String token;
    private String url;
    private LocalDateTime expiresAt;
    private Long classId;
}
