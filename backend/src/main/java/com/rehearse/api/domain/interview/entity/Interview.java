package com.rehearse.api.domain.interview.entity;

import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.service.InterviewTrack;
import com.rehearse.api.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import org.hibernate.annotations.BatchSize;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "interview")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Position position;

    @Column(length = 100)
    private String positionDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewLevel level;

    @ElementCollection(targetClass = InterviewType.class)
    @CollectionTable(name = "interview_interview_types", joinColumns = @JoinColumn(name = "interview_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false, length = 30)
    @BatchSize(size = 100)
    private Set<InterviewType> interviewTypes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "interview_cs_sub_topics", joinColumns = @JoinColumn(name = "interview_id"))
    @Column(name = "cs_sub_topic", length = 50)
    @BatchSize(size = 100)
    private Set<String> csSubTopics = new HashSet<>();

    @Column(nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TechStack techStack;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionGenerationStatus questionGenerationStatus;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String overallComment;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void assignPublicId() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID().toString();
        }
    }

    @Builder
    public Interview(Long userId, Position position, String positionDetail, InterviewLevel level,
                     List<InterviewType> interviewTypes, List<String> csSubTopics,
                     Integer durationMinutes, TechStack techStack) {
        this.userId = userId;
        this.position = position;
        this.positionDetail = positionDetail;
        this.level = level;
        this.interviewTypes = interviewTypes != null ? new HashSet<>(interviewTypes) : new HashSet<>();
        this.csSubTopics = csSubTopics != null ? new HashSet<>(csSubTopics) : new HashSet<>();
        this.durationMinutes = durationMinutes;
        this.techStack = techStack;
        this.status = InterviewStatus.READY;
        this.questionGenerationStatus = QuestionGenerationStatus.PENDING;
    }

    public Set<InterviewType> getInterviewTypes() {
        return Collections.unmodifiableSet(interviewTypes);
    }

    public Set<String> getCsSubTopics() {
        return Collections.unmodifiableSet(csSubTopics);
    }

    public TechStack getEffectiveTechStack() {
        return techStack != null ? techStack : TechStack.getDefaultForPosition(this.position);
    }

    public InterviewTrack getTrack() {
        if (interviewTypes.contains(InterviewType.RESUME_BASED)) {
            return InterviewTrack.RESUME;
        }
        return InterviewTrack.CS;
    }

    public void startQuestionGeneration() {
        this.questionGenerationStatus = QuestionGenerationStatus.GENERATING;
        this.failureReason = null;
    }

    public void completeQuestionGeneration() {
        this.questionGenerationStatus = QuestionGenerationStatus.COMPLETED;
    }

    public void failQuestionGeneration(String reason) {
        this.questionGenerationStatus = QuestionGenerationStatus.FAILED;
        this.failureReason = reason;
    }

    public void resetForRetry() {
        this.questionGenerationStatus = QuestionGenerationStatus.PENDING;
        this.failureReason = null;
    }

    public void updateStatus(InterviewStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("상태를 %s에서 %s로 변경할 수 없습니다.", this.status, newStatus));
        }
        this.status = newStatus;
    }

    public void validateOwner(Long userId) {
        if (this.userId != null && !this.userId.equals(userId)) {
            throw new BusinessException(InterviewErrorCode.FORBIDDEN);
        }
    }

    public void completeWithComment(String comment) {
        this.overallComment = comment;
    }
}
