package com.rehearse.api.domain.questionset.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "timestamp_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimestampFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_set_feedback_id", nullable = false)
    private QuestionSetFeedback questionSetFeedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType answerType;

    @Column(nullable = false)
    private long startMs;

    @Column(nullable = false)
    private long endMs;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    private Integer verbalScore;

    @Column(columnDefinition = "TEXT")
    private String verbalComment;

    private Integer fillerWordCount;

    private Integer eyeContactScore;

    private Integer postureScore;

    @Column(length = 50)
    private String expressionLabel;

    @Column(columnDefinition = "TEXT")
    private String nonverbalComment;

    @Column(columnDefinition = "TEXT")
    private String overallComment;

    @Column(nullable = false)
    private boolean isAnalyzed;

    @Builder
    public TimestampFeedback(QuestionType answerType, long startMs, long endMs,
                             String transcript, Integer verbalScore, String verbalComment,
                             Integer fillerWordCount, Integer eyeContactScore, Integer postureScore,
                             String expressionLabel, String nonverbalComment, String overallComment,
                             boolean isAnalyzed) {
        this.answerType = answerType;
        this.startMs = startMs;
        this.endMs = endMs;
        this.transcript = transcript;
        this.verbalScore = verbalScore;
        this.verbalComment = verbalComment;
        this.fillerWordCount = fillerWordCount;
        this.eyeContactScore = eyeContactScore;
        this.postureScore = postureScore;
        this.expressionLabel = expressionLabel;
        this.nonverbalComment = nonverbalComment;
        this.overallComment = overallComment;
        this.isAnalyzed = isAnalyzed;
    }

    void assignQuestionSetFeedback(QuestionSetFeedback questionSetFeedback) {
        this.questionSetFeedback = questionSetFeedback;
    }
}
