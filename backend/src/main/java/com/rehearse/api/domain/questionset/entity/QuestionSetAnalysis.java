package com.rehearse.api.domain.questionset.entity;

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
@Table(name = "question_set_analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuestionSetAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_set_id", nullable = false, unique = true)
    private QuestionSet questionSet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConvertStatus convertStatus = ConvertStatus.PENDING;

    @Column(nullable = false)
    private boolean isVerbalCompleted = false;

    @Column(nullable = false)
    private boolean isNonverbalCompleted = false;

    @Column(length = 500)
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String failureDetail;

    @Column(length = 500)
    private String convertFailureReason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @Builder
    public QuestionSetAnalysis(QuestionSet questionSet) {
        this.questionSet = questionSet;
    }

    public void updateAnalysisStatus(AnalysisStatus newStatus) {
        if (!this.analysisStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("분석 상태를 %s에서 %s로 변경할 수 없습니다.", this.analysisStatus, newStatus));
        }
        this.analysisStatus = newStatus;
    }

    public void updateConvertStatus(ConvertStatus newStatus) {
        if (!this.convertStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("변환 상태를 %s에서 %s로 변경할 수 없습니다.", this.convertStatus, newStatus));
        }
        this.convertStatus = newStatus;
    }

    public void completeAnalysis(boolean verbalCompleted, boolean nonverbalCompleted) {
        this.isVerbalCompleted = verbalCompleted;
        this.isNonverbalCompleted = nonverbalCompleted;

        if (verbalCompleted && nonverbalCompleted) {
            updateAnalysisStatus(AnalysisStatus.COMPLETED);
        } else if (!verbalCompleted && !nonverbalCompleted) {
            updateAnalysisStatus(AnalysisStatus.FAILED);
        } else {
            updateAnalysisStatus(AnalysisStatus.PARTIAL);
        }
    }

    public void markFailed(String reason, String detail) {
        updateAnalysisStatus(AnalysisStatus.FAILED);
        this.failureReason = reason;
        this.failureDetail = detail;
    }

    public void setConvertFailureReason(String reason) {
        this.convertFailureReason = reason;
    }

    public void resetVerbalResult() {
        this.isVerbalCompleted = false;
    }

    public void resetNonverbalResult() {
        this.isNonverbalCompleted = false;
    }

    public boolean isFullyReady() {
        boolean analysisOk = analysisStatus == AnalysisStatus.COMPLETED
                          || analysisStatus == AnalysisStatus.PARTIAL;
        boolean convertOk = convertStatus == ConvertStatus.COMPLETED;
        return analysisOk && convertOk;
    }
}
