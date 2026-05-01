package com.rehearse.api.domain.question.entity;

import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_set_id", nullable = false)
    private QuestionSet questionSet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType questionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String ttsText;

    @Column(columnDefinition = "TEXT")
    private String modelAnswer;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReferenceType referenceType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FeedbackPerspective feedbackPerspective;

    @Column(nullable = false)
    private int orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_pool_id")
    private QuestionPool questionPool;

    @Column(length = 128)
    private String chainId;

    @Column(length = 16)
    private String chainStepType;

    @Column(length = 64)
    private String projectId;

    @Builder
    public Question(QuestionType questionType, String questionText,
                    String ttsText, String modelAnswer, ReferenceType referenceType,
                    FeedbackPerspective feedbackPerspective,
                    int orderIndex, QuestionPool questionPool) {
        this.questionType = questionType;
        this.questionText = questionText;
        this.ttsText = ttsText;
        this.modelAnswer = modelAnswer;
        this.referenceType = referenceType;
        this.feedbackPerspective = feedbackPerspective;
        this.orderIndex = orderIndex;
        this.questionPool = questionPool;
    }

    public static Question resume(QuestionSet questionSet, QuestionType type,
                                   String questionText, int orderIndex,
                                   String chainId, String chainStepType, String projectId) {
        if (type == QuestionType.MAIN || type == QuestionType.FOLLOWUP) {
            throw new IllegalArgumentException("resume() 팩토리는 RESUME_* 타입만 허용합니다: " + type);
        }
        Question q = new Question();
        q.questionSet = questionSet;
        q.questionType = type;
        q.questionText = questionText;
        q.orderIndex = orderIndex;
        q.chainId = chainId;
        q.chainStepType = chainStepType;
        q.projectId = projectId;
        return q;
    }

    public void assignQuestionSet(QuestionSet questionSet) {
        this.questionSet = questionSet;
    }
}
