package com.rehearse.api.domain.feedback.entity;

import com.rehearse.api.domain.questionset.entity.QuestionSet;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionSetComment;

    @OneToMany(mappedBy = "questionSetFeedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimestampFeedback> timestampFeedbacks = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public QuestionSetFeedback(QuestionSet questionSet, String questionSetComment) {
        this.questionSet = questionSet;
        this.questionSetComment = questionSetComment;
    }

    public List<TimestampFeedback> getTimestampFeedbacks() {
        return Collections.unmodifiableList(timestampFeedbacks);
    }

    public void addTimestampFeedback(TimestampFeedback feedback) {
        this.timestampFeedbacks.add(feedback);
        feedback.assignQuestionSetFeedback(this);
    }
}
