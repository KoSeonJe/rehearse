package com.rehearse.api.domain.feedback.rubric.entity;

import com.rehearse.api.domain.feedback.rubric.service.NonverbalTurnScore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "nonverbal_score")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NonverbalScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "interview_id", nullable = false)
    private Long interviewId;

    @Column(name = "turn_id", nullable = false)
    private Long turnId;

    @Column(name = "fluency_score")
    private Integer fluencyScore;

    @Column(name = "tone_score")
    private Integer toneScore;

    @Column(name = "posture_score")
    private Integer postureScore;

    @Column(name = "composure_score")
    private Integer composureScore;

    @Column(name = "raw_signals", nullable = false, columnDefinition = "JSON")
    private String rawSignals;

    @Column(name = "context_multiplier", precision = 3, scale = 2)
    private BigDecimal contextMultiplier;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public NonverbalScore(Long interviewId, Long turnId, Integer fluencyScore, Integer toneScore,
                          Integer postureScore, Integer composureScore, String rawSignals,
                          BigDecimal contextMultiplier) {
        this.interviewId = interviewId;
        this.turnId = turnId;
        this.fluencyScore = fluencyScore;
        this.toneScore = toneScore;
        this.postureScore = postureScore;
        this.composureScore = composureScore;
        this.rawSignals = rawSignals;
        this.contextMultiplier = contextMultiplier;
    }

    public static NonverbalScore from(Long interviewId, Long turnId, NonverbalTurnScore score,
                                      String rawSignalsJson) {
        return NonverbalScore.builder()
                .interviewId(interviewId)
                .turnId(turnId)
                .fluencyScore(score.d11Fluency())
                .toneScore(score.d12Tone())
                .postureScore(score.d13Posture())
                .composureScore(score.d14Composure())
                .rawSignals(rawSignalsJson)
                .contextMultiplier(BigDecimal.valueOf(score.contextMultiplier()))
                .build();
    }
}
