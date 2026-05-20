package com.rehearse.api.domain.feedback.score.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question_score_dimension")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionScoreDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_score_id", nullable = false)
    private Long questionScoreId;

    @Column(name = "dimension_ref", nullable = false, length = 64)
    private String dimensionRef;

    @Column
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String observation;

    @Column(name = "evidence_quote", columnDefinition = "TEXT")
    private String evidenceQuote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DimensionStatus status;

    @Builder
    public QuestionScoreDimension(Long questionScoreId, String dimensionRef,
                                  Integer score, String observation, String evidenceQuote,
                                  DimensionStatus status) {
        this.questionScoreId = questionScoreId;
        this.dimensionRef = dimensionRef;
        this.score = score;
        this.observation = observation;
        this.evidenceQuote = evidenceQuote;
        this.status = status != null ? status : DimensionStatus.OK;
    }
}
