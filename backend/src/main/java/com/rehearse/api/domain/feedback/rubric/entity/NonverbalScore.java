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

    @Column(name = "d11_fluency")
    private Integer d11Fluency;

    @Column(name = "d12_tone")
    private Integer d12Tone;

    @Column(name = "d13_posture")
    private Integer d13Posture;

    @Column(name = "d14_composure")
    private Integer d14Composure;

    @Column(name = "raw_signals", nullable = false, columnDefinition = "JSON")
    private String rawSignals;

    @Column(name = "context_multiplier", precision = 3, scale = 2)
    private BigDecimal contextMultiplier;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public NonverbalScore(Long interviewId, Long turnId, Integer d11Fluency, Integer d12Tone,
                          Integer d13Posture, Integer d14Composure, String rawSignals,
                          BigDecimal contextMultiplier) {
        this.interviewId = interviewId;
        this.turnId = turnId;
        this.d11Fluency = d11Fluency;
        this.d12Tone = d12Tone;
        this.d13Posture = d13Posture;
        this.d14Composure = d14Composure;
        this.rawSignals = rawSignals;
        this.contextMultiplier = contextMultiplier;
    }

    public static NonverbalScore from(Long interviewId, Long turnId, NonverbalTurnScore score,
                                      String rawSignalsJson) {
        return NonverbalScore.builder()
                .interviewId(interviewId)
                .turnId(turnId)
                .d11Fluency(score.d11Fluency())
                .d12Tone(score.d12Tone())
                .d13Posture(score.d13Posture())
                .d14Composure(score.d14Composure())
                .rawSignals(rawSignalsJson)
                .contextMultiplier(BigDecimal.valueOf(score.contextMultiplier()))
                .build();
    }
}
