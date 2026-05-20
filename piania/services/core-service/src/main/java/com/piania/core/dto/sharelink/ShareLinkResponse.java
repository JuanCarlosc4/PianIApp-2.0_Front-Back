package com.piania.core.dto.sharelink;

import com.piania.core.enums.ShareAccessType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShareLinkResponse {

    private Long id;
    private String token;
    private Long sheetMusicId;
    private ShareAccessType accessType;
    private LocalDateTime expiresAt;
    private boolean active;
    private LocalDateTime createdAt;
}
