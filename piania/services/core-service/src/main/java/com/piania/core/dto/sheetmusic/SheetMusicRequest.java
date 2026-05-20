package com.piania.core.dto.sheetmusic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SheetMusicRequest {

    @NotBlank
    private String title;

    private String composer;

    @NotBlank
    private String originalFileUrl;

    @NotNull
    private Boolean isPublic;
}
