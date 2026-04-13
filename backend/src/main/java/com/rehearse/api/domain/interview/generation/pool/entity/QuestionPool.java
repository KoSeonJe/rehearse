package com.rehearse.api.domain.interview.generation.pool.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_pool")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuestionPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String cacheKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String ttsContent;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String modelAnswer;

    @Column(length = 50)
    private String referenceType;

    @Column(nullable = false)
    private boolean isActive;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private QuestionPool(String cacheKey, String content, String ttsContent,
                         String category, String modelAnswer, String referenceType) {
        this.cacheKey = cacheKey;
        this.content = content;
        this.ttsContent = ttsContent;
        this.category = category;
        this.modelAnswer = modelAnswer;
        this.referenceType = referenceType;
        this.isActive = true;
    }

    public static QuestionPool create(String cacheKey, String content, String ttsContent,
            String category, String modelAnswer, String referenceType) {
        if (cacheKey == null || cacheKey.isBlank()) {
            throw new IllegalArgumentException("cacheKey는 필수입니다");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 필수입니다");
        }
        return QuestionPool.builder()
                .cacheKey(cacheKey)
                .content(content)
                .ttsContent(ttsContent)
                .category(category)
                .modelAnswer(modelAnswer)
                .referenceType(referenceType)
                .build();
    }

    public void deactivate() {
        this.isActive = false;
    }
}
