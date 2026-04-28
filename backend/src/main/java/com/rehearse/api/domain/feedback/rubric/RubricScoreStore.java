package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.feedback.rubric.entity.RubricScoreEntity;
import com.rehearse.api.domain.feedback.rubric.repository.RubricScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RubricScoreStore {

    private final RubricScoreRepository rubricScoreRepository;

    @Transactional(readOnly = true)
    public Optional<RubricScoreEntity> findExisting(Long interviewId, Long turnId) {
        return rubricScoreRepository.findFirstByInterviewIdAndTurnId(interviewId, turnId);
    }

    @Transactional
    public RubricScoreEntity save(Long interviewId, Long turnId, RubricScore rubricScore) {
        RubricScoreEntity entity = RubricScoreEntity.builder()
                .interviewId(interviewId)
                .turnId(turnId)
                .rubricId(rubricScore.rubricId())
                .scoresJson(rubricScore.dimensionScores())
                .levelFlag(rubricScore.levelFlag())
                .build();
        return rubricScoreRepository.save(entity);
    }
}
