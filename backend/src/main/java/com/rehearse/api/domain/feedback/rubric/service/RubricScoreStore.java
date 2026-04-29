package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.rubric.entity.RubricScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
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
    public Optional<RubricScore> findExisting(Long interviewId, Long turnId) {
        return rubricScoreRepository.findFirstByInterviewIdAndTurnId(interviewId, turnId);
    }

    @Transactional
    public RubricScore save(Long interviewId, Long turnId, RubricScoringResult rubricScore) {
        RubricScore entity = RubricScore.builder()
                .interviewId(interviewId)
                .turnId(turnId)
                .rubricId(rubricScore.rubricId())
                .scoresJson(rubricScore.dimensionScores())
                .levelFlag(rubricScore.levelFlag())
                .build();
        return rubricScoreRepository.save(entity);
    }
}
