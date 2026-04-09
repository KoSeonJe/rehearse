package com.rehearse.api.domain.servicefeedback.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ServiceFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackSource source;

    @Column(nullable = false)
    private int completedCountSnapshot;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ServiceFeedback create(Long userId, String content, Integer rating,
                                         FeedbackSource source, long completedCount) {
        ServiceFeedback feedback = new ServiceFeedback();
        feedback.userId = userId;
        feedback.content = content;
        feedback.rating = rating;
        feedback.source = source;
        feedback.completedCountSnapshot = (int) completedCount;
        return feedback;
    }
}
