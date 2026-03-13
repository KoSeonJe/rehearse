package com.devlens.api.domain.report.entity;

import com.devlens.api.domain.interview.entity.Interview;
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
@Table(name = "interview_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InterviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false, unique = true)
    private Interview interview;

    @Column(nullable = false)
    private int overallScore;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @ElementCollection
    @CollectionTable(name = "report_strengths", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "strength", nullable = false, columnDefinition = "TEXT")
    private List<String> strengths = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "report_improvements", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "improvement", nullable = false, columnDefinition = "TEXT")
    private List<String> improvements = new ArrayList<>();

    @Column(nullable = false)
    private int feedbackCount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public InterviewReport(Interview interview, int overallScore, String summary,
                           List<String> strengths, List<String> improvements, int feedbackCount) {
        this.interview = interview;
        this.overallScore = overallScore;
        this.summary = summary;
        this.strengths = strengths != null ? new ArrayList<>(strengths) : new ArrayList<>();
        this.improvements = improvements != null ? new ArrayList<>(improvements) : new ArrayList<>();
        this.feedbackCount = feedbackCount;
    }
}
