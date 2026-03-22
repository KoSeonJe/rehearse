package com.rehearse.api.domain.questionpool.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.questionpool.entity.QuestionPool;
import com.rehearse.api.domain.questionpool.util.CacheKeyGenerator;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheableQuestionProvider {

    private final QuestionPoolService questionPoolService;
    private final AiClient aiClient;

    private final ConcurrentHashMap<String, ReentrantLock> keyLocks = new ConcurrentHashMap<>();

    public List<QuestionPool> provide(Position position, InterviewLevel level,
                                      TechStack techStack, InterviewType type,
                                      int requiredCount, List<String> csSubTopics) {

        String cacheKey = CacheKeyGenerator.generate(position, level, techStack, type, csSubTopics);

        if (questionPoolService.isPoolSufficient(cacheKey, requiredCount)) {
            log.info("[CACHE] pool 히트: cacheKey={}, required={}", cacheKey, requiredCount);
            return questionPoolService.selectFromPool(cacheKey, requiredCount);
        }

        log.info("[CACHE] pool 부족, Claude 호출: cacheKey={}", cacheKey);
        return generateWithStampedeProtection(cacheKey, position, level, techStack, type,
                requiredCount, csSubTopics);
    }

    private List<QuestionPool> generateWithStampedeProtection(
            String cacheKey, Position position, InterviewLevel level,
            TechStack techStack, InterviewType type,
            int requiredCount, List<String> csSubTopics) {

        ReentrantLock lock = keyLocks.computeIfAbsent(cacheKey, k -> new ReentrantLock());
        lock.lock();
        try {
            if (questionPoolService.isPoolSufficient(cacheKey, requiredCount)) {
                log.info("[CACHE] lock 후 pool 히트: cacheKey={}", cacheKey);
                return questionPoolService.selectFromPool(cacheKey, requiredCount);
            }

            QuestionGenerationRequest request = new QuestionGenerationRequest(
                    position, null, level,
                    Set.of(type),
                    csSubTopics != null ? new HashSet<>(csSubTopics) : Set.of(),
                    null, null, techStack
            );

            List<GeneratedQuestion> generated = aiClient.generateQuestions(request);
            List<QuestionPool> allGenerated = questionPoolService.convertAndCacheIfEligible(cacheKey, generated);

            if (allGenerated.isEmpty()) {
                return allGenerated;
            }

            return questionPoolService.selectWithCategoryDistribution(allGenerated, requiredCount);
        } catch (Exception e) {
            log.error("[CACHE] Claude 호출 실패: cacheKey={}", cacheKey, e);
            throw e;
        } finally {
            lock.unlock();
            keyLocks.remove(cacheKey, lock);
        }
    }
}
