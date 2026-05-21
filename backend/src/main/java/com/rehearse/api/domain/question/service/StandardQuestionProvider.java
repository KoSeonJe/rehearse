package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.interview.entity.CsSubTopic;
import com.rehearse.api.domain.question.dto.GetQuestionPoolCommand;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.models.service.StandardQuestionGenerator;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandardQuestionProvider {

    private final QuestionPoolService questionPoolService;
    private final QuestionRepository questionRepository;
    private final StandardQuestionGenerator standardQuestionGenerator;
    private final QuestionGenerationLock questionGenerationLock;

    public List<QuestionPool> provide(InterviewType type, int requiredCount, GetQuestionPoolCommand command) {

        String cacheKey = QuestionCacheKeyGenerator.generate(command.position(), command.level(), command.techStack(), type);
        List<String> csTopicsFilter = extractCsTopicsFilter(type, command.csSubTopics());
        Set<Long> usedPoolIds = findUsedPoolIds(command.userId(), cacheKey);

        PoolSelectionCriteria criteria = new PoolSelectionCriteria(cacheKey, requiredCount, csTopicsFilter, usedPoolIds);
        Optional<List<QuestionPool>> hit = questionPoolService.selectIfSufficient(criteria);
        if (hit.isPresent()) {
            log.info("[CACHE] pool 히트: cacheKey={}, required={}, usedByUser={}", cacheKey, requiredCount, usedPoolIds.size());
            return hit.get();
        }

        log.info("[CACHE] pool 부족, AI 호출: cacheKey={}, usedByUser={}", cacheKey, usedPoolIds.size());
        return generateWithStampedeProtection(cacheKey, command.userId(), command.position(), command.level(), command.techStack(), type,
                requiredCount, command.csSubTopics(), csTopicsFilter);
    }

    private Set<Long> findUsedPoolIds(Long userId, String cacheKey) {
        if (userId == null) {
            throw new IllegalArgumentException("user Id가 null일 수 없습니다.");
        }
        return questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(userId, cacheKey);
    }

    private List<QuestionPool> generateWithStampedeProtection(
            String cacheKey, Long userId, Position position, InterviewLevel level,
            TechStack techStack, InterviewType type,
            int requiredCount, List<String> csSubTopics, List<String> categoryFilter) {

        ReentrantLock lock = questionGenerationLock.acquire(cacheKey);
        try {
            // lock 획득 후 usedPoolIds를 재조회하여 stale 데이터 방지
            Set<Long> freshUsedPoolIds = findUsedPoolIds(userId, cacheKey);

            PoolSelectionCriteria freshCriteria = new PoolSelectionCriteria(cacheKey, requiredCount, categoryFilter, freshUsedPoolIds);
            Optional<List<QuestionPool>> hit = questionPoolService.selectIfSufficient(freshCriteria);
            if (hit.isPresent()) {
                log.info("[CACHE] lock 후 pool 히트: cacheKey={}", cacheKey);
                return hit.get();
            }

            QuestionGenerationRequest request = new QuestionGenerationRequest(
                    position, null, level,
                    Set.of(type),
                    csSubTopics != null ? new HashSet<>(csSubTopics) : Set.of(),
                    null, techStack
            );

            List<GeneratedQuestion> generatedQuestions = standardQuestionGenerator.generate(request);
            List<QuestionPool> generatedQuestionPools = questionPoolService.saveQuestionPools(cacheKey, generatedQuestions);

            // category 필터가 있으면 필터링 후 선택
            if (categoryFilter != null && !categoryFilter.isEmpty()) {
                List<QuestionPool> filtered = generatedQuestionPools.stream()
                        .filter(qp -> qp.getCategory() != null && categoryFilter.contains(qp.getCategory()))
                        .toList();
                if (!filtered.isEmpty()) {
                    return questionPoolService.selectWithCategoryDistribution(filtered, requiredCount);
                }
            }

            return questionPoolService.selectWithCategoryDistribution(generatedQuestionPools, requiredCount);
        } catch (Exception e) {
            log.error("[CACHE] AI 호출 실패: cacheKey={}", cacheKey, e);
            throw e;
        } finally {
            questionGenerationLock.release(lock);
        }
    }

    private List<String> extractCsTopicsFilter(InterviewType type, List<String> csSubTopics) {
        if (type != InterviewType.CS_FUNDAMENTAL) {
            return List.of();
        }
        if (csSubTopics == null || csSubTopics.isEmpty()) {
            return CsSubTopic.allCategoryNames();
        }
        return csSubTopics.stream()
                .flatMap(s -> CsSubTopic.toCategoryName(s).stream())
                .toList();
    }
}
