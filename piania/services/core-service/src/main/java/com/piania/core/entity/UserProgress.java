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
@Table(name = "user_progress")
public class UserProgress extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String userEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_music_id", nullable = false)
    private SheetMusic sheetMusic;

    @Column(nullable = false)
    private Integer totalPractices;

    @Column(nullable = false)
    private Integer averageScore;

    @Column(nullable = false)
    private Integer totalPracticeTimeSeconds;
}
