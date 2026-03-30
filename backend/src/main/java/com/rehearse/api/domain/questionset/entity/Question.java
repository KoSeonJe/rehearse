package com.rehearse.api.domain.questionset.entity;

import com.rehearse.api.domain.questionpool.entity.QuestionPool;
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

    @Builder
    public Question(QuestionType questionType, String questionText,
                    String modelAnswer, ReferenceType referenceType,
                    FeedbackPerspective feedbackPerspective,
                    int orderIndex, QuestionPool questionPool) {
        this.questionType = questionType;
        this.questionText = questionText;
        this.modelAnswer = modelAnswer;
        this.referenceType = referenceType;
        this.feedbackPerspective = feedbackPerspective;
        this.orderIndex = orderIndex;
        this.questionPool = questionPool;
    }

    void assignQuestionSet(QuestionSet questionSet) {
        this.questionSet = questionSet;
    }
}
