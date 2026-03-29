package com.rehearse.api.domain.questionset.entity;

import com.rehearse.api.domain.questionpool.converter.StringListJsonConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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

    // Gemini 네이티브 오디오 분석 음성 특성 필드 (nullable — 기존 데이터 호환)
    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> fillerWords;  // JSON 배열로 저장 예: ["음", "어"]

    @Column(length = 10)
    private String speechPace;  // "빠름" / "적절" / "느림"

    private Integer toneConfidence;  // 0-100

    @Column(length = 20)
    private String emotionLabel;  // "자신감" / "긴장" / "평온" / "불안"

    @Column(columnDefinition = "TEXT")
    private String vocalComment;

    @Builder
    public TimestampFeedback(Question question, long startMs, long endMs,
                             String transcript, Integer verbalScore, String verbalComment,
                             Integer fillerWordCount, Integer eyeContactScore, Integer postureScore,
                             String expressionLabel, String nonverbalComment, String overallComment,
                             boolean isAnalyzed,
                             List<String> fillerWords, String speechPace, Integer toneConfidence,
                             String emotionLabel, String vocalComment) {
        this.question = question;
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
        this.fillerWords = fillerWords;
        this.speechPace = speechPace;
        this.toneConfidence = toneConfidence;
        this.emotionLabel = emotionLabel;
        this.vocalComment = vocalComment;
    }

    void assignQuestionSetFeedback(QuestionSetFeedback questionSetFeedback) {
        this.questionSetFeedback = questionSetFeedback;
    }
}
