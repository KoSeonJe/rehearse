package com.rehearse.api.domain.feedback.session.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SessionFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "interview_id", nullable = false, unique = true)
    private Long interviewId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SessionFeedbackStatus status;

    @Column(name = "overall_json", columnDefinition = "JSON")
    private String overallJson;

    @Column(name = "strengths_json", columnDefinition = "JSON")
    private String strengthsJson;

    @Column(name = "gaps_json", columnDefinition = "JSON")
    private String gapsJson;

    @Column(name = "delivery_json", columnDefinition = "JSON")
    private String deliveryJson;

    @Column(name = "week_plan_json", columnDefinition = "JSON")
    private String weekPlanJson;

    @Column(name = "coverage", length = 64)
    private String coverage;

    @Column(name = "delivery_retryable", nullable = false)
    private boolean deliveryRetryable = true;

    @Column(name = "last_failure_reason", length = 64)
    private String lastFailureReason;

    @Column(name = "retry_attempts", nullable = false)
    private int retryAttempts = 0;

    @Column(name = "retry_started_at")
    private LocalDateTime retryStartedAt;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public SessionFeedback(Long interviewId, String overallJson, String strengthsJson,
                           String gapsJson, String deliveryJson, String weekPlanJson,
                           String coverage) {
        this.interviewId = interviewId;
        this.status = SessionFeedbackStatus.PRELIMINARY;
        this.overallJson = overallJson;
        this.strengthsJson = strengthsJson;
        this.gapsJson = gapsJson;
        this.deliveryJson = deliveryJson;
        this.weekPlanJson = weekPlanJson;
        this.coverage = coverage;
        this.deliveryRetryable = true;
        this.retryAttempts = 0;
    }

    public void markComplete() {
        this.status = SessionFeedbackStatus.COMPLETE;
    }

    public void markCompleteWithFailure(String reason) {
        this.status = SessionFeedbackStatus.COMPLETE;
        this.lastFailureReason = reason;
        this.deliveryRetryable = true;
    }

    public void applyDeliveryEnrichment(String deliveryJson) {
        this.deliveryJson = deliveryJson;
        this.status = SessionFeedbackStatus.COMPLETE;
    }

    public void updateCoverage(String coverage) {
        this.coverage = coverage;
    }

    public void recordFailure(String reason, boolean retryable) {
        this.lastFailureReason = reason;
        this.deliveryRetryable = retryable;
    }

    public void incrementRetry() {
        this.retryAttempts++;
        this.retryStartedAt = LocalDateTime.now();
    }

    public boolean isRetryCoolingDown() {
        if (retryStartedAt == null) return false;
        return retryStartedAt.isAfter(LocalDateTime.now().minusSeconds(60));
    }
}
