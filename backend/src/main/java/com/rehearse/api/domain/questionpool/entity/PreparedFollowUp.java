package com.rehearse.api.domain.questionpool.entity;

import com.rehearse.api.domain.questionpool.converter.StringListJsonConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "prepared_follow_up")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PreparedFollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_pool_id", nullable = false)
    private QuestionPool questionPool;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String modelAnswer;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> matchKeywords;

    @Column(nullable = false)
    private int matchThreshold;

    @Column(nullable = false)
    private int displayOrder;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public PreparedFollowUp(String content, String modelAnswer,
                            List<String> matchKeywords, Integer matchThreshold,
                            int displayOrder) {
        this.content = content;
        this.modelAnswer = modelAnswer;
        this.matchKeywords = matchKeywords != null ? matchKeywords : List.of();
        this.matchThreshold = matchThreshold != null ? matchThreshold : 2;
        this.displayOrder = displayOrder;
    }

    void assignQuestionPool(QuestionPool questionPool) {
        this.questionPool = questionPool;
    }
}
