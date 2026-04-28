package com.rehearse.api.domain.feedback.rubric.dto;

import com.rehearse.api.domain.feedback.rubric.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoreEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class RubricScoreResponse {

    private final Long id;
    private final Long interviewId;
    private final Long turnId;
    private final String rubricId;
    private final Map<String, DimensionScore> scoresJson;
    private final String levelFlag;
    private final LocalDateTime createdAt;

    public static RubricScoreResponse from(RubricScoreEntity entity) {
        return RubricScoreResponse.builder()
                .id(entity.getId())
                .interviewId(entity.getInterviewId())
                .turnId(entity.getTurnId())
                .rubricId(entity.getRubricId())
                .scoresJson(entity.getScoresJson())
                .levelFlag(entity.getLevelFlag())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
