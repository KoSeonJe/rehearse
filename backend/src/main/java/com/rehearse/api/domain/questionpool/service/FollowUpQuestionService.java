package com.rehearse.api.domain.questionpool.service;

import com.rehearse.api.domain.questionpool.entity.FollowUpStrategy;
import com.rehearse.api.domain.questionpool.entity.PreparedFollowUp;
import com.rehearse.api.domain.questionpool.entity.QuestionPool;
import com.rehearse.api.domain.questionpool.repository.PreparedFollowUpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowUpQuestionService {

    private final PreparedFollowUpRepository preparedFollowUpRepository;
    private final KeywordMatcher keywordMatcher;

    public Optional<PreparedFollowUp> selectPrepared(QuestionPool questionPool, String userAnswer) {
        if (questionPool == null || questionPool.getFollowUpStrategy() != FollowUpStrategy.PREPARED) {
            return Optional.empty();
        }

        List<PreparedFollowUp> candidates = preparedFollowUpRepository
                .findByQuestionPoolIdOrderByDisplayOrderAsc(questionPool.getId());

        if (candidates.isEmpty()) {
            log.warn("[PREPARED] 후보 없음: questionPoolId={}", questionPool.getId());
            return Optional.empty();
        }

        for (PreparedFollowUp candidate : candidates) {
            if (keywordMatcher.matches(candidate.getMatchKeywords(),
                    candidate.getMatchThreshold(), userAnswer)) {
                log.info("[PREPARED] 키워드 매칭 성공: questionPoolId={}, followUpId={}",
                        questionPool.getId(), candidate.getId());
                return Optional.of(candidate);
            }
        }

        PreparedFollowUp fallback = candidates.get(0);
        log.info("[PREPARED] 키워드 매칭 실패, fallback: questionPoolId={}, followUpId={}",
                questionPool.getId(), fallback.getId());
        return Optional.of(fallback);
    }

    public boolean isPrepared(QuestionPool questionPool) {
        return questionPool != null && questionPool.getFollowUpStrategy() == FollowUpStrategy.PREPARED;
    }
}
