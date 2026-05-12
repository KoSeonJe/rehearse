package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.repository.QuestionPoolRepository;
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
    private static final double USER_SUFFICIENCY_MULTIPLIER = 2.0;

    private final QuestionPoolRepository questionPoolRepository;

    public Optional<List<QuestionPool>> selectIfSufficient(PoolSelectionCriteria criteria) {
        List<QuestionPool> candidates = getCandidates(criteria.cacheKey(), criteria.categoryFilter());
        List<QuestionPool> available = criteria.hasUsedPoolIds()
                ? candidates.stream()
                        .filter(qp -> !criteria.usedPoolIds().contains(qp.getId()))
                        .toList()
                : candidates;

        long threshold = sufficiencyThreshold(criteria);
        if (available.size() < threshold) {
            return Optional.empty();
        }
        return Optional.of(selectWithCategoryDistribution(available, criteria.requiredCount()));
    }

    private long sufficiencyThreshold(PoolSelectionCriteria criteria) {
        if (criteria.hasUsedPoolIds()) {
            return (long) Math.ceil(criteria.requiredCount() * USER_SUFFICIENCY_MULTIPLIER);
        }
        return (long) criteria.requiredCount() * POOL_SUFFICIENCY_MULTIPLIER;
    }

    private List<QuestionPool> getCandidates(String cacheKey, List<String> categoryFilter) {
        if (categoryFilter == null || categoryFilter.isEmpty()) {
            return questionPoolRepository.findByCacheKeyAndIsActiveTrue(cacheKey);
        }
        return questionPoolRepository.findByCacheKeyAndIsActiveTrueAndCategoryIn(cacheKey, categoryFilter);
    }

    public List<QuestionPool> selectWithCategoryDistribution(
            List<QuestionPool> candidates, int requiredCount) {

        if (candidates.size() <= requiredCount) {
            return new ArrayList<>(candidates);
        }

        Map<String, Queue<QuestionPool>> byCategory = groupAndShuffleByCategory(candidates);
        List<String> categories = new ArrayList<>(byCategory.keySet());
        Collections.shuffle(categories);

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

    private Map<String, Queue<QuestionPool>> groupAndShuffleByCategory(List<QuestionPool> candidates) {
        Map<String, Queue<QuestionPool>> byCategory = candidates.stream()
                .collect(Collectors.groupingBy(
                        qp -> qp.getCategory() != null ? qp.getCategory() : "UNKNOWN",
                        Collectors.toCollection(LinkedList::new)));

        byCategory.values().forEach(queue -> {
            List<QuestionPool> list = new ArrayList<>(queue);
            Collections.shuffle(list);
            queue.clear();
            queue.addAll(list);
        });

        return byCategory;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    List<QuestionPool> saveQuestionPools(String cacheKey, List<GeneratedQuestion> generated) {
        List<QuestionPool> pools = generated.stream()
                .map(gq -> QuestionPool.create(
                        cacheKey,
                        gq.content(),
                        gq.ttsContent(),
                        gq.category(),
                        gq.bestAnswer()))
                .collect(Collectors.toList());
        questionPoolRepository.saveAll(pools);
        log.info("[POOL] 저장 완료: cacheKey={}, count={}", cacheKey, pools.size());
        return pools;
    }
}
