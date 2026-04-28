package com.rehearse.api.domain.feedback.rubric.entity;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.global.util.DimensionScoreMapConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "rubric_score")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RubricScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "interview_id", nullable = false)
    private Long interviewId;

    @Column(name = "turn_id", nullable = false)
    private Long turnId;

    @Column(name = "rubric_id", nullable = false, length = 64)
    private String rubricId;

    @Column(name = "scores_json", nullable = false, columnDefinition = "JSON")
    @Convert(converter = DimensionScoreMapConverter.class)
    private Map<String, DimensionScore> scoresJson;

    @Column(name = "level_flag", length = 64)
    private String levelFlag;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RubricScoreEntity(Long interviewId, Long turnId, String rubricId,
                             Map<String, DimensionScore> scoresJson, String levelFlag) {
        this.interviewId = interviewId;
        this.turnId = turnId;
        this.rubricId = rubricId;
        this.scoresJson = scoresJson;
        this.levelFlag = levelFlag;
    }
}
