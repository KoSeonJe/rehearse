package com.devlens.api.domain.feedback.entity;

import com.devlens.api.domain.interview.entity.Interview;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InterviewAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(nullable = false)
    private int questionIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionContent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Column(columnDefinition = "TEXT")
    private String nonVerbalSummary;

    @Column(columnDefinition = "TEXT")
    private String voiceSummary;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public InterviewAnswer(Interview interview, int questionIndex, String questionContent,
                           String answerText, String nonVerbalSummary, String voiceSummary) {
        this.interview = interview;
        this.questionIndex = questionIndex;
        this.questionContent = questionContent;
        this.answerText = answerText;
        this.nonVerbalSummary = nonVerbalSummary;
        this.voiceSummary = voiceSummary;
    }
}
