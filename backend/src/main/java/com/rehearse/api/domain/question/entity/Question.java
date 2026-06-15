package com.rehearse.api.domain.question.entity;

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
    private String bestAnswer;

    @Column(nullable = false)
    private int orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_pool_id")
    private QuestionPool questionPool;

    @Enumerated(EnumType.STRING)
    @Column(name = "depth_type", length = 20)
    private QuestionDepthType depthType;

    @Builder
    public Question(QuestionType questionType, String questionText,
                    String ttsText, String bestAnswer,
                    int orderIndex, QuestionPool questionPool) {
        requireValidQuestionText(questionText);
        requireNonNullQuestionType(questionType);
        this.questionType = questionType;
        this.questionText = questionText;
        this.ttsText = ttsText;
        this.bestAnswer = bestAnswer;
        this.orderIndex = orderIndex;
        this.questionPool = questionPool;
    }

    public static Question resume(QuestionSet questionSet, QuestionType type,
                                   String questionText, String ttsText, String bestAnswer,
                                   int orderIndex, QuestionDepthType depthType) {
        requireValidQuestionText(questionText);
        requireNonNullQuestionType(type);
        if (!type.isResume()) {
            throw new IllegalArgumentException("resume() 팩토리는 RESUME_* 타입만 허용합니다: " + type);
        }
        if (type == QuestionType.RESUME_MAIN && depthType == null) {
            throw new IllegalArgumentException("RESUME_MAIN 질문은 depthType 이 필수입니다.");
        }
        Question q = new Question();
        q.questionSet = questionSet;
        q.questionType = type;
        q.questionText = questionText;
        q.ttsText = ttsText;
        q.bestAnswer = bestAnswer;
        q.orderIndex = orderIndex;
        q.depthType = depthType;
        return q;
    }

    public void assignQuestionSet(QuestionSet questionSet) {
        this.questionSet = questionSet;
    }

    private static void requireValidQuestionText(String questionText) {
        if (questionText == null || questionText.isBlank()) {
            throw new IllegalArgumentException("questionText must not be blank");
        }
    }

    private static void requireNonNullQuestionType(QuestionType questionType) {
        if (questionType == null) {
            throw new IllegalArgumentException("questionType must not be null");
        }
    }
}
