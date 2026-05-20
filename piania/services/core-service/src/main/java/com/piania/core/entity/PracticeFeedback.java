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
@Table(name = "practice_feedback")
public class PracticeFeedback {

    @Id
    private Long practiceSessionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "practice_session_id")
    private PracticeSession practiceSession;

    @Column(nullable = false)
    private Integer precisionGeneral;

    @Column(nullable = false)
    private Integer noteErrors;

    @Column(nullable = false)
    private Integer rhythmErrors;

    @Column(columnDefinition = "TEXT")
    private String detailedReport;
}
