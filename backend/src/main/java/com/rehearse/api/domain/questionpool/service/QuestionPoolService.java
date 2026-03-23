package com.rehearse.api.domain.questionpool.service;

import com.rehearse.api.domain.questionpool.entity.FollowUpStrategy;
import com.rehearse.api.domain.questionpool.entity.QuestionPool;
import com.rehearse.api.domain.questionpool.repository.QuestionPoolRepository;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionPoolService {

    private static final int POOL_SUFFICIENCY_MULTIPLIER = 3;
    private static final int POOL_SOFT_CAP = 200;

    private final QuestionPoolRepository questionPoolRepository;

    public boolean isPoolSufficient(String cacheKey, int requiredCount) {
        long activeCount = questionPoolRepository.countByCacheKeyAndIsActiveTrue(cacheKey);
        return activeCount >= (long) requiredCount * POOL_SUFFICIENCY_MULTIPLIER;
    }

    public boolean shouldSaveToPool(String cacheKey) {
        long activeCount = questionPoolRepository.countByCacheKeyAndIsActiveTrue(cacheKey);
        return activeCount < POOL_SOFT_CAP;
    }

    public List<QuestionPool> selectFromPool(String cacheKey, int requiredCount) {
        List<QuestionPool> candidates = questionPoolRepository.findByCacheKeyAndIsActiveTrue(cacheKey);
        return selectWithCategoryDistribution(candidates, requiredCount);
    }

    public List<QuestionPool> selectFromPool(String cacheKey, int requiredCount, List<String> categoryFilter) {
        if (categoryFilter == null || categoryFilter.isEmpty()) {
            return selectFromPool(cacheKey, requiredCount);
        }
        List<QuestionPool> candidates = questionPoolRepository
                .findByCacheKeyAndIsActiveTrueAndCategoryIn(cacheKey, categoryFilter);
        return selectWithCategoryDistribution(candidates, requiredCount);
    }

    public List<QuestionPool> selectWithCategoryDistribution(
            List<QuestionPool> candidates, int requiredCount) {

        if (candidates.size() <= requiredCount) {
            return new ArrayList<>(candidates);
        }

        Map<String, Queue<QuestionPool>> byCategory = candidates.stream()
                .collect(Collectors.groupingBy(
                        qp -> qp.getCategory() != null ? qp.getCategory() : "UNKNOWN",
                        Collectors.toCollection(LinkedList::new)));

        List<String> categories = new ArrayList<>(byCategory.keySet());
        Collections.shuffle(categories);

        byCategory.values().forEach(queue -> {
            List<QuestionPool> list = new ArrayList<>(queue);
            Collections.shuffle(list);
            queue.clear();
            queue.addAll(list);
        });

        List<QuestionPool> result = new ArrayList<>();
        int catIdx = 0;

        while (result.size() < requiredCount && !categories.isEmpty()) {
            if (catIdx >= categories.size()) {
                catIdx = 0;
            }

            String cat = categories.get(catIdx);
            Queue<QuestionPool> queue = byCategory.get(cat);

            if (queue != null && !queue.isEmpty()) {
                result.add(queue.poll());
                catIdx++;
            } else {
                categories.remove(catIdx);
                byCategory.remove(cat);
            }
        }
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    List<QuestionPool> convertAndCacheIfEligible(String cacheKey, List<GeneratedQuestion> generated) {
        List<QuestionPool> pools = generated.stream()
                .map(gq -> QuestionPool.builder()
                        .cacheKey(cacheKey)
                        .content(gq.getContent())
                        .category(gq.getCategory())
                        .questionOrder(gq.getOrder())
                        .evaluationCriteria(gq.getEvaluationCriteria())
                        .modelAnswer(gq.getModelAnswer())
                        .referenceType(gq.getReferenceType())
                        .followUpStrategy(parseFollowUpStrategy(gq.getFollowUpStrategy()))
                        .build())
                .collect(Collectors.toList());

        if (shouldSaveToPool(cacheKey)) {
            questionPoolRepository.saveAll(pools);
            log.info("[POOL] 저장 완료: cacheKey={}, count={}", cacheKey, pools.size());
        } else {
            log.info("[POOL] soft cap 도달, DB 저장 생략: cacheKey={}, count={}", cacheKey, pools.size());
        }

        return pools;
    }

    private FollowUpStrategy parseFollowUpStrategy(String strategy) {
        if (strategy != null) {
            try {
                return FollowUpStrategy.valueOf(strategy.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return FollowUpStrategy.PREPARED;
    }
}
