package com.piania.core.dto.sharelink;

import com.piania.core.enums.ShareAccessType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShareLinkCreateRequest {

    @NotNull
    private Long sheetMusicId;

    @NotNull
    private ShareAccessType accessType;

    private LocalDateTime expiresAt;
}
