package com.rehearse.api.domain.questionset.entity;

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

    @Column(nullable = false)
    private int orderIndex;

    @Builder
    public Question(QuestionType questionType, String questionText,
                    String modelAnswer, ReferenceType referenceType, int orderIndex) {
        this.questionType = questionType;
        this.questionText = questionText;
        this.modelAnswer = modelAnswer;
        this.referenceType = referenceType;
        this.orderIndex = orderIndex;
    }

    void assignQuestionSet(QuestionSet questionSet) {
        this.questionSet = questionSet;
    }
}
