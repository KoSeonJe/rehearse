package com.rehearse.api.domain.feedback.score.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_score")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuestionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "interview_id", nullable = false)
    private Long interviewId;

    @Column(name = "rubric_id", nullable = false, length = 64)
    private String rubricId;

    @Column(name = "feedback_perspective", length = 32)
    private String feedbackPerspective;

    @Column(name = "level_flag", length = 16)
    private String levelFlag;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public QuestionScore(Long questionId, Long interviewId, String rubricId,
                         String feedbackPerspective, String levelFlag) {
        this.questionId = questionId;
        this.interviewId = interviewId;
        this.rubricId = rubricId;
        this.feedbackPerspective = feedbackPerspective;
        this.levelFlag = levelFlag;
    }
}
