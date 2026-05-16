package com.rehearse.api.domain.feedback.entity;

import com.rehearse.api.domain.question.entity.Question;
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

    private Integer fillerWordCount;

    @Column(nullable = false)
    private boolean isAnalyzed;

    @Column(columnDefinition = "TEXT")
    private String fillerWords;  // JSON 배열 문자열 예: ["음", "어"]

    @Builder
    public TimestampFeedback(Question question, long startMs, long endMs,
                             String transcript,
                             Integer fillerWordCount,
                             boolean isAnalyzed,
                             String fillerWords) {
        this.question = question;
        this.startMs = startMs;
        this.endMs = endMs;
        this.transcript = transcript;
        this.fillerWordCount = fillerWordCount;
        this.isAnalyzed = isAnalyzed;
        this.fillerWords = fillerWords;
    }

    void assignQuestionSetFeedback(QuestionSetFeedback questionSetFeedback) {
        this.questionSetFeedback = questionSetFeedback;
    }
}
