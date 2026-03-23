package com.rehearse.api.domain.questionset.entity;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.interview.entity.Interview;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_set")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuestionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionCategory category;

    @Column(nullable = false)
    private int orderIndex;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_metadata_id")
    private FileMetadata fileMetadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnalysisStatus analysisStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AnalysisProgress analysisProgress;

    @Column(length = 500)
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String failureDetail;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "questionSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Question> questions = new ArrayList<>();

    @Builder
    public QuestionSet(Interview interview, QuestionCategory category, int orderIndex) {
        this.interview = interview;
        this.category = category;
        this.orderIndex = orderIndex;
        this.analysisStatus = AnalysisStatus.PENDING;
    }

    public void assignInterview(Interview interview) {
        this.interview = interview;
    }

    public void updateOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public void addQuestion(Question question) {
        this.questions.add(question);
        question.assignQuestionSet(this);
    }

    public void assignFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public void updateAnalysisStatus(AnalysisStatus newStatus) {
        if (!this.analysisStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("분석 상태를 %s에서 %s로 변경할 수 없습니다.", this.analysisStatus, newStatus));
        }
        this.analysisStatus = newStatus;
    }

    public void updateAnalysisProgress(AnalysisProgress progress) {
        this.analysisProgress = progress;
    }

    public void markFailed(String reason, String detail) {
        updateAnalysisStatus(AnalysisStatus.FAILED);
        this.analysisProgress = AnalysisProgress.FAILED;
        this.failureReason = reason;
        this.failureDetail = detail;
    }
}
