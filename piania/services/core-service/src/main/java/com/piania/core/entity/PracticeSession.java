package com.piania.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "practice_sessions")
public class PracticeSession extends BaseEntity {

    @Column(nullable = false)
    private String userEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_music_id", nullable = false)
    private SheetMusic sheetMusic;

    @Column(nullable = false)
    private String audioUrl;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column(nullable = false)
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String studentObservations;

    @Column(columnDefinition = "TEXT")
    private String teacherCorrections;

    @Column(nullable = false)
    private Integer listenCount;

    @Column(nullable = false)
    private boolean deleted;
}
