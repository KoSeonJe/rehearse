package com.rehearse.api.domain.feedback.score.service;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.score.entity.DimensionStatus;
import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.entity.QuestionScoreDimension;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreDimensionRepository;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionScorePersister {

    private final QuestionScoreRepository questionScoreRepository;
    private final QuestionScoreDimensionRepository dimensionRepository;

    @Transactional
    public void saveRubric(Long questionId, Long interviewId, String rubricId,
                           String levelFlag,
                           Map<String, DimensionScore> dimensionScores) {
        if (questionScoreRepository.findByQuestionIdAndRubricId(questionId, rubricId).isPresent()) {
            log.debug("QuestionScore 이미 존재 (idempotent skip): questionId={}, rubricId={}", questionId, rubricId);
            return;
        }

        QuestionScore qs = QuestionScore.builder()
                .questionId(questionId)
                .interviewId(interviewId)
                .rubricId(rubricId)
                .levelFlag(levelFlag)
                .build();
        questionScoreRepository.save(qs);

        dimensionScores.forEach((dimensionRef, ds) -> {
            if (ds == null) return;
            DimensionStatus status = ds.status() != null ? ds.status() : DimensionStatus.OK;
            if (status == DimensionStatus.OK && ds.score() == null) return;
            QuestionScoreDimension dim = QuestionScoreDimension.builder()
                    .questionScoreId(qs.getId())
                    .dimensionRef(dimensionRef)
                    .score(ds.score())
                    .observation(ds.observation())
                    .evidenceQuote(ds.evidenceQuote())
                    .status(status)
                    .build();
            dimensionRepository.save(dim);
        });

        log.info("QuestionScore 저장: questionId={}, rubricId={}, dimensions={}", questionId, rubricId, dimensionScores.keySet());
    }
}
