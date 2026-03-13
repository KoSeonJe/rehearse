package com.devlens.api.domain.interview.entity;

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
@Table(name = "interview")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Position position;

    @Column(length = 100)
    private String positionDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewLevel level;

    @ElementCollection(targetClass = InterviewType.class)
    @CollectionTable(name = "interview_interview_types", joinColumns = @JoinColumn(name = "interview_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false, length = 30)
    private List<InterviewType> interviewTypes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "interview_cs_sub_topics", joinColumns = @JoinColumn(name = "interview_id"))
    @Column(name = "cs_sub_topic", length = 50)
    private List<String> csSubTopics = new ArrayList<>();

    @Column(nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status;

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    private List<InterviewQuestion> questions = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Interview(Position position, String positionDetail, InterviewLevel level, List<InterviewType> interviewTypes, List<String> csSubTopics, Integer durationMinutes) {
        this.position = position;
        this.positionDetail = positionDetail;
        this.level = level;
        this.interviewTypes = interviewTypes != null ? new ArrayList<>(interviewTypes) : new ArrayList<>();
        this.csSubTopics = csSubTopics != null ? new ArrayList<>(csSubTopics) : new ArrayList<>();
        this.durationMinutes = durationMinutes;
        this.status = InterviewStatus.READY;
    }

    public void addQuestion(InterviewQuestion question) {
        this.questions.add(question);
        question.assignInterview(this);
    }

    public void updateStatus(InterviewStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("상태를 %s에서 %s로 변경할 수 없습니다.", this.status, newStatus));
        }
        this.status = newStatus;
    }
}
