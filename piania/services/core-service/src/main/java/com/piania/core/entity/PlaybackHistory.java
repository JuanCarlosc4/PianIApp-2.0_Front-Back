package com.piania.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "playback_history")
public class PlaybackHistory extends BaseEntity {

    @Column(nullable = false)
    private String userEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_music_id", nullable = false)
    private SheetMusic sheetMusic;

    @Column(nullable = false)
    private Integer tempoUsed;

    @Column(nullable = false)
    private boolean metronomeEnabled;
}
