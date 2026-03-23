package com.rehearse.api.domain.questionset.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_set_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuestionSetFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_set_id", nullable = false, unique = true)
    private QuestionSet questionSet;

    @Column(nullable = false)
    private int questionSetScore;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionSetComment;

    @OneToMany(mappedBy = "questionSetFeedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimestampFeedback> timestampFeedbacks = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Gemini 네이티브 오디오 분석 종합 리포트 필드 (nullable — 기존 데이터 호환)
    @Column(columnDefinition = "TEXT")
    private String verbalSummary;

    @Column(columnDefinition = "TEXT")
    private String vocalSummary;

    @Column(columnDefinition = "TEXT")
    private String nonverbalSummary;

    @Column(columnDefinition = "TEXT")
    private String strengths;  // JSON 배열 문자열

    @Column(columnDefinition = "TEXT")
    private String improvements;  // JSON 배열 문자열

    @Column(columnDefinition = "TEXT")
    private String topPriorityAdvice;

    @Builder
    public QuestionSetFeedback(QuestionSet questionSet, int questionSetScore, String questionSetComment,
                               String verbalSummary, String vocalSummary, String nonverbalSummary,
                               String strengths, String improvements, String topPriorityAdvice) {
        this.questionSet = questionSet;
        this.questionSetScore = questionSetScore;
        this.questionSetComment = questionSetComment;
        this.verbalSummary = verbalSummary;
        this.vocalSummary = vocalSummary;
        this.nonverbalSummary = nonverbalSummary;
        this.strengths = strengths;
        this.improvements = improvements;
        this.topPriorityAdvice = topPriorityAdvice;
    }

    public void addTimestampFeedback(TimestampFeedback feedback) {
        this.timestampFeedbacks.add(feedback);
        feedback.assignQuestionSetFeedback(this);
    }
}
