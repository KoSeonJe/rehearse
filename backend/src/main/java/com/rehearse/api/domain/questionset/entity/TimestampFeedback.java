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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false)
    private long startMs;

    @Column(nullable = false)
    private long endMs;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(columnDefinition = "TEXT")
    private String verbalComment;

    private Integer fillerWordCount;

    @Column(length = 20)
    private String eyeContactLevel;  // GOOD / AVERAGE / NEEDS_IMPROVEMENT

    @Column(length = 20)
    private String postureLevel;  // GOOD / AVERAGE / NEEDS_IMPROVEMENT

    @Column(length = 50)
    private String expressionLabel;

    @Column(columnDefinition = "TEXT")
    private String nonverbalComment;

    @Column(columnDefinition = "TEXT")
    private String overallComment;

    @Column(nullable = false)
    private boolean isAnalyzed;

    @Column(columnDefinition = "TEXT")
    private String fillerWords;  // JSON 배열 문자열 예: ["음", "어"]

    @Column(length = 10)
    private String speechPace;  // "빠름" / "적절" / "느림"

    @Column(length = 20)
    private String toneConfidenceLevel;  // GOOD / AVERAGE / NEEDS_IMPROVEMENT

    @Column(length = 20)
    private String emotionLabel;  // "자신감" / "긴장" / "평온" / "불안"

    @Column(columnDefinition = "TEXT")
    private String vocalComment;

    @Column(columnDefinition = "TEXT")
    private String accuracyIssues;  // JSON: [{"claim":"...","correction":"..."}]

    @Column(length = 500)
    private String coachingStructure;

    @Column(length = 500)
    private String coachingImprovement;

    @Builder
    public TimestampFeedback(Question question, long startMs, long endMs,
                             String transcript, String verbalComment,
                             Integer fillerWordCount,
                             String eyeContactLevel, String postureLevel,
                             String expressionLabel, String nonverbalComment, String overallComment,
                             boolean isAnalyzed,
                             String fillerWords, String speechPace, String toneConfidenceLevel,
                             String emotionLabel, String vocalComment,
                             String accuracyIssues, String coachingStructure, String coachingImprovement) {
        this.question = question;
        this.startMs = startMs;
        this.endMs = endMs;
        this.transcript = transcript;
        this.verbalComment = verbalComment;
        this.fillerWordCount = fillerWordCount;
        this.eyeContactLevel = eyeContactLevel;
        this.postureLevel = postureLevel;
        this.expressionLabel = expressionLabel;
        this.nonverbalComment = nonverbalComment;
        this.overallComment = overallComment;
        this.isAnalyzed = isAnalyzed;
        this.fillerWords = fillerWords;
        this.speechPace = speechPace;
        this.toneConfidenceLevel = toneConfidenceLevel;
        this.emotionLabel = emotionLabel;
        this.vocalComment = vocalComment;
        this.accuracyIssues = accuracyIssues;
        this.coachingStructure = coachingStructure;
        this.coachingImprovement = coachingImprovement;
    }

    void assignQuestionSetFeedback(QuestionSetFeedback questionSetFeedback) {
        this.questionSetFeedback = questionSetFeedback;
    }
}
