package com.rehearse.api.domain.questionpool.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
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

    @Column(length = 100)
    private String category;

    private Integer questionOrder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String evaluationCriteria;

    @Column(columnDefinition = "TEXT")
    private String modelAnswer;

    @Column(length = 50)
    private String referenceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FollowUpStrategy followUpStrategy;

    @Column(precision = 3, scale = 2)
    private BigDecimal qualityScore;

    @Column(nullable = false)
    private boolean isActive;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private QuestionPool(String cacheKey, String content, String category, Integer questionOrder,
                         String evaluationCriteria, String modelAnswer, String referenceType,
                         FollowUpStrategy followUpStrategy) {
        this.cacheKey = cacheKey;
        this.content = content;
        this.category = category;
        this.questionOrder = questionOrder;
        this.evaluationCriteria = evaluationCriteria;
        this.modelAnswer = modelAnswer;
        this.referenceType = referenceType;
        this.followUpStrategy = followUpStrategy != null ? followUpStrategy : FollowUpStrategy.REALTIME;
        this.qualityScore = new BigDecimal("1.00");
        this.isActive = true;
    }

    public static QuestionPool create(String cacheKey, String content, String category,
            Integer questionOrder, String evaluationCriteria, String modelAnswer,
            String referenceType, FollowUpStrategy followUpStrategy) {
        if (cacheKey == null || cacheKey.isBlank()) {
            throw new IllegalArgumentException("cacheKey는 필수입니다");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 필수입니다");
        }
        if (evaluationCriteria == null || evaluationCriteria.isBlank()) {
            throw new IllegalArgumentException("evaluationCriteria는 필수입니다");
        }
        return QuestionPool.builder()
                .cacheKey(cacheKey)
                .content(content)
                .category(category)
                .questionOrder(questionOrder)
                .evaluationCriteria(evaluationCriteria)
                .modelAnswer(modelAnswer)
                .referenceType(referenceType)
                .followUpStrategy(followUpStrategy)
                .build();
    }

    public void deactivate() {
        this.isActive = false;
    }
}
